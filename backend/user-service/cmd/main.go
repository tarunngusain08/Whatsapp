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
	"github.com/rs/zerolog"
	"google.golang.org/grpc"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"

	"github.com/whatsapp-clone/backend/pkg/logger"
	"github.com/whatsapp-clone/backend/pkg/metrics"
	"github.com/whatsapp-clone/backend/pkg/middleware"
	"github.com/whatsapp-clone/backend/pkg/tracing"
	userv1 "github.com/whatsapp-clone/backend/proto/user/v1"
	"github.com/whatsapp-clone/backend/user-service/config"
	"github.com/whatsapp-clone/backend/user-service/internal/handler"
	"github.com/whatsapp-clone/backend/user-service/internal/repository"
	"github.com/whatsapp-clone/backend/user-service/internal/service"
)

func main() {
	var cfg config.Config
	if err := env.Parse(&cfg); err != nil {
		panic("failed to parse config: " + err.Error())
	}

	log := logger.New("user-service", cfg.LogLevel)

	// Initialize OpenTelemetry tracing
	shutdownTracer, err := tracing.Init(context.Background(), "user-service", cfg.OTLPEndpoint)
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

	// Repositories
	userRepo := repository.NewPostgresUserRepository(pgPool)
	contactRepo := repository.NewPostgresContactRepository(pgPool)
	privacyRepo := repository.NewPostgresPrivacyRepository(pgPool)
	deviceTokenRepo := repository.NewPostgresDeviceTokenRepository(pgPool)
	presenceRepo := repository.NewRedisPresenceRepository(rdb)
	statusRepo := repository.NewPostgresStatusRepository(pgPool)

	// Service
	userSvc := service.NewUserService(
		userRepo,
		contactRepo,
		privacyRepo,
		deviceTokenRepo,
		presenceRepo,
		statusRepo,
		cfg.PresenceTTL,
		cfg.MediaServiceURL,
		log,
	)

	// Start periodic status cleanup job
	cleanupCtx, cleanupCancel := context.WithCancel(context.Background())
	go startStatusCleanupJob(cleanupCtx, statusRepo, log)

	// --- HTTP Server ---
	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(
		middleware.RequestID(),
		middleware.Logger(log),
		middleware.Recovery(log),
		metrics.GinMiddleware("user-service"),
		tracing.GinMiddleware("user-service"),
		middleware.CORS([]string{"*"}),
		userIDMiddleware(),
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

	httpHandler := handler.NewHTTPHandler(userSvc, log)
	apiV1 := router.Group("/api/v1/users")
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
	grpcHandler := handler.NewGRPCHandler(userSvc)
	userv1.RegisterUserServiceServer(grpcServer, grpcHandler)

	healthServer := health.NewServer()
	healthpb.RegisterHealthServer(grpcServer, healthServer)
	healthServer.SetServingStatus("user.v1.UserService", healthpb.HealthCheckResponse_SERVING)

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

	cleanupCancel()
	grpcServer.GracefulStop()
	log.Info().Msg("gRPC server stopped")

	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		log.Error().Err(err).Msg("HTTP server shutdown error")
	}
	log.Info().Msg("HTTP server stopped")

	log.Info().Msg("user-service stopped gracefully")
}

// userIDMiddleware extracts X-User-ID from the request header (set by api-gateway)
// and stores it in the Gin context for downstream handlers.
func userIDMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		if userID := c.GetHeader("X-User-ID"); userID != "" {
			c.Set("user_id", userID)
		}
		c.Next()
	}
}

func errString(err error) string {
	if err != nil {
		return err.Error()
	}
	return "ok"
}

// startStatusCleanupJob runs immediately on startup, then every hour,
// and deletes expired statuses. It exits when ctx is cancelled.
func startStatusCleanupJob(ctx context.Context, statusRepo repository.StatusRepository, log zerolog.Logger) {
	runCleanup := func() {
		opCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
		count, err := statusRepo.DeleteExpired(opCtx)
		cancel()
		if err != nil {
			log.Error().Err(err).Msg("status cleanup: failed to delete expired statuses")
		} else if count > 0 {
			log.Info().Int64("deleted", count).Msg("status cleanup: removed expired statuses")
		}
	}

	runCleanup()

	ticker := time.NewTicker(1 * time.Hour)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			runCleanup()
		}
	}
}
