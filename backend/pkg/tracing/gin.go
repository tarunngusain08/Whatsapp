package tracing

import (
	"fmt"

	"github.com/gin-gonic/gin"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/propagation"
	semconv "go.opentelemetry.io/otel/semconv/v1.24.0"
	"go.opentelemetry.io/otel/trace"
)

// GinMiddleware returns a Gin middleware that creates a span per request.
func GinMiddleware(serviceName string) gin.HandlerFunc {
	tracer := otel.Tracer(serviceName)
	return func(c *gin.Context) {
		savedCtx := c.Request.Context()
		defer func() { c.Request = c.Request.WithContext(savedCtx) }()

		// Extract propagation context from headers
		ctx := otel.GetTextMapPropagator().Extract(savedCtx, propagation.HeaderCarrier(c.Request.Header))

		path := c.FullPath()
		if path == "" {
			path = c.Request.URL.Path
		}
		spanName := fmt.Sprintf("%s %s", c.Request.Method, path)

		ctx, span := tracer.Start(ctx, spanName,
			trace.WithAttributes(
				semconv.HTTPMethodKey.String(c.Request.Method),
				semconv.HTTPTargetKey.String(c.Request.URL.Path),
				attribute.String("http.route", path),
			),
			trace.WithSpanKind(trace.SpanKindServer),
		)
		defer span.End()

		c.Request = c.Request.WithContext(ctx)
		c.Next()

		span.SetAttributes(
			semconv.HTTPStatusCodeKey.Int(c.Writer.Status()),
		)
	}
}
