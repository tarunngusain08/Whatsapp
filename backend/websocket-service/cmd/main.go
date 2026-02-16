package main

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/caarlos0/env/v11"
	"github.com/gin-gonic/gin"
	"github.com/nats-io/nats.go"
	"github.com/redis/go-redis/v9"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	authv1 "github.com/whatsapp-clone/backend/proto/auth/v1"
	chatv1 "github.com/whatsapp-clone/backend/proto/chat/v1"
	messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"

	"github.com/whatsapp-clone/backend/pkg/logger"
	"github.com/whatsapp-clone/backend/pkg/metrics"
	"github.com/whatsapp-clone/backend/pkg/middleware"
	"github.com/whatsapp-clone/backend/pkg/tracing"
	"github.com/whatsapp-clone/backend/websocket-service/config"
	"github.com/whatsapp-clone/backend/websocket-service/internal/handler"
	"github.com/whatsapp-clone/backend/websocket-service/internal/model"
	"github.com/whatsapp-clone/backend/websocket-service/internal/service"
)

func main() {
	var cfg config.Config
	if err := env.Parse(&cfg); err != nil {
		panic("failed to parse config: " + err.Error())
	}

	log := logger.New("websocket-service", cfg.LogLevel)

	// Initialize OpenTelemetry tracing
	shutdownTracer, err := tracing.Init(context.Background(), "websocket-service", cfg.OTLPEndpoint)
	if err != nil {
		log.Warn().Err(err).Msg("failed to init tracing, continuing without it")
	} else {
		defer shutdownTracer(context.Background())
	}

	// --- Redis ---
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	rdb := redis.NewClient(&redis.Options{
		Addr:     cfg.RedisAddr,
		Password: cfg.RedisPassword,
	})
	if err := rdb.Ping(ctx).Err(); err != nil {
		log.Fatal().Err(err).Msg("failed to connect to Redis")
	}
	defer rdb.Close()
	log.Info().Str("addr", cfg.RedisAddr).Msg("connected to Redis")

	// --- NATS + JetStream ---
	nc, err := nats.Connect(cfg.NATSUrl,
		nats.RetryOnFailedConnect(true),
		nats.MaxReconnects(-1),
		nats.ReconnectWait(2*time.Second),
		nats.DisconnectErrHandler(func(_ *nats.Conn, err error) {
			log.Warn().Err(err).Msg("NATS disconnected")
		}),
		nats.ReconnectHandler(func(_ *nats.Conn) {
			log.Info().Msg("NATS reconnected")
		}),
	)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect to NATS")
	}
	defer nc.Drain()
	log.Info().Str("url", cfg.NATSUrl).Msg("connected to NATS")

	js, err := nc.JetStream()
	if err != nil {
		log.Fatal().Err(err).Msg("failed to create JetStream context")
	}

	// --- gRPC Connections ---
	authConn, err := grpc.NewClient(cfg.AuthGRPCAddr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatal().Err(err).Str("addr", cfg.AuthGRPCAddr).Msg("failed to create auth gRPC client")
	}
	defer authConn.Close()

	msgConn, err := grpc.NewClient(cfg.MessageGRPCAddr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatal().Err(err).Str("addr", cfg.MessageGRPCAddr).Msg("failed to create message gRPC client")
	}
	defer msgConn.Close()

	chatConn, err := grpc.NewClient(cfg.ChatGRPCAddr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatal().Err(err).Str("addr", cfg.ChatGRPCAddr).Msg("failed to create chat gRPC client")
	}
	defer chatConn.Close()

	authClient := authv1.NewAuthServiceClient(authConn)
	messageClient := messagev1.NewMessageServiceClient(msgConn)
	chatClient := chatv1.NewChatServiceClient(chatConn)

	log.Info().Msg("gRPC connections established")

	// --- Hub & Service ---
	hub := model.NewHub()

	wsSvc := service.NewWebSocketService(
		hub, rdb, nc, js,
		messageClient, chatClient,
		&cfg, log,
	)

	authVal := &grpcAuthValidator{client: authClient}

	// Start NATS consumers in background.
	if err := wsSvc.StartNATSConsumers(context.Background()); err != nil {
		log.Fatal().Err(err).Msg("failed to start NATS consumers")
	}
	log.Info().Msg("NATS consumers started")

	// --- HTTP Server ---
	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(
		middleware.RequestID(),
		middleware.Logger(log),
		middleware.Recovery(log),
		metrics.GinMiddleware("websocket-service"),
		tracing.GinMiddleware("websocket-service"),
	)

	wsHandler := handler.NewWSHandler(hub, wsSvc, authVal, &cfg, log)

	router.GET("/ws", gin.WrapF(wsHandler.ServeWS))

	router.GET("/health", func(c *gin.Context) {
		redisErr := rdb.Ping(c.Request.Context()).Err()
		natsOK := nc.IsConnected()
		if redisErr != nil || !natsOK {
			c.JSON(http.StatusServiceUnavailable, gin.H{
				"status": "unhealthy",
				"redis":  errString(redisErr),
				"nats":   fmt.Sprintf("connected=%v", natsOK),
			})
			return
		}
		c.JSON(http.StatusOK, gin.H{"status": "healthy"})
	})

	router.GET("/ready", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ready"})
	})

	// Prometheus metrics endpoint
	metrics.RegisterMetricsEndpoint(router)

	httpServer := &http.Server{
		Addr:         cfg.HTTPPort,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	go func() {
		log.Info().Str("port", cfg.HTTPPort).Msg("starting HTTP server")
		if err := httpServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal().Err(err).Msg("HTTP server failed")
		}
	}()

	// --- Graceful Shutdown ---
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	sig := <-quit
	log.Info().Str("signal", sig.String()).Msg("shutting down")

	wsSvc.GracefulShutdown()
	log.Info().Msg("websocket connections closed")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer shutdownCancel()

	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		log.Error().Err(err).Msg("HTTP server shutdown error")
	}
	log.Info().Msg("HTTP server stopped")

	log.Info().Msg("websocket-service stopped gracefully")
}

// grpcAuthValidator validates tokens via the auth-service gRPC.
type grpcAuthValidator struct {
	client authv1.AuthServiceClient
}

func (v *grpcAuthValidator) ValidateToken(ctx context.Context, token string) (string, string, error) {
	resp, err := v.client.ValidateToken(ctx, &authv1.ValidateTokenRequest{Token: token})
	if err != nil {
		return "", "", fmt.Errorf("auth gRPC error: %w", err)
	}
	if !resp.Valid {
		return "", "", fmt.Errorf("invalid token")
	}
	return resp.UserId, resp.Phone, nil
}

func errString(err error) string {
	if err != nil {
		return err.Error()
	}
	return "ok"
}
