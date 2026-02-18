package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/redis/go-redis/v9"

	"github.com/whatsapp-clone/backend/api-gateway/config"
	"github.com/whatsapp-clone/backend/api-gateway/internal/handler"
	"github.com/whatsapp-clone/backend/api-gateway/internal/model"
	"github.com/whatsapp-clone/backend/api-gateway/internal/service"
	pkgcfg "github.com/whatsapp-clone/backend/pkg/config"
	"github.com/whatsapp-clone/backend/pkg/grpcclient"
	"github.com/whatsapp-clone/backend/pkg/health"
	"github.com/whatsapp-clone/backend/pkg/logger"
	"github.com/whatsapp-clone/backend/pkg/metrics"
	"github.com/whatsapp-clone/backend/pkg/middleware"
	"github.com/whatsapp-clone/backend/pkg/tracing"
)

func main() {
	var cfg config.Config
	if err := pkgcfg.Load(&cfg); err != nil {
		panic("failed to load config: " + err.Error())
	}

	log := logger.New("api-gateway", cfg.LogLevel)

	// Initialize OpenTelemetry tracing
	shutdownTracer, err := tracing.Init(context.Background(), "api-gateway", cfg.OTLPEndpoint)
	if err != nil {
		log.Warn().Err(err).Msg("failed to init tracing, continuing without it")
	} else {
		defer shutdownTracer(context.Background())
	}

	// Redis client
	rdb := redis.NewClient(&redis.Options{
		Addr:     cfg.RedisAddr,
		Password: cfg.RedisPassword,
	})

	// gRPC connection to auth-service
	grpcFactory := grpcclient.NewFactory()
	defer grpcFactory.Close()

	authConn, err := grpcFactory.GetConnection(grpcclient.Config{
		Address: cfg.AuthGRPCAddr,
		Timeout: cfg.GRPCTimeout,
	})
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to auth-service gRPC")
	}

	// Services
	authValidator := service.NewAuthValidator(authConn)
	rateLimiter := service.NewRedisRateLimiter(rdb)

	// Gin engine
	gin.SetMode(gin.ReleaseMode)
	engine := gin.New()

	// Global middleware chain
	corsOrigins := strings.Split(cfg.CORSOrigins, ",")
	engine.Use(
		middleware.Recovery(log),
		middleware.RequestID(),
		middleware.Logger(log),
		metrics.GinMiddleware("api-gateway"),
		tracing.GinMiddleware("api-gateway"),
		middleware.CORS(corsOrigins),
	)

	// Body size limiter
	engine.Use(func(c *gin.Context) {
		c.Request.Body = http.MaxBytesReader(c.Writer, c.Request.Body, cfg.MaxBodySize)
		c.Next()
	})

	// Health checks
	healthHandler := health.NewHandler()
	healthHandler.AddChecker("redis", func(ctx context.Context) error {
		return rdb.Ping(ctx).Err()
	})
	healthHandler.RegisterRoutes(engine)

	// Middleware instances
	authMW := handler.AuthMiddleware(authValidator)
	rateLimitMW := handler.RateLimitMiddleware(rateLimiter, cfg.RateLimitRPS)

	// Public routes (no auth required)
	publicRoutes := []model.RouteTarget{
		{PathPrefix: "/api/v1/auth", TargetURL: cfg.AuthHTTPAddr, StripPrefix: false, RequireAuth: false},
	}

	// Protected routes (auth + rate limit required)
	protectedRoutes := []model.RouteTarget{
		{PathPrefix: "/api/v1/messages", TargetURL: cfg.MessageHTTPAddr, StripPrefix: false, RequireAuth: true},
		{PathPrefix: "/api/v1/media", TargetURL: cfg.MediaHTTPAddr, StripPrefix: false, RequireAuth: true},
	}

	// Register proxy routes
	handler.RegisterProxyRoutes(engine, publicRoutes, nil, nil)
	handler.RegisterProxyRoutes(engine, protectedRoutes, authMW, rateLimitMW)

	// User routes with path rewriting for client-compatible block/unblock paths
	handler.RegisterUserRoutes(engine, cfg.UserHTTPAddr, authMW, rateLimitMW)

	// Chat routes with sub-path rewriting for chat-scoped messages.
	// Client calls /api/v1/chats/:chatId/messages/* which needs to go to
	// the message-service, while other /api/v1/chats/* paths go to chat-service.
	handler.RegisterChatRoutes(engine, cfg.ChatHTTPAddr, cfg.MessageHTTPAddr, authMW, rateLimitMW)

	// Notification device routes: client calls /api/v1/notifications/devices/*
	// which map to user-service /api/v1/users/devices/*
	handler.RegisterNotificationRoutes(engine, cfg.UserHTTPAddr, authMW, rateLimitMW)

	// WebSocket proxy (protected)
	engine.GET("/ws", authMW, handler.WSProxyHandler(cfg.WSAddr))

	// Prometheus metrics endpoint
	metrics.RegisterMetricsEndpoint(engine)

	// HTTP server with graceful shutdown
	srv := &http.Server{
		Addr:              cfg.Port,
		Handler:           engine,
		ReadHeaderTimeout: 10 * time.Second,
		WriteTimeout:      60 * time.Second,
		IdleTimeout:       120 * time.Second,
	}

	go func() {
		log.Info().Str("addr", cfg.Port).Msg("api-gateway listening")
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal().Err(err).Msg("server failed")
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Info().Msg("shutting down api-gateway")

	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		log.Error().Err(err).Msg("server forced to shutdown")
	}

	if err := rdb.Close(); err != nil {
		log.Error().Err(err).Msg("redis close error")
	}

	log.Info().Msg("api-gateway stopped")
}
