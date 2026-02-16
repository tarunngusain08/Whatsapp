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
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"github.com/rs/zerolog"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"
	"google.golang.org/grpc"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"

	"github.com/whatsapp-clone/backend/media-service/config"
	"github.com/whatsapp-clone/backend/media-service/internal/handler"
	"github.com/whatsapp-clone/backend/media-service/internal/repository"
	"github.com/whatsapp-clone/backend/media-service/internal/service"
	"github.com/whatsapp-clone/backend/pkg/logger"
	"github.com/whatsapp-clone/backend/pkg/metrics"
	"github.com/whatsapp-clone/backend/pkg/middleware"
	"github.com/whatsapp-clone/backend/pkg/tracing"
	mediav1 "github.com/whatsapp-clone/backend/proto/media/v1"
)

func main() {
	var cfg config.Config
	if err := env.Parse(&cfg); err != nil {
		panic("failed to parse config: " + err.Error())
	}

	log := logger.New("media-service", cfg.LogLevel)

	// Initialize OpenTelemetry tracing
	shutdownTracer, err := tracing.Init(context.Background(), "media-service", cfg.OTLPEndpoint)
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

	// --- MinIO ---
	minioClient, err := minio.New(cfg.MinIOEndpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(cfg.MinIOAccessKey, cfg.MinIOSecretKey, ""),
		Secure: cfg.MinIOUseSSL,
	})
	if err != nil {
		log.Fatal().Err(err).Msg("failed to create MinIO client")
	}

	// Ensure bucket exists.
	bucketExists, err := minioClient.BucketExists(ctx, cfg.MinIOBucket)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to check MinIO bucket")
	}
	if !bucketExists {
		if err := minioClient.MakeBucket(ctx, cfg.MinIOBucket, minio.MakeBucketOptions{}); err != nil {
			log.Fatal().Err(err).Msg("failed to create MinIO bucket")
		}
		log.Info().Str("bucket", cfg.MinIOBucket).Msg("created MinIO bucket")
	}
	log.Info().Str("endpoint", cfg.MinIOEndpoint).Msg("connected to MinIO")

	// --- Repositories ---
	mediaRepo := repository.NewMediaMongoRepository(mongoDB, log)
	storageRepo := repository.NewStorageMinIORepository(minioClient, cfg.MinIOBucket, log)

	// --- Services ---
	thumbGen := service.NewThumbnailGenerator(cfg.FFmpegPath, storageRepo, log)
	mediaSvc := service.NewMediaService(&cfg, mediaRepo, storageRepo, thumbGen, log)

	// --- Start orphan cleanup goroutine ---
	cleanupCtx, cleanupCancel := context.WithCancel(context.Background())
	defer cleanupCancel()
	mediaSvc.StartCleanupJob(cleanupCtx)
	log.Info().Dur("interval", cfg.CleanupInterval).Msg("orphan cleanup job started")

	// --- HTTP Server ---
	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(
		middleware.RequestID(),
		middleware.Logger(log),
		middleware.Recovery(log),
		metrics.GinMiddleware("media-service"),
		tracing.GinMiddleware("media-service"),
		middleware.CORS([]string{"*"}),
	)

	router.GET("/health", healthCheck(mongoClient, minioClient, cfg.MinIOBucket, log))

	httpHandler := handler.NewHTTPHandler(mediaSvc, cfg.PresignedURLTTL, log)
	apiV1 := router.Group("/api/v1")
	httpHandler.RegisterRoutes(apiV1)

	// Prometheus metrics endpoint
	metrics.RegisterMetricsEndpoint(router)

	httpServer := &http.Server{
		Addr:         cfg.HTTPPort,
		Handler:      router,
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 60 * time.Second,
		IdleTimeout:  120 * time.Second,
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
	grpcHandler := handler.NewGRPCHandler(mediaSvc)
	mediav1.RegisterMediaServiceServer(grpcServer, grpcHandler)

	healthServer := health.NewServer()
	healthpb.RegisterHealthServer(grpcServer, healthServer)
	healthServer.SetServingStatus("media.v1.MediaService", healthpb.HealthCheckResponse_SERVING)

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

	cleanupCancel()

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer shutdownCancel()

	grpcServer.GracefulStop()
	log.Info().Msg("gRPC server stopped")

	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		log.Error().Err(err).Msg("HTTP server shutdown error")
	}
	log.Info().Msg("HTTP server stopped")

	log.Info().Msg("media-service stopped gracefully")
}

func healthCheck(mongoClient *mongo.Client, minioClient *minio.Client, bucket string, log zerolog.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		ctx, cancel := context.WithTimeout(c.Request.Context(), 5*time.Second)
		defer cancel()

		mongoErr := mongoClient.Ping(ctx, readpref.Primary())

		_, minioErr := minioClient.BucketExists(ctx, bucket)

		if mongoErr != nil || minioErr != nil {
			c.JSON(http.StatusServiceUnavailable, gin.H{
				"status":  "unhealthy",
				"mongodb": errString(mongoErr),
				"minio":   errString(minioErr),
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
