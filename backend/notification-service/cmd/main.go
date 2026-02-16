package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/caarlos0/env/v11"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/nats-io/nats.go"
	"github.com/redis/go-redis/v9"

	"github.com/whatsapp-clone/backend/notification-service/config"
	"github.com/whatsapp-clone/backend/notification-service/internal/repository"
	"github.com/whatsapp-clone/backend/notification-service/internal/service"
	"github.com/whatsapp-clone/backend/pkg/health"
	"github.com/whatsapp-clone/backend/pkg/logger"
	"github.com/whatsapp-clone/backend/pkg/metrics"
	"github.com/whatsapp-clone/backend/pkg/middleware"
	"github.com/whatsapp-clone/backend/pkg/tracing"
)

func main() {
	var cfg config.Config
	if err := env.Parse(&cfg); err != nil {
		panic("failed to parse config: " + err.Error())
	}

	log := logger.New("notification-service", cfg.LogLevel)

	// Initialize OpenTelemetry tracing
	shutdownTracer, err := tracing.Init(context.Background(), "notification-service", cfg.OTLPEndpoint)
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
		log.Fatal().Err(err).Msg("failed to create PostgreSQL pool")
	}
	defer pgPool.Close()

	if err := pgPool.Ping(ctx); err != nil {
		log.Fatal().Err(err).Msg("failed to ping PostgreSQL")
	}
	log.Info().Msg("connected to PostgreSQL")

	// --- Redis ---
	rdb := redis.NewClient(&redis.Options{
		Addr:     cfg.RedisAddr,
		Password: cfg.RedisPassword,
	})
	defer rdb.Close()

	if err := rdb.Ping(ctx).Err(); err != nil {
		log.Fatal().Err(err).Msg("failed to ping Redis")
	}
	log.Info().Msg("connected to Redis")

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

	// --- Repositories ---
	tokenRepo := repository.NewDeviceTokenRepository(pgPool, log)
	presenceRepo := repository.NewPresenceRepository(rdb, log)
	muteRepo := repository.NewParticipantRepository(pgPool, log)

	// --- FCM Client ---
	var fcmClient service.FCMClient

	if cfg.FCMCredentialsPath != "" {
		credBytes, readErr := os.ReadFile(cfg.FCMCredentialsPath)
		if readErr != nil {
			log.Warn().Err(readErr).Msg("failed to read FCM credentials file, falling back to mock")
			fcmClient = service.NewMockFCMClient(log)
		} else {
			var creds struct {
				ProjectID string `json:"project_id"`
			}
			if err := json.Unmarshal(credBytes, &creds); err != nil {
				log.Warn().Err(err).Msg("failed to parse FCM credentials JSON, falling back to mock")
				fcmClient = service.NewMockFCMClient(log)
			} else {
				fcmClient = service.NewHTTPFCMClient(
					creds.ProjectID,
					string(credBytes),
					tokenRepo,
					cfg.MaxRetries,
					log,
				)
				log.Info().Str("project_id", creds.ProjectID).Msg("FCM client initialized")
			}
		}
	} else {
		log.Warn().Msg("FCM credentials path not set, using mock FCM client")
		fcmClient = service.NewMockFCMClient(log)
	}

	// --- Batcher ---
	batcher := service.NewNotificationBatcher(cfg.GroupBatchWindow, fcmClient, tokenRepo, log)

	// --- NATS Consumer ---
	consumer := service.NewConsumer(js, presenceRepo, muteRepo, tokenRepo, fcmClient, batcher, log)

	consumerCtx, consumerCancel := context.WithCancel(context.Background())
	go func() {
		if err := consumer.Start(consumerCtx); err != nil {
			log.Error().Err(err).Msg("NATS consumer error")
		}
	}()

	// --- HTTP Server (health checks only) ---
	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(
		middleware.RequestID(),
		middleware.Logger(log),
		middleware.Recovery(log),
		metrics.GinMiddleware("notification-service"),
		tracing.GinMiddleware("notification-service"),
	)

	healthHandler := health.NewHandler()
	healthHandler.AddChecker("postgres", func(ctx context.Context) error {
		return pgPool.Ping(ctx)
	})
	healthHandler.AddChecker("redis", func(ctx context.Context) error {
		return rdb.Ping(ctx).Err()
	})
	healthHandler.AddChecker("nats", func(_ context.Context) error {
		if !nc.IsConnected() {
			return fmt.Errorf("NATS disconnected")
		}
		return nil
	})
	healthHandler.RegisterRoutes(router)

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
		log.Info().Str("port", cfg.HTTPPort).Msg("starting HTTP server (health only)")
		if err := httpServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal().Err(err).Msg("HTTP server failed")
		}
	}()

	// --- Graceful Shutdown ---
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	sig := <-quit
	log.Info().Str("signal", sig.String()).Msg("shutting down")

	consumerCancel()
	batcher.Shutdown()

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer shutdownCancel()

	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		log.Error().Err(err).Msg("HTTP server shutdown error")
	}
	log.Info().Msg("HTTP server stopped")
	log.Info().Msg("notification-service stopped gracefully")
}
