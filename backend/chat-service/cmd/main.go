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
	"github.com/nats-io/nats.go"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"

	"github.com/whatsapp-clone/backend/chat-service/config"
	"github.com/whatsapp-clone/backend/chat-service/internal/handler"
	"github.com/whatsapp-clone/backend/chat-service/internal/repository"
	"github.com/whatsapp-clone/backend/chat-service/internal/service"
	"github.com/whatsapp-clone/backend/pkg/logger"
	"github.com/whatsapp-clone/backend/pkg/metrics"
	"github.com/whatsapp-clone/backend/pkg/middleware"
	"github.com/whatsapp-clone/backend/pkg/tracing"
	chatv1 "github.com/whatsapp-clone/backend/proto/chat/v1"
	messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"
)

func main() {
	var cfg config.Config
	if err := env.Parse(&cfg); err != nil {
		panic("failed to parse config: " + err.Error())
	}

	log := logger.New("chat-service", cfg.LogLevel)

	// Initialize OpenTelemetry tracing
	shutdownTracer, err := tracing.Init(context.Background(), "chat-service", cfg.OTLPEndpoint)
	if err != nil {
		log.Warn().Err(err).Msg("failed to init tracing, continuing without it")
	} else {
		defer shutdownTracer(context.Background())
	}

	// --- PostgreSQL ---
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

	// --- NATS + JetStream ---
	nc, err := nats.Connect(cfg.NATSUrl)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to NATS")
	}
	defer nc.Close()
	log.Info().Msg("connected to NATS")

	js, err := nc.JetStream()
	if err != nil {
		log.Fatal().Err(err).Msg("failed to get JetStream context")
	}

	// Create CHATS stream if not exists.
	_, err = js.AddStream(&nats.StreamConfig{
		Name:     "CHATS",
		Subjects: []string{"chat.>", "group.>"},
	})
	if err != nil {
		log.Warn().Err(err).Msg("failed to create CHATS stream (may already exist)")
	}

	// --- gRPC client to message-service ---
	msgConn, err := grpc.NewClient(
		cfg.MessageGRPC,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to create message-service gRPC client")
	}
	defer msgConn.Close()

	messageClient := messagev1.NewMessageServiceClient(msgConn)

	// --- Repositories, Service ---
	chatRepo := repository.NewChatPostgres(pgPool)
	chatSvc := service.NewChatService(chatRepo, messageClient, js, log)

	// --- HTTP Server ---
	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(
		middleware.RequestID(),
		middleware.Logger(log),
		middleware.Recovery(log),
		metrics.GinMiddleware("chat-service"),
		tracing.GinMiddleware("chat-service"),
		middleware.CORS([]string{"*"}),
	)

	// Health check.
	router.GET("/health", func(c *gin.Context) {
		pgErr := pgPool.Ping(c.Request.Context())
		natsOk := nc.IsConnected()
		if pgErr != nil || !natsOk {
			c.JSON(http.StatusServiceUnavailable, gin.H{
				"status":   "unhealthy",
				"postgres": errString(pgErr),
				"nats":     boolToStatus(natsOk),
			})
			return
		}
		c.JSON(http.StatusOK, gin.H{"status": "healthy"})
	})

	httpHandler := handler.NewHTTPHandler(chatSvc, log)
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
	grpcHandler := handler.NewGRPCHandler(chatRepo, log)
	chatv1.RegisterChatServiceServer(grpcServer, grpcHandler)

	healthServer := health.NewServer()
	healthpb.RegisterHealthServer(grpcServer, healthServer)
	healthServer.SetServingStatus("chat.v1.ChatService", healthpb.HealthCheckResponse_SERVING)

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

	log.Info().Msg("chat-service stopped gracefully")
}

func errString(err error) string {
	if err != nil {
		return err.Error()
	}
	return "ok"
}

func boolToStatus(ok bool) string {
	if ok {
		return "ok"
	}
	return "disconnected"
}
