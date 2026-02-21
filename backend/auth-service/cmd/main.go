package main

import (
	"context"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/caarlos0/env/v11"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
	"google.golang.org/grpc"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"

	"github.com/whatsapp-clone/backend/auth-service/config"
	"github.com/whatsapp-clone/backend/auth-service/internal/handler"
	"github.com/whatsapp-clone/backend/auth-service/internal/repository"
	"github.com/whatsapp-clone/backend/auth-service/internal/service"
	"github.com/whatsapp-clone/backend/pkg/jwt"
	"github.com/whatsapp-clone/backend/pkg/logger"
	"github.com/whatsapp-clone/backend/pkg/metrics"
	"github.com/whatsapp-clone/backend/pkg/middleware"
	"github.com/whatsapp-clone/backend/pkg/tracing"
	authv1 "github.com/whatsapp-clone/backend/proto/auth/v1"
)

func main() {
	var cfg config.Config
	if err := env.Parse(&cfg); err != nil {
		panic("failed to parse config: " + err.Error())
	}

	log := logger.New("auth-service", cfg.LogLevel)

	// Initialize OpenTelemetry tracing
	shutdownTracer, err := tracing.Init(context.Background(), "auth-service", cfg.OTLPEndpoint)
	if err != nil {
		log.Warn().Err(err).Msg("failed to init tracing, continuing without it")
	} else {
		defer shutdownTracer(context.Background())
	}

	// PostgreSQL
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	pgPool, err := pgxpool.New(ctx, cfg.PostgresDSN)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to PostgreSQL")
	}
	defer pgPool.Close()

	if err := pgPool.Ping(ctx); err != nil {
		log.Fatal().Err(err).Msg("failed to ping PostgreSQL")
	}
	log.Info().Msg("connected to PostgreSQL")

	// Redis
	rdb := redis.NewClient(&redis.Options{
		Addr:     cfg.RedisAddr,
		Password: cfg.RedisPassword,
	})
	if err := rdb.Ping(ctx).Err(); err != nil {
		log.Fatal().Err(err).Msg("failed to connect to Redis")
	}
	defer rdb.Close()
	log.Info().Msg("connected to Redis")

	// JWT Manager
	jwtManager := jwt.NewManager(cfg.JWTSecret, cfg.AccessTokenTTL)

	// Repositories
	userRepo := repository.NewPostgresUserRepository(pgPool)
	otpRepo := repository.NewRedisOTPRepository(rdb, cfg.OTPTTL)
	refreshRepo := repository.NewPostgresRefreshTokenRepository(pgPool)

	// Service
	authSvc := service.NewAuthService(
		userRepo,
		otpRepo,
		refreshRepo,
		jwtManager,
		cfg.RefreshTokenTTL,
		cfg.OTPLength,
		cfg.OTPMaxAttempts,
		log,
	)

	// --- HTTP Server ---
	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(
		middleware.RequestID(),
		middleware.Logger(log),
		middleware.Recovery(log),
		metrics.GinMiddleware("auth-service"),
		tracing.GinMiddleware("auth-service"),
		middleware.CORS([]string{"*"}),
	)

	// Health check
	router.GET("/health", func(c *gin.Context) {
		pgErr := pgPool.Ping(c.Request.Context())
		redisErr := rdb.Ping(c.Request.Context()).Err()
		if pgErr != nil || redisErr != nil {
			c.JSON(http.StatusServiceUnavailable, gin.H{
				"status":   "unhealthy",
				"postgres": errString(pgErr),
				"redis":    errString(redisErr),
			})
			return
		}
		c.JSON(http.StatusOK, gin.H{"status": "healthy"})
	})

	httpHandler := handler.NewHTTPHandler(authSvc, cfg.DevMode, log)
	apiV1 := router.Group("/api/v1")
	httpHandler.RegisterRoutes(apiV1)

	// Prometheus metrics endpoint
	metrics.RegisterMetricsEndpoint(router)

	httpServer := &http.Server{
		Addr:         cfg.HTTPPort,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	go func() {
		log.Info().Str("port", cfg.HTTPPort).Msg("starting HTTP server")
		if err := httpServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal().Err(err).Msg("HTTP server failed")
		}
	}()

	// --- gRPC Server ---
	grpcListener, err := net.Listen("tcp", cfg.GRPCPort)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to listen on gRPC port")
	}

	grpcServer := grpc.NewServer()
	grpcHandler := handler.NewGRPCHandler(authSvc, log)
	authv1.RegisterAuthServiceServer(grpcServer, grpcHandler)

	healthServer := health.NewServer()
	healthpb.RegisterHealthServer(grpcServer, healthServer)
	healthServer.SetServingStatus("auth.v1.AuthService", healthpb.HealthCheckResponse_SERVING)

	reflection.Register(grpcServer)

	go func() {
		log.Info().Str("port", cfg.GRPCPort).Msg("starting gRPC server")
		if err := grpcServer.Serve(grpcListener); err != nil {
			log.Fatal().Err(err).Msg("gRPC server failed")
		}
	}()

	// --- Graceful Shutdown ---
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	sig := <-quit
	log.Info().Str("signal", sig.String()).Msg("shutting down")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer shutdownCancel()

	grpcServer.GracefulStop()
	log.Info().Msg("gRPC server stopped")

	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		log.Error().Err(err).Msg("HTTP server shutdown error")
	}
	log.Info().Msg("HTTP server stopped")

	log.Info().Msg("auth-service stopped gracefully")
}

func errString(err error) string {
	if err != nil {
		return err.Error()
	}
	return "ok"
}
