package metrics

import (
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	httpRequestsTotal = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "http_requests_total",
			Help: "Total number of HTTP requests",
		},
		[]string{"service", "method", "path", "status"},
	)

	httpRequestDuration = prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "http_request_duration_seconds",
			Help:    "HTTP request duration in seconds",
			Buckets: prometheus.DefBuckets,
		},
		[]string{"service", "method", "path"},
	)

	activeConnections = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Name: "active_connections",
			Help: "Number of active connections",
		},
		[]string{"service", "type"},
	)
)

func init() {
	prometheus.MustRegister(httpRequestsTotal, httpRequestDuration, activeConnections)
}

// GinMiddleware returns a Gin middleware that records HTTP metrics.
func GinMiddleware(serviceName string) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		path := c.FullPath()
		if path == "" {
			path = "unknown"
		}

		c.Next()

		duration := time.Since(start).Seconds()
		status := strconv.Itoa(c.Writer.Status())

		httpRequestsTotal.WithLabelValues(serviceName, c.Request.Method, path, status).Inc()
		httpRequestDuration.WithLabelValues(serviceName, c.Request.Method, path).Observe(duration)
	}
}

// RegisterMetricsEndpoint adds /metrics to the Gin engine.
func RegisterMetricsEndpoint(engine *gin.Engine) {
	engine.GET("/metrics", gin.WrapH(promhttp.Handler()))
}

// IncrementConnections increments the active connection gauge.
func IncrementConnections(service, connType string) {
	activeConnections.WithLabelValues(service, connType).Inc()
}

// DecrementConnections decrements the active connection gauge.
func DecrementConnections(service, connType string) {
	activeConnections.WithLabelValues(service, connType).Dec()
}
