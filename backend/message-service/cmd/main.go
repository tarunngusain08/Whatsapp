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
	"github.com/nats-io/nats.go"
	"github.com/rs/zerolog"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"

	"github.com/whatsapp-clone/backend/message-service/config"
	"github.com/whatsapp-clone/backend/message-service/internal/handler"
	"github.com/whatsapp-clone/backend/message-service/internal/repository"
	"github.com/whatsapp-clone/backend/message-service/internal/service"
	"github.com/whatsapp-clone/backend/pkg/logger"
	"github.com/whatsapp-clone/backend/pkg/metrics"
	"github.com/whatsapp-clone/backend/pkg/middleware"
	"github.com/whatsapp-clone/backend/pkg/tracing"
	chatv1 "github.com/whatsapp-clone/backend/proto/chat/v1"
	messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"
	userv1 "github.com/whatsapp-clone/backend/proto/user/v1"
)

func main() {
	var cfg config.Config
	if err := env.Parse(&cfg); err != nil {
		panic("failed to parse config: " + err.Error())
	}

	log := logger.New("message-service", cfg.LogLevel)

	// Initialize OpenTelemetry tracing
	shutdownTracer, err := tracing.Init(context.Background(), "message-service", cfg.OTLPEndpoint)
	if err != nil {
		log.Warn().Err(err).Msg("failed to init tracing, continuing without it")
	} else {
		defer shutdownTracer(context.Background())
	}

	// --- MongoDB ---
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	mongoClient, err := mongo.Connect(ctx, options.Client().ApplyURI(cfg.MongoURI))
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to MongoDB")
	}
	defer func() {
		if err := mongoClient.Disconnect(context.Background()); err != nil {
			log.Error().Err(err).Msg("error disconnecting from MongoDB")
		}
	}()

	if err := mongoClient.Ping(ctx, readpref.Primary()); err != nil {
		log.Fatal().Err(err).Msg("failed to ping MongoDB")
	}
	log.Info().Msg("connected to MongoDB")

	mongoDB := mongoClient.Database(cfg.MongoDB)

	// --- NATS ---
	nc, err := nats.Connect(cfg.NATSUrl)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to NATS")
	}
	defer nc.Drain()
	log.Info().Msg("connected to NATS")

	js, err := nc.JetStream()
	if err != nil {
		log.Fatal().Err(err).Msg("failed to get JetStream context")
	}

	// --- User-service gRPC client ---
	userConn, err := grpc.NewClient(cfg.UserServiceGRPC, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to user-service gRPC")
	}
	defer userConn.Close()
	userClient := userv1.NewUserServiceClient(userConn)
	log.Info().Str("addr", cfg.UserServiceGRPC).Msg("user-service gRPC client created")

	// --- Chat-service gRPC client ---
	chatConn, err := grpc.NewClient(cfg.ChatServiceGRPC, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to chat-service gRPC")
	}
	defer chatConn.Close()
	chatClient := chatv1.NewChatServiceClient(chatConn)
	log.Info().Str("addr", cfg.ChatServiceGRPC).Msg("chat-service gRPC client created")

	// --- Dependencies ---
	msgRepo := repository.NewMessageMongoRepository(mongoDB, log)
	publisher := service.NewEventPublisher(js, log)
	if err := publisher.EnsureStream(); err != nil {
		log.Fatal().Err(err).Msg("failed to ensure MESSAGES stream")
	}
	log.Info().Msg("MESSAGES JetStream stream ready")

	msgSvc := service.NewMessageService(msgRepo, publisher, userClient, chatClient, log)

	// Start disappearing messages cleanup job (runs every 6 hours)
	cleaner := service.NewDisappearingMessagesCleaner(msgRepo, 6*time.Hour, log)
	cleaner.Start(context.Background())
	defer cleaner.Stop()

	// --- HTTP Server ---
	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(
		middleware.RequestID(),
		middleware.Logger(log),
		middleware.Recovery(log),
		metrics.GinMiddleware("message-service"),
		tracing.GinMiddleware("message-service"),
		middleware.CORS([]string{"*"}),
	)

	router.GET("/health", healthCheck(mongoClient, nc, log))

	httpHandler := handler.NewHTTPHandler(msgSvc, log)
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
	grpcHandler := handler.NewGRPCHandler(msgSvc)
	messagev1.RegisterMessageServiceServer(grpcServer, grpcHandler)

	healthServer := health.NewServer()
	healthpb.RegisterHealthServer(grpcServer, healthServer)
	healthServer.SetServingStatus("message.v1.MessageService", healthpb.HealthCheckResponse_SERVING)

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

	log.Info().Msg("message-service stopped gracefully")
}

func healthCheck(mongoClient *mongo.Client, nc *nats.Conn, log zerolog.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		ctx, cancel := context.WithTimeout(c.Request.Context(), 5*time.Second)
		defer cancel()

		mongoErr := mongoClient.Ping(ctx, readpref.Primary())

		natsOK := nc.IsConnected()

		if mongoErr != nil || !natsOK {
			c.JSON(http.StatusServiceUnavailable, gin.H{
				"status":  "unhealthy",
				"mongodb": errString(mongoErr),
				"nats":    boolToStatus(natsOK),
			})
			return
		}
		c.JSON(http.StatusOK, gin.H{"status": "healthy"})
	}
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
