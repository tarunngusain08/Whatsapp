# WhatsApp Clone Backend — Low-Level Design (LLD)

> **Version:** 1.0  
> **Date:** 2026-02-16  
> **Stack:** Go 1.22+, Gin, gRPC, PostgreSQL 16, MongoDB 7, Redis 7, NATS JetStream, MinIO  
> **Infrastructure:** Docker, Kind (K8s), Helm 3, ngrok  
> **Observability:** zerolog, Prometheus, OpenTelemetry + Jaeger

---

## Table of Contents

1. [Shared Libraries (`backend/pkg/`)](#1-shared-libraries-backendpkg)
2. [Proto Definitions (`backend/proto/`)](#2-proto-definitions-backendproto)
3. [Per-Service Detailed Design](#3-per-service-detailed-design)
   - 3.1 [api-gateway](#31-api-gateway)
   - 3.2 [auth-service](#32-auth-service)
   - 3.3 [user-service](#33-user-service)
   - 3.4 [chat-service](#34-chat-service)
   - 3.5 [message-service](#35-message-service)
   - 3.6 [notification-service](#36-notification-service)
   - 3.7 [media-service](#37-media-service)
   - 3.8 [websocket-service](#38-websocket-service)
4. [Database Access Patterns](#4-database-access-patterns)
5. [Error Handling Strategy](#5-error-handling-strategy)
6. [Middleware Chain](#6-middleware-chain)
7. [Graceful Shutdown Pattern](#7-graceful-shutdown-pattern)
8. [Testing Strategy](#8-testing-strategy)
9. [Migration Strategy](#9-migration-strategy)
10. [Configuration Management](#10-configuration-management)

---

## 1. Shared Libraries (`backend/pkg/`)

### 1.1 `pkg/jwt/` — JWT Creation & Validation

```go
// pkg/jwt/jwt.go
package jwt

import (
    "errors"
    "time"

    jwtgo "github.com/golang-jwt/jwt/v5"
)

var (
    ErrTokenExpired  = errors.New("token has expired")
    ErrTokenInvalid  = errors.New("token is invalid")
    ErrTokenMalformed = errors.New("token is malformed")
)

// Claims represents the JWT payload for access tokens.
type Claims struct {
    jwtgo.RegisteredClaims
    UserID string `json:"user_id"`
    Phone  string `json:"phone"`
}

// Manager handles JWT creation and validation.
type Manager struct {
    signingKey []byte
    accessTTL  time.Duration
}

// NewManager creates a Manager with the given HS256 signing key and access token TTL.
func NewManager(signingKey string, accessTTL time.Duration) *Manager {
    return &Manager{
        signingKey: []byte(signingKey),
        accessTTL:  accessTTL,
    }
}

// CreateAccessToken generates a signed HS256 JWT with user_id and phone claims.
func (m *Manager) CreateAccessToken(userID, phone string) (string, error) {
    now := time.Now()
    claims := Claims{
        RegisteredClaims: jwtgo.RegisteredClaims{
            ExpiresAt: jwtgo.NewNumericDate(now.Add(m.accessTTL)),
            IssuedAt:  jwtgo.NewNumericDate(now),
            Issuer:    "whatsapp-auth",
        },
        UserID: userID,
        Phone:  phone,
    }
    token := jwtgo.NewWithClaims(jwtgo.SigningMethodHS256, claims)
    return token.SignedString(m.signingKey)
}

// ValidateToken parses and validates the token, returning claims on success.
func (m *Manager) ValidateToken(tokenStr string) (*Claims, error) {
    token, err := jwtgo.ParseWithClaims(tokenStr, &Claims{}, func(t *jwtgo.Token) (interface{}, error) {
        if _, ok := t.Method.(*jwtgo.SigningMethodHMAC); !ok {
            return nil, ErrTokenInvalid
        }
        return m.signingKey, nil
    })
    if err != nil {
        if errors.Is(err, jwtgo.ErrTokenExpired) {
            return nil, ErrTokenExpired
        }
        return nil, ErrTokenInvalid
    }
    claims, ok := token.Claims.(*Claims)
    if !ok || !token.Valid {
        return nil, ErrTokenInvalid
    }
    return claims, nil
}
```

### 1.2 `pkg/logger/` — zerolog Wrapper with Request-ID Correlation

```go
// pkg/logger/logger.go
package logger

import (
    "io"
    "os"
    "time"

    "github.com/rs/zerolog"
)

// New creates a zerolog.Logger configured for the service.
// level can be "debug", "info", "warn", "error".
func New(serviceName, level string) zerolog.Logger {
    var lvl zerolog.Level
    switch level {
    case "debug":
        lvl = zerolog.DebugLevel
    case "warn":
        lvl = zerolog.WarnLevel
    case "error":
        lvl = zerolog.ErrorLevel
    default:
        lvl = zerolog.InfoLevel
    }

    var w io.Writer = os.Stdout
    if os.Getenv("LOG_FORMAT") == "pretty" {
        w = zerolog.ConsoleWriter{Out: os.Stdout, TimeFormat: time.RFC3339}
    }

    return zerolog.New(w).
        Level(lvl).
        With().
        Timestamp().
        Str("service", serviceName).
        Logger()
}

// WithRequestID returns a sub-logger with the given request_id.
func WithRequestID(log zerolog.Logger, requestID string) zerolog.Logger {
    return log.With().Str("request_id", requestID).Logger()
}

// WithUserID returns a sub-logger with the given user_id.
func WithUserID(log zerolog.Logger, userID string) zerolog.Logger {
    return log.With().Str("user_id", userID).Logger()
}
```

### 1.3 `pkg/errors/` — Custom Error Types

```go
// pkg/errors/errors.go
package errors

import "fmt"

// Error codes used across all services.
const (
    CodeInternal         = "INTERNAL_ERROR"
    CodeNotFound         = "NOT_FOUND"
    CodeBadRequest       = "BAD_REQUEST"
    CodeUnauthorized     = "UNAUTHORIZED"
    CodeForbidden        = "FORBIDDEN"
    CodeConflict         = "CONFLICT"
    CodeTooManyRequests  = "TOO_MANY_REQUESTS"
    CodeValidation       = "VALIDATION_ERROR"
    CodeOTPExpired       = "OTP_EXPIRED"
    CodeOTPInvalid       = "OTP_INVALID"
    CodeOTPMaxAttempts   = "OTP_MAX_ATTEMPTS"
    CodeTokenExpired     = "TOKEN_EXPIRED"
    CodeTokenInvalid     = "TOKEN_INVALID"
    CodeMediaTooLarge    = "MEDIA_TOO_LARGE"
    CodeMediaInvalidType = "MEDIA_INVALID_TYPE"
    CodeChatNotFound     = "CHAT_NOT_FOUND"
    CodeNotChatMember    = "NOT_CHAT_MEMBER"
    CodeNotAdmin         = "NOT_ADMIN"
    CodeAlreadyMember    = "ALREADY_MEMBER"
    CodeUserBlocked      = "USER_BLOCKED"
)

// AppError is the standard application error type propagated through all layers.
type AppError struct {
    Code       string `json:"code"`
    Message    string `json:"message"`
    HTTPStatus int    `json:"-"`
    Err        error  `json:"-"` // wrapped underlying error
}

func (e *AppError) Error() string {
    if e.Err != nil {
        return fmt.Sprintf("%s: %s: %v", e.Code, e.Message, e.Err)
    }
    return fmt.Sprintf("%s: %s", e.Code, e.Message)
}

func (e *AppError) Unwrap() error {
    return e.Err
}

// Constructor helpers

func NewInternal(msg string, err error) *AppError {
    return &AppError{Code: CodeInternal, Message: msg, HTTPStatus: 500, Err: err}
}

func NewNotFound(msg string) *AppError {
    return &AppError{Code: CodeNotFound, Message: msg, HTTPStatus: 404}
}

func NewBadRequest(msg string) *AppError {
    return &AppError{Code: CodeBadRequest, Message: msg, HTTPStatus: 400}
}

func NewUnauthorized(msg string) *AppError {
    return &AppError{Code: CodeUnauthorized, Message: msg, HTTPStatus: 401}
}

func NewForbidden(msg string) *AppError {
    return &AppError{Code: CodeForbidden, Message: msg, HTTPStatus: 403}
}

func NewConflict(msg string) *AppError {
    return &AppError{Code: CodeConflict, Message: msg, HTTPStatus: 409}
}

func NewTooManyRequests(msg string) *AppError {
    return &AppError{Code: CodeTooManyRequests, Message: msg, HTTPStatus: 429}
}

func NewValidation(msg string) *AppError {
    return &AppError{Code: CodeValidation, Message: msg, HTTPStatus: 422}
}

// Wrap wraps an existing error into an AppError preserving the code and status.
func Wrap(code string, httpStatus int, msg string, err error) *AppError {
    return &AppError{Code: code, Message: msg, HTTPStatus: httpStatus, Err: err}
}
```

### 1.4 `pkg/middleware/` — Gin Middlewares

```go
// pkg/middleware/request_id.go
package middleware

import (
    "github.com/gin-gonic/gin"
    "github.com/google/uuid"
)

const RequestIDHeader = "X-Request-ID"

// RequestID injects a unique request_id into the Gin context and response header.
func RequestID() gin.HandlerFunc {
    return func(c *gin.Context) {
        rid := c.GetHeader(RequestIDHeader)
        if rid == "" {
            rid = uuid.New().String()
        }
        c.Set("request_id", rid)
        c.Header(RequestIDHeader, rid)
        c.Next()
    }
}
```

```go
// pkg/middleware/logger.go
package middleware

import (
    "time"

    "github.com/gin-gonic/gin"
    "github.com/rs/zerolog"
)

// Logger logs each request with method, path, status, latency, and request_id.
func Logger(log zerolog.Logger) gin.HandlerFunc {
    return func(c *gin.Context) {
        start := time.Now()
        path := c.Request.URL.Path

        c.Next()

        latency := time.Since(start)
        status := c.Writer.Status()
        rid, _ := c.Get("request_id")

        log.Info().
            Str("request_id", rid.(string)).
            Str("method", c.Request.Method).
            Str("path", path).
            Int("status", status).
            Dur("latency", latency).
            Str("client_ip", c.ClientIP()).
            Msg("request completed")
    }
}
```

```go
// pkg/middleware/recovery.go
package middleware

import (
    "net/http"

    "github.com/gin-gonic/gin"
    "github.com/rs/zerolog"
)

// Recovery recovers from panics and logs the stack trace.
func Recovery(log zerolog.Logger) gin.HandlerFunc {
    return func(c *gin.Context) {
        defer func() {
            if err := recover(); err != nil {
                rid, _ := c.Get("request_id")
                log.Error().
                    Str("request_id", rid.(string)).
                    Interface("error", err).
                    Msg("panic recovered")
                c.AbortWithStatusJSON(http.StatusInternalServerError, gin.H{
                    "success": false,
                    "error":   gin.H{"code": "INTERNAL_ERROR", "message": "internal server error"},
                })
            }
        }()
        c.Next()
    }
}
```

```go
// pkg/middleware/cors.go
package middleware

import (
    "time"

    "github.com/gin-contrib/cors"
    "github.com/gin-gonic/gin"
)

// CORS returns a CORS middleware configured for the allowed origins.
func CORS(allowedOrigins []string) gin.HandlerFunc {
    return cors.New(cors.Config{
        AllowOrigins:     allowedOrigins,
        AllowMethods:     []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"},
        AllowHeaders:     []string{"Origin", "Content-Type", "Authorization", "X-Request-ID"},
        ExposeHeaders:    []string{"X-Request-ID"},
        AllowCredentials: true,
        MaxAge:           12 * time.Hour,
    })
}
```

### 1.5 `pkg/response/` — Standard API Response Envelope

```go
// pkg/response/response.go
package response

import (
    "net/http"

    "github.com/gin-gonic/gin"
    apperr "github.com/whatsapp-clone/backend/pkg/errors"
)

// SuccessResponse is the standard envelope for successful API responses.
type SuccessResponse struct {
    Success bool        `json:"success"`
    Data    interface{} `json:"data,omitempty"`
    Meta    *Meta       `json:"meta,omitempty"`
}

// Meta carries pagination metadata.
type Meta struct {
    NextCursor string `json:"next_cursor,omitempty"`
    HasMore    bool   `json:"has_more"`
    Total      int64  `json:"total,omitempty"`
}

// ErrorResponse is the standard envelope for error API responses.
type ErrorResponse struct {
    Success bool       `json:"success"`
    Error   ErrorBody  `json:"error"`
}

// ErrorBody contains the error details.
type ErrorBody struct {
    Code    string `json:"code"`
    Message string `json:"message"`
}

// OK sends a 200 success response.
func OK(c *gin.Context, data interface{}) {
    c.JSON(http.StatusOK, SuccessResponse{Success: true, Data: data})
}

// OKWithMeta sends a 200 success response with pagination metadata.
func OKWithMeta(c *gin.Context, data interface{}, meta *Meta) {
    c.JSON(http.StatusOK, SuccessResponse{Success: true, Data: data, Meta: meta})
}

// Created sends a 201 success response.
func Created(c *gin.Context, data interface{}) {
    c.JSON(http.StatusCreated, SuccessResponse{Success: true, Data: data})
}

// NoContent sends a 204 response with no body.
func NoContent(c *gin.Context) {
    c.Status(http.StatusNoContent)
}

// Error sends an error response, mapping AppError to the appropriate HTTP status.
func Error(c *gin.Context, err error) {
    if appErr, ok := err.(*apperr.AppError); ok {
        c.JSON(appErr.HTTPStatus, ErrorResponse{
            Success: false,
            Error:   ErrorBody{Code: appErr.Code, Message: appErr.Message},
        })
        return
    }
    // Fallback for unknown errors — never leak internal details.
    c.JSON(http.StatusInternalServerError, ErrorResponse{
        Success: false,
        Error:   ErrorBody{Code: apperr.CodeInternal, Message: "internal server error"},
    })
}
```

### 1.6 `pkg/validator/` — Phone & UUID Validation

```go
// pkg/validator/validator.go
package validator

import (
    "regexp"

    "github.com/google/uuid"
)

// e164Regex matches E.164 phone numbers: + followed by 1-15 digits.
var e164Regex = regexp.MustCompile(`^\+[1-9]\d{1,14}$`)

// IsValidPhone checks if the phone string matches E.164 format.
func IsValidPhone(phone string) bool {
    return e164Regex.MatchString(phone)
}

// IsValidUUID checks if s is a valid UUID v4.
func IsValidUUID(s string) bool {
    _, err := uuid.Parse(s)
    return err == nil
}

// ValidatePhone returns an error if the phone is not valid E.164.
func ValidatePhone(phone string) error {
    if !IsValidPhone(phone) {
        return &ValidationError{Field: "phone", Message: "must be valid E.164 format (e.g. +14155552671)"}
    }
    return nil
}

// ValidateUUID returns an error if s is not a valid UUID.
func ValidateUUID(s, fieldName string) error {
    if !IsValidUUID(s) {
        return &ValidationError{Field: fieldName, Message: "must be a valid UUID"}
    }
    return nil
}

// ValidationError represents a field validation failure.
type ValidationError struct {
    Field   string `json:"field"`
    Message string `json:"message"`
}

func (e *ValidationError) Error() string {
    return e.Field + ": " + e.Message
}
```

### 1.7 `pkg/config/` — Base Config Loader

```go
// pkg/config/config.go
package config

import (
    "fmt"
    "os"
    "reflect"
    "strconv"
    "time"
)

// Load populates a struct from environment variables using `env` struct tags.
// Supports string, int, bool, time.Duration, and []string (comma-separated).
// Use `env:"VAR_NAME"` and `envDefault:"value"` tags.
func Load(cfg interface{}) error {
    v := reflect.ValueOf(cfg)
    if v.Kind() != reflect.Ptr || v.Elem().Kind() != reflect.Struct {
        return fmt.Errorf("config.Load: expected pointer to struct")
    }
    return loadStruct(v.Elem())
}

func loadStruct(v reflect.Value) error {
    t := v.Type()
    for i := 0; i < t.NumField(); i++ {
        field := t.Field(i)
        fv := v.Field(i)

        if field.Type.Kind() == reflect.Struct && field.Anonymous {
            if err := loadStruct(fv); err != nil {
                return err
            }
            continue
        }

        envKey := field.Tag.Get("env")
        if envKey == "" {
            continue
        }

        val := os.Getenv(envKey)
        if val == "" {
            val = field.Tag.Get("envDefault")
        }
        if val == "" {
            if field.Tag.Get("envRequired") == "true" {
                return fmt.Errorf("config.Load: required env var %s not set", envKey)
            }
            continue
        }

        if err := setField(fv, val); err != nil {
            return fmt.Errorf("config.Load: field %s: %w", field.Name, err)
        }
    }
    return nil
}

func setField(fv reflect.Value, val string) error {
    switch fv.Kind() {
    case reflect.String:
        fv.SetString(val)
    case reflect.Int, reflect.Int64:
        if fv.Type() == reflect.TypeOf(time.Duration(0)) {
            d, err := time.ParseDuration(val)
            if err != nil {
                return err
            }
            fv.Set(reflect.ValueOf(d))
        } else {
            i, err := strconv.ParseInt(val, 10, 64)
            if err != nil {
                return err
            }
            fv.SetInt(i)
        }
    case reflect.Bool:
        b, err := strconv.ParseBool(val)
        if err != nil {
            return err
        }
        fv.SetBool(b)
    default:
        return fmt.Errorf("unsupported type %s", fv.Kind())
    }
    return nil
}
```

### 1.8 `pkg/health/` — Health Check Handler

```go
// pkg/health/health.go
package health

import (
    "context"
    "net/http"
    "sync"
    "time"

    "github.com/gin-gonic/gin"
)

// Checker is a function that verifies a dependency is healthy.
type Checker func(ctx context.Context) error

// Handler exposes /health (liveness) and /ready (readiness) endpoints.
type Handler struct {
    mu       sync.RWMutex
    checkers map[string]Checker
}

// NewHandler creates a Handler.
func NewHandler() *Handler {
    return &Handler{checkers: make(map[string]Checker)}
}

// AddChecker registers a named readiness checker (e.g. "postgres", "redis").
func (h *Handler) AddChecker(name string, fn Checker) {
    h.mu.Lock()
    defer h.mu.Unlock()
    h.checkers[name] = fn
}

// RegisterRoutes registers /health and /ready on the provided Gin engine.
func (h *Handler) RegisterRoutes(r *gin.Engine) {
    r.GET("/health", h.liveness)
    r.GET("/ready", h.readiness)
}

func (h *Handler) liveness(c *gin.Context) {
    c.JSON(http.StatusOK, gin.H{"status": "alive"})
}

func (h *Handler) readiness(c *gin.Context) {
    h.mu.RLock()
    defer h.mu.RUnlock()

    ctx, cancel := context.WithTimeout(c.Request.Context(), 5*time.Second)
    defer cancel()

    results := make(map[string]string, len(h.checkers))
    allOK := true
    for name, check := range h.checkers {
        if err := check(ctx); err != nil {
            results[name] = err.Error()
            allOK = false
        } else {
            results[name] = "ok"
        }
    }

    status := http.StatusOK
    if !allOK {
        status = http.StatusServiceUnavailable
    }
    c.JSON(status, gin.H{"status": results})
}
```

### 1.9 `pkg/grpcclient/` — gRPC Client Factory with Circuit Breaker & Retry

```go
// pkg/grpcclient/factory.go
package grpcclient

import (
    "context"
    "fmt"
    "sync"
    "time"

    "github.com/sony/gobreaker/v2"
    "google.golang.org/grpc"
    "google.golang.org/grpc/codes"
    "google.golang.org/grpc/credentials/insecure"
    "google.golang.org/grpc/status"
)

// Config holds connection settings for a gRPC target.
type Config struct {
    Address       string        // e.g. "auth-service:9081"
    Timeout       time.Duration // per-call deadline, default 5s
    MaxRetries    int           // default 3
    RetryBackoff  time.Duration // base backoff between retries, default 100ms
    CBMaxRequests uint32        // gobreaker half-open max requests, default 5
    CBInterval    time.Duration // gobreaker closed-state interval, default 60s
    CBTimeout     time.Duration // gobreaker open-state timeout, default 30s
}

// Factory manages gRPC client connections with pooling and circuit breakers.
type Factory struct {
    mu    sync.RWMutex
    conns map[string]*grpc.ClientConn
    cbs   map[string]*gobreaker.CircuitBreaker[any]
}

// NewFactory creates a new gRPC client factory.
func NewFactory() *Factory {
    return &Factory{
        conns: make(map[string]*grpc.ClientConn),
        cbs:   make(map[string]*gobreaker.CircuitBreaker[any]),
    }
}

// GetConnection returns a shared *grpc.ClientConn for the target, creating one if needed.
func (f *Factory) GetConnection(cfg Config) (*grpc.ClientConn, error) {
    f.mu.RLock()
    if conn, ok := f.conns[cfg.Address]; ok {
        f.mu.RUnlock()
        return conn, nil
    }
    f.mu.RUnlock()

    f.mu.Lock()
    defer f.mu.Unlock()

    // Double-check after acquiring write lock.
    if conn, ok := f.conns[cfg.Address]; ok {
        return conn, nil
    }

    conn, err := grpc.NewClient(
        cfg.Address,
        grpc.WithTransportCredentials(insecure.NewCredentials()),
        grpc.WithUnaryInterceptor(f.retryInterceptor(cfg)),
    )
    if err != nil {
        return nil, fmt.Errorf("grpcclient: dial %s: %w", cfg.Address, err)
    }

    f.conns[cfg.Address] = conn
    f.cbs[cfg.Address] = gobreaker.NewCircuitBreaker[any](gobreaker.Settings{
        Name:        cfg.Address,
        MaxRequests: cfg.CBMaxRequests,
        Interval:    cfg.CBInterval,
        Timeout:     cfg.CBTimeout,
        ReadyToTrip: func(counts gobreaker.Counts) bool {
            return counts.ConsecutiveFailures > 5
        },
    })

    return conn, nil
}

// retryInterceptor wraps calls with retry + circuit breaker logic.
func (f *Factory) retryInterceptor(cfg Config) grpc.UnaryClientInterceptor {
    return func(
        ctx context.Context,
        method string,
        req, reply interface{},
        cc *grpc.ClientConn,
        invoker grpc.UnaryInvoker,
        opts ...grpc.CallOption,
    ) error {
        cb := f.cbs[cfg.Address]
        timeout := cfg.Timeout
        if timeout == 0 {
            timeout = 5 * time.Second
        }
        maxRetries := cfg.MaxRetries
        if maxRetries == 0 {
            maxRetries = 3
        }
        backoff := cfg.RetryBackoff
        if backoff == 0 {
            backoff = 100 * time.Millisecond
        }

        var lastErr error
        for attempt := 0; attempt <= maxRetries; attempt++ {
            _, cbErr := cb.Execute(func() (any, error) {
                callCtx, cancel := context.WithTimeout(ctx, timeout)
                defer cancel()
                err := invoker(callCtx, method, req, reply, cc, opts...)
                if err != nil {
                    return nil, err
                }
                return nil, nil
            })
            if cbErr == nil {
                return nil
            }
            lastErr = cbErr

            // Only retry on transient errors.
            st, ok := status.FromError(cbErr)
            if !ok || !isRetryable(st.Code()) {
                return cbErr
            }

            time.Sleep(backoff * time.Duration(1<<attempt))
        }
        return lastErr
    }
}

func isRetryable(code codes.Code) bool {
    switch code {
    case codes.Unavailable, codes.DeadlineExceeded, codes.ResourceExhausted:
        return true
    default:
        return false
    }
}

// Close closes all managed connections.
func (f *Factory) Close() {
    f.mu.Lock()
    defer f.mu.Unlock()
    for addr, conn := range f.conns {
        conn.Close()
        delete(f.conns, addr)
    }
}
```

---

## 2. Proto Definitions (`backend/proto/`)

### 2.1 `proto/auth/v1/auth.proto`

```protobuf
syntax = "proto3";

package auth.v1;

option go_package = "github.com/whatsapp-clone/backend/proto/auth/v1;authv1";

service AuthService {
  // ValidateToken validates a JWT access token and returns the associated user identity.
  rpc ValidateToken(ValidateTokenRequest) returns (ValidateTokenResponse);
}

message ValidateTokenRequest {
  string token = 1;
}

message ValidateTokenResponse {
  bool   valid   = 1;
  string user_id = 2;
  string phone   = 3;
}
```

### 2.2 `proto/user/v1/user.proto`

```protobuf
syntax = "proto3";

package user.v1;

option go_package = "github.com/whatsapp-clone/backend/proto/user/v1;userv1";

import "google/protobuf/timestamp.proto";

service UserService {
  // GetUser retrieves a single user profile by ID.
  rpc GetUser(GetUserRequest) returns (GetUserResponse);

  // GetUsers retrieves multiple user profiles by IDs (batch).
  rpc GetUsers(GetUsersRequest) returns (GetUsersResponse);

  // CheckPresence checks if a user is currently online.
  rpc CheckPresence(CheckPresenceRequest) returns (CheckPresenceResponse);
}

message GetUserRequest {
  string user_id = 1;
}

message GetUserResponse {
  UserProfile user = 1;
}

message GetUsersRequest {
  repeated string user_ids = 1;
}

message GetUsersResponse {
  repeated UserProfile users = 1;
}

message UserProfile {
  string user_id      = 1;
  string phone        = 2;
  string display_name = 3;
  string avatar_url   = 4;
  string status_text  = 5;
  google.protobuf.Timestamp created_at = 6;
  google.protobuf.Timestamp updated_at = 7;
}

message CheckPresenceRequest {
  string user_id = 1;
}

message CheckPresenceResponse {
  bool   online    = 1;
  google.protobuf.Timestamp last_seen = 2;
}
```

### 2.3 `proto/message/v1/message.proto`

```protobuf
syntax = "proto3";

package message.v1;

option go_package = "github.com/whatsapp-clone/backend/proto/message/v1;messagev1";

import "google/protobuf/timestamp.proto";

service MessageService {
  // SendMessage persists a new message and publishes the msg.new event.
  rpc SendMessage(SendMessageRequest) returns (SendMessageResponse);

  // UpdateMessageStatus updates the delivery/read status for a specific recipient.
  rpc UpdateMessageStatus(UpdateMessageStatusRequest) returns (UpdateMessageStatusResponse);

  // GetLastMessages retrieves the latest message per chat for chat-list previews.
  rpc GetLastMessages(GetLastMessagesRequest) returns (GetLastMessagesResponse);

  // GetUnreadCounts returns the unread message count per chat for a user.
  rpc GetUnreadCounts(GetUnreadCountsRequest) returns (GetUnreadCountsResponse);
}

message SendMessageRequest {
  string chat_id       = 1;
  string sender_id     = 2;
  string type          = 3; // "text", "image", "video", "audio", "document", "location"
  MessagePayload payload = 4;
  string client_msg_id = 5; // client-generated idempotency key
  string reply_to_message_id = 6;
  ForwardedFrom forwarded_from = 7;
}

message MessagePayload {
  string body        = 1;
  string media_id    = 2;
  string caption     = 3;
  string filename    = 4;
  int64  duration_ms = 5;
}

message ForwardedFrom {
  string chat_id    = 1;
  string message_id = 2;
}

message SendMessageResponse {
  string message_id = 1;
  google.protobuf.Timestamp created_at = 2;
}

message UpdateMessageStatusRequest {
  string message_id = 1;
  string user_id    = 2;
  string status     = 3; // "delivered", "read"
}

message UpdateMessageStatusResponse {
  bool success = 1;
}

message GetLastMessagesRequest {
  repeated string chat_ids = 1;
}

message GetLastMessagesResponse {
  map<string, MessagePreview> messages = 1; // keyed by chat_id
}

message MessagePreview {
  string message_id = 1;
  string sender_id  = 2;
  string type       = 3;
  string body       = 4; // first 100 chars of payload.body or "[Image]", "[Video]", etc.
  google.protobuf.Timestamp created_at = 5;
}

message GetUnreadCountsRequest {
  string user_id            = 1;
  repeated string chat_ids  = 2;
}

message GetUnreadCountsResponse {
  map<string, int64> counts = 1; // keyed by chat_id
}
```

### 2.4 `proto/media/v1/media.proto`

```protobuf
syntax = "proto3";

package media.v1;

option go_package = "github.com/whatsapp-clone/backend/proto/media/v1;mediav1";

service MediaService {
  // GetMediaMetadata returns file metadata and a presigned download URL.
  rpc GetMediaMetadata(GetMediaMetadataRequest) returns (GetMediaMetadataResponse);
}

message GetMediaMetadataRequest {
  string media_id = 1;
}

message GetMediaMetadataResponse {
  string media_id       = 1;
  string file_type      = 2; // "image", "video", "audio", "document"
  string mime_type      = 3;
  int64  size_bytes     = 4;
  string url            = 5; // presigned download URL
  string thumbnail_url  = 6; // presigned thumbnail URL (empty if N/A)
  int32  width          = 7;
  int32  height         = 8;
  int64  duration_ms    = 9;
}
```

---

## 3. Per-Service Detailed Design

---

### 3.1 api-gateway

**Port:** `:8080`

#### a) Configuration Struct

```go
// api-gateway/config/config.go
package config

import "time"

type Config struct {
    Port             string        `env:"GATEWAY_PORT"              envDefault:":8080"`
    AuthGRPCAddr     string        `env:"AUTH_GRPC_ADDR"            envDefault:"auth-service:9081"`
    AuthHTTPAddr     string        `env:"AUTH_HTTP_ADDR"            envDefault:"http://auth-service:8081"`
    UserHTTPAddr     string        `env:"USER_HTTP_ADDR"            envDefault:"http://user-service:8082"`
    ChatHTTPAddr     string        `env:"CHAT_HTTP_ADDR"            envDefault:"http://chat-service:8083"`
    MessageHTTPAddr  string        `env:"MESSAGE_HTTP_ADDR"         envDefault:"http://message-service:8084"`
    MediaHTTPAddr    string        `env:"MEDIA_HTTP_ADDR"           envDefault:"http://media-service:8086"`
    WSAddr           string        `env:"WS_ADDR"                   envDefault:"http://ws-service:8087"`
    RedisAddr        string        `env:"GATEWAY_REDIS_ADDR"        envDefault:"redis:6379"`
    RedisPassword    string        `env:"GATEWAY_REDIS_PASSWORD"    envDefault:""`
    RateLimitRPS     int           `env:"GATEWAY_RATE_LIMIT_RPS"    envDefault:"60"`
    RateLimitBurst   int           `env:"GATEWAY_RATE_LIMIT_BURST"  envDefault:"10"`
    CORSOrigins      string        `env:"GATEWAY_CORS_ORIGINS"      envDefault:"*"`
    LogLevel         string        `env:"GATEWAY_LOG_LEVEL"         envDefault:"info"`
    GRPCTimeout      time.Duration `env:"GATEWAY_GRPC_TIMEOUT"      envDefault:"5s"`
}
```

#### b) Domain Models

The api-gateway is a pass-through proxy and does not own domain models. It works with `pkg/jwt.Claims` and route definitions.

```go
// api-gateway/internal/model/route.go
package model

// RouteTarget maps a URL prefix to a backend service base URL.
type RouteTarget struct {
    PathPrefix string // e.g. "/api/v1/auth"
    TargetURL  string // e.g. "http://auth-service:8081"
    StripPrefix bool  // whether to strip the prefix before proxying
    RequireAuth bool  // whether the route requires JWT authentication
}
```

#### c) Service Interface

```go
// api-gateway/internal/service/auth.go
package service

import "context"

// AuthValidator validates JWT tokens by calling the auth-service gRPC endpoint.
type AuthValidator interface {
    // ValidateToken returns user_id and phone if the token is valid, or an error.
    ValidateToken(ctx context.Context, token string) (userID string, phone string, err error)
}
```

```go
// api-gateway/internal/service/ratelimiter.go
package service

import "context"

// RateLimiter checks and enforces per-IP rate limits.
type RateLimiter interface {
    // Allow returns true if the request is within rate limits.
    // key is typically "ip:endpoint", limit is requests per window.
    Allow(ctx context.Context, key string, limit int, windowSec int) (bool, error)
}
```

#### d) AuthValidator Implementation (gRPC)

```go
// api-gateway/internal/service/auth_grpc.go
package service

import (
    "context"
    "fmt"

    authv1 "github.com/whatsapp-clone/backend/proto/auth/v1"
    "google.golang.org/grpc"
)

type authGRPCValidator struct {
    client authv1.AuthServiceClient
}

func NewAuthValidator(conn *grpc.ClientConn) AuthValidator {
    return &authGRPCValidator{client: authv1.NewAuthServiceClient(conn)}
}

func (v *authGRPCValidator) ValidateToken(ctx context.Context, token string) (string, string, error) {
    resp, err := v.client.ValidateToken(ctx, &authv1.ValidateTokenRequest{Token: token})
    if err != nil {
        return "", "", fmt.Errorf("auth grpc: %w", err)
    }
    if !resp.Valid {
        return "", "", fmt.Errorf("token invalid")
    }
    return resp.UserId, resp.Phone, nil
}
```

#### e) RateLimiter Implementation (Redis Token Bucket)

```go
// api-gateway/internal/service/ratelimiter_redis.go
package service

import (
    "context"
    "time"

    "github.com/redis/go-redis/v9"
)

type redisRateLimiter struct {
    rdb *redis.Client
}

func NewRedisRateLimiter(rdb *redis.Client) RateLimiter {
    return &redisRateLimiter{rdb: rdb}
}

// Allow implements a sliding-window counter using Redis INCR + EXPIRE.
// Returns true if the current count is within the limit.
func (r *redisRateLimiter) Allow(ctx context.Context, key string, limit int, windowSec int) (bool, error) {
    pipe := r.rdb.Pipeline()
    incrCmd := pipe.Incr(ctx, key)
    pipe.Expire(ctx, key, time.Duration(windowSec)*time.Second)
    _, err := pipe.Exec(ctx)
    if err != nil {
        return false, err
    }
    count := incrCmd.Val()
    return count <= int64(limit), nil
}
```

#### f) Handler Layer — Auth Middleware

```go
// api-gateway/internal/handler/auth_middleware.go
package handler

import (
    "net/http"
    "strings"

    "github.com/gin-gonic/gin"
    "github.com/whatsapp-clone/backend/api-gateway/internal/service"
    "github.com/whatsapp-clone/backend/pkg/response"
    apperr "github.com/whatsapp-clone/backend/pkg/errors"
)

// AuthMiddleware extracts the Bearer token, validates it via gRPC, and injects
// user_id and phone into the Gin context.
func AuthMiddleware(validator service.AuthValidator) gin.HandlerFunc {
    return func(c *gin.Context) {
        auth := c.GetHeader("Authorization")
        if auth == "" || !strings.HasPrefix(auth, "Bearer ") {
            response.Error(c, apperr.NewUnauthorized("missing or invalid Authorization header"))
            c.Abort()
            return
        }
        token := strings.TrimPrefix(auth, "Bearer ")

        userID, phone, err := validator.ValidateToken(c.Request.Context(), token)
        if err != nil {
            response.Error(c, apperr.NewUnauthorized("invalid token"))
            c.Abort()
            return
        }

        c.Set("user_id", userID)
        c.Set("phone", phone)
        c.Next()
    }
}
```

#### g) Handler Layer — Rate Limit Middleware

```go
// api-gateway/internal/handler/ratelimit_middleware.go
package handler

import (
    "github.com/gin-gonic/gin"
    "github.com/whatsapp-clone/backend/api-gateway/internal/service"
    "github.com/whatsapp-clone/backend/pkg/response"
    apperr "github.com/whatsapp-clone/backend/pkg/errors"
)

func RateLimitMiddleware(limiter service.RateLimiter, rps int) gin.HandlerFunc {
    return func(c *gin.Context) {
        key := "rate:" + c.ClientIP() + ":" + c.FullPath()
        allowed, err := limiter.Allow(c.Request.Context(), key, rps, 60)
        if err != nil {
            // Fail open on Redis errors to avoid blocking traffic.
            c.Next()
            return
        }
        if !allowed {
            response.Error(c, apperr.NewTooManyRequests("rate limit exceeded"))
            c.Abort()
            return
        }
        c.Next()
    }
}
```

#### h) Handler Layer — Reverse Proxy & WebSocket Upgrade

```go
// api-gateway/internal/handler/proxy.go
package handler

import (
    "net/http"
    "net/http/httputil"
    "net/url"
    "strings"

    "github.com/gin-gonic/gin"
    "github.com/whatsapp-clone/backend/api-gateway/internal/model"
)

// RegisterProxyRoutes sets up the reverse-proxy routes for each backend service.
func RegisterProxyRoutes(r *gin.Engine, routes []model.RouteTarget) {
    for _, rt := range routes {
        target, _ := url.Parse(rt.TargetURL)
        proxy := httputil.NewSingleHostReverseProxy(target)

        stripPrefix := rt.PathPrefix
        if rt.StripPrefix {
            originalDirector := proxy.Director
            proxy.Director = func(req *http.Request) {
                originalDirector(req)
                req.URL.Path = strings.TrimPrefix(req.URL.Path, stripPrefix)
                if req.URL.Path == "" {
                    req.URL.Path = "/"
                }
            }
        }

        r.Any(rt.PathPrefix+"/*path", func(c *gin.Context) {
            // Forward user identity headers to downstream services.
            if uid, exists := c.Get("user_id"); exists {
                c.Request.Header.Set("X-User-ID", uid.(string))
            }
            if phone, exists := c.Get("phone"); exists {
                c.Request.Header.Set("X-User-Phone", phone.(string))
            }
            proxy.ServeHTTP(c.Writer, c.Request)
        })
    }
}
```

```go
// api-gateway/internal/handler/ws_proxy.go
package handler

import (
    "io"
    "net/http"
    "net/url"

    "github.com/gin-gonic/gin"
    "github.com/gorilla/websocket"
)

var wsUpgrader = websocket.Upgrader{
    CheckOrigin: func(r *http.Request) bool { return true },
}

// WSProxyHandler upgrades the client connection and pipes traffic to the ws-service.
func WSProxyHandler(wsTargetURL string) gin.HandlerFunc {
    return func(c *gin.Context) {
        // Upgrade client -> gateway connection.
        clientConn, err := wsUpgrader.Upgrade(c.Writer, c.Request, nil)
        if err != nil {
            return
        }
        defer clientConn.Close()

        // Dial gateway -> ws-service connection, forwarding auth headers.
        target, _ := url.Parse(wsTargetURL)
        header := http.Header{}
        header.Set("Authorization", c.GetHeader("Authorization"))
        if uid, exists := c.Get("user_id"); exists {
            header.Set("X-User-ID", uid.(string))
        }

        backendConn, _, err := websocket.DefaultDialer.Dial(target.String(), header)
        if err != nil {
            clientConn.WriteMessage(websocket.CloseMessage,
                websocket.FormatCloseMessage(websocket.CloseInternalServerErr, "backend unavailable"))
            return
        }
        defer backendConn.Close()

        // Bidirectional pipe.
        errCh := make(chan error, 2)
        go pumpWS(clientConn, backendConn, errCh)
        go pumpWS(backendConn, clientConn, errCh)
        <-errCh
    }
}

func pumpWS(src, dst *websocket.Conn, errCh chan<- error) {
    for {
        msgType, reader, err := src.NextReader()
        if err != nil {
            errCh <- err
            return
        }
        writer, err := dst.NextWriter(msgType)
        if err != nil {
            errCh <- err
            return
        }
        if _, err := io.Copy(writer, reader); err != nil {
            errCh <- err
            return
        }
        writer.Close()
    }
}
```

#### i) Initialization / Bootstrap (`main.go`)

```go
// api-gateway/cmd/main.go
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
    svc "github.com/whatsapp-clone/backend/api-gateway/internal/service"
    pkgcfg "github.com/whatsapp-clone/backend/pkg/config"
    "github.com/whatsapp-clone/backend/pkg/grpcclient"
    "github.com/whatsapp-clone/backend/pkg/health"
    "github.com/whatsapp-clone/backend/pkg/logger"
    "github.com/whatsapp-clone/backend/pkg/middleware"
)

func main() {
    // 1. Load config.
    var cfg config.Config
    if err := pkgcfg.Load(&cfg); err != nil {
        panic(err)
    }

    // 2. Logger.
    log := logger.New("api-gateway", cfg.LogLevel)

    // 3. Redis client.
    rdb := redis.NewClient(&redis.Options{
        Addr:     cfg.RedisAddr,
        Password: cfg.RedisPassword,
    })
    defer rdb.Close()

    // 4. gRPC connection to auth-service.
    grpcFactory := grpcclient.NewFactory()
    defer grpcFactory.Close()
    authConn, err := grpcFactory.GetConnection(grpcclient.Config{
        Address: cfg.AuthGRPCAddr,
        Timeout: cfg.GRPCTimeout,
    })
    if err != nil {
        log.Fatal().Err(err).Msg("failed to connect to auth-service gRPC")
    }

    // 5. Services.
    authValidator := svc.NewAuthValidator(authConn)
    rateLimiter := svc.NewRedisRateLimiter(rdb)

    // 6. Gin engine.
    r := gin.New()
    origins := strings.Split(cfg.CORSOrigins, ",")

    // 7. Middleware chain (order matters).
    r.Use(
        middleware.Recovery(log),
        middleware.RequestID(),
        middleware.Logger(log),
        middleware.CORS(origins),
    )

    // 8. Health checks.
    hc := health.NewHandler()
    hc.AddChecker("redis", func(ctx context.Context) error {
        return rdb.Ping(ctx).Err()
    })
    hc.RegisterRoutes(r)

    // 9. Public routes (no auth required).
    publicRoutes := []model.RouteTarget{
        {PathPrefix: "/api/v1/auth", TargetURL: cfg.AuthHTTPAddr, RequireAuth: false},
    }
    handler.RegisterProxyRoutes(r, publicRoutes)

    // 10. Protected routes (auth + rate limit required).
    protected := r.Group("/")
    protected.Use(
        handler.RateLimitMiddleware(rateLimiter, cfg.RateLimitRPS),
        handler.AuthMiddleware(authValidator),
    )

    protectedRoutes := []model.RouteTarget{
        {PathPrefix: "/api/v1/users", TargetURL: cfg.UserHTTPAddr, RequireAuth: true},
        {PathPrefix: "/api/v1/chats", TargetURL: cfg.ChatHTTPAddr, RequireAuth: true},
        {PathPrefix: "/api/v1/messages", TargetURL: cfg.MessageHTTPAddr, RequireAuth: true},
        {PathPrefix: "/api/v1/media", TargetURL: cfg.MediaHTTPAddr, RequireAuth: true},
    }
    for _, rt := range protectedRoutes {
        handler.RegisterProxyRoutes(r, []model.RouteTarget{rt})
    }

    // 11. WebSocket route.
    protected.GET("/ws", handler.WSProxyHandler(cfg.WSAddr))

    // 12. Start server with graceful shutdown.
    srv := &http.Server{Addr: cfg.Port, Handler: r}
    go func() {
        if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            log.Fatal().Err(err).Msg("server failed")
        }
    }()

    quit := make(chan os.Signal, 1)
    signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
    <-quit

    log.Info().Msg("shutting down api-gateway")
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()
    if err := srv.Shutdown(ctx); err != nil {
        log.Error().Err(err).Msg("server forced shutdown")
    }
}
```

---

### 3.2 auth-service

**Ports:** `:8081` HTTP, `:9081` gRPC

#### a) Configuration Struct

```go
// auth-service/config/config.go
package config

import "time"

type Config struct {
    HTTPPort         string        `env:"AUTH_HTTP_PORT"          envDefault:":8081"`
    GRPCPort         string        `env:"AUTH_GRPC_PORT"          envDefault:":9081"`
    PostgresDSN      string        `env:"AUTH_POSTGRES_DSN"       envRequired:"true"`
    RedisAddr        string        `env:"AUTH_REDIS_ADDR"         envDefault:"redis:6379"`
    RedisPassword    string        `env:"AUTH_REDIS_PASSWORD"     envDefault:""`
    JWTSecret        string        `env:"AUTH_JWT_SECRET"         envRequired:"true"`
    AccessTokenTTL   time.Duration `env:"AUTH_ACCESS_TOKEN_TTL"   envDefault:"15m"`
    RefreshTokenTTL  time.Duration `env:"AUTH_REFRESH_TOKEN_TTL"  envDefault:"720h"` // 30 days
    OTPLength        int           `env:"AUTH_OTP_LENGTH"         envDefault:"6"`
    OTPTTL           time.Duration `env:"AUTH_OTP_TTL"            envDefault:"5m"`
    OTPMaxAttempts   int           `env:"AUTH_OTP_MAX_ATTEMPTS"   envDefault:"5"`
    LogLevel         string        `env:"AUTH_LOG_LEVEL"          envDefault:"info"`
}
```

#### b) Domain Models

```go
// auth-service/internal/model/user.go
package model

import "time"

type User struct {
    ID          string    `json:"id"           db:"id"`
    Phone       string    `json:"phone"        db:"phone"`
    DisplayName string    `json:"display_name" db:"display_name"`
    AvatarURL   string    `json:"avatar_url"   db:"avatar_url"`
    StatusText  string    `json:"status_text"  db:"status_text"`
    CreatedAt   time.Time `json:"created_at"   db:"created_at"`
    UpdatedAt   time.Time `json:"updated_at"   db:"updated_at"`
}
```

```go
// auth-service/internal/model/refresh_token.go
package model

import "time"

type RefreshToken struct {
    ID        string    `db:"id"`
    UserID    string    `db:"user_id"`
    TokenHash string    `db:"token_hash"` // SHA-256 hash of the opaque token
    ExpiresAt time.Time `db:"expires_at"`
    Revoked   bool      `db:"revoked"`
    CreatedAt time.Time `db:"created_at"`
}
```

```go
// auth-service/internal/model/otp.go
package model

// OTPEntry represents a stored OTP in Redis.
type OTPEntry struct {
    HashedOTP string `json:"hashed_otp"` // bcrypt hash
    Attempts  int    `json:"attempts"`
}
```

```go
// auth-service/internal/model/auth.go
package model

// TokenPair represents the access + refresh tokens returned on login.
type TokenPair struct {
    AccessToken  string `json:"access_token"`
    RefreshToken string `json:"refresh_token"`
    ExpiresIn    int64  `json:"expires_in"` // seconds until access token expires
}

// SendOTPRequest is the inbound HTTP request for OTP generation.
type SendOTPRequest struct {
    Phone string `json:"phone" binding:"required"`
}

// VerifyOTPRequest is the inbound HTTP request for OTP verification.
type VerifyOTPRequest struct {
    Phone string `json:"phone" binding:"required"`
    Code  string `json:"code"  binding:"required"`
}

// RefreshRequest is the inbound HTTP request for token refresh.
type RefreshRequest struct {
    RefreshToken string `json:"refresh_token" binding:"required"`
}
```

#### c) Repository Interfaces

```go
// auth-service/internal/repository/user_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/auth-service/internal/model"
)

// UserRepository handles user persistence for the auth-service.
type UserRepository interface {
    // UpsertByPhone creates or returns the existing user for the given phone.
    // On conflict (phone already exists), returns the existing user without modification.
    UpsertByPhone(ctx context.Context, phone string) (*model.User, error)

    // GetByID retrieves a user by their UUID.
    GetByID(ctx context.Context, id string) (*model.User, error)
}
```

```go
// auth-service/internal/repository/refresh_token_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/auth-service/internal/model"
)

// RefreshTokenRepository handles refresh token persistence.
type RefreshTokenRepository interface {
    // Create inserts a new refresh token record.
    Create(ctx context.Context, token *model.RefreshToken) error

    // GetByTokenHash finds a non-revoked, non-expired token by its SHA-256 hash.
    GetByTokenHash(ctx context.Context, tokenHash string) (*model.RefreshToken, error)

    // RevokeByID marks a specific refresh token as revoked.
    RevokeByID(ctx context.Context, id string) error

    // RevokeAllByUserID revokes all refresh tokens for a user (used on logout-all).
    RevokeAllByUserID(ctx context.Context, userID string) error
}
```

```go
// auth-service/internal/repository/otp_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/auth-service/internal/model"
)

// OTPRepository handles OTP storage in Redis.
type OTPRepository interface {
    // Store saves the hashed OTP for the phone with the configured TTL.
    Store(ctx context.Context, phone string, entry *model.OTPEntry) error

    // Get retrieves the OTP entry for the phone. Returns nil if expired/not found.
    Get(ctx context.Context, phone string) (*model.OTPEntry, error)

    // IncrementAttempts atomically increments the attempt counter.
    IncrementAttempts(ctx context.Context, phone string) (int, error)

    // Delete removes the OTP entry (after successful verification).
    Delete(ctx context.Context, phone string) error
}
```

#### d) Service Interface

```go
// auth-service/internal/service/auth_service.go
package service

import (
    "context"

    "github.com/whatsapp-clone/backend/auth-service/internal/model"
)

// AuthService encapsulates authentication business logic.
type AuthService interface {
    // SendOTP generates a 6-digit OTP, bcrypt-hashes it, stores in Redis, and
    // returns the plaintext OTP (in production, this would be sent via SMS provider).
    SendOTP(ctx context.Context, phone string) (otp string, err error)

    // VerifyOTP validates the OTP against Redis, upserts the user, and issues tokens.
    VerifyOTP(ctx context.Context, phone, code string) (*model.TokenPair, error)

    // RefreshTokens validates the refresh token, rotates it (revoke old, issue new),
    // and returns a fresh token pair.
    RefreshTokens(ctx context.Context, refreshToken string) (*model.TokenPair, error)

    // Logout revokes the specific refresh token.
    Logout(ctx context.Context, refreshToken string) error

    // ValidateToken validates a JWT access token and returns claims.
    ValidateToken(ctx context.Context, token string) (userID string, phone string, err error)
}
```

#### e) Key Business Logic — AuthService Implementation

```go
// auth-service/internal/service/auth_service_impl.go
package service

import (
    "context"
    "crypto/rand"
    "crypto/sha256"
    "encoding/hex"
    "fmt"
    "math/big"
    "time"

    "golang.org/x/crypto/bcrypt"

    "github.com/whatsapp-clone/backend/auth-service/internal/model"
    "github.com/whatsapp-clone/backend/auth-service/internal/repository"
    apperr "github.com/whatsapp-clone/backend/pkg/errors"
    "github.com/whatsapp-clone/backend/pkg/jwt"
)

type authServiceImpl struct {
    userRepo    repository.UserRepository
    tokenRepo   repository.RefreshTokenRepository
    otpRepo     repository.OTPRepository
    jwtManager  *jwt.Manager
    otpLength   int
    maxAttempts int
    refreshTTL  time.Duration
    accessTTL   time.Duration
}

func NewAuthService(
    userRepo repository.UserRepository,
    tokenRepo repository.RefreshTokenRepository,
    otpRepo repository.OTPRepository,
    jwtManager *jwt.Manager,
    otpLength, maxAttempts int,
    accessTTL, refreshTTL time.Duration,
) AuthService {
    return &authServiceImpl{
        userRepo:    userRepo,
        tokenRepo:   tokenRepo,
        otpRepo:     otpRepo,
        jwtManager:  jwtManager,
        otpLength:   otpLength,
        maxAttempts: maxAttempts,
        refreshTTL:  refreshTTL,
        accessTTL:   accessTTL,
    }
}

// SendOTP generates a cryptographically random N-digit OTP, bcrypt-hashes it,
// and stores the hash in Redis keyed by "otp:<phone>" with a 5-minute TTL.
func (s *authServiceImpl) SendOTP(ctx context.Context, phone string) (string, error) {
    // Generate random OTP: e.g. for 6 digits, pick random number in [0, 999999].
    max := new(big.Int).Exp(big.NewInt(10), big.NewInt(int64(s.otpLength)), nil)
    n, err := rand.Int(rand.Reader, max)
    if err != nil {
        return "", apperr.NewInternal("failed to generate OTP", err)
    }
    otp := fmt.Sprintf("%0*d", s.otpLength, n.Int64())

    // Bcrypt-hash the OTP before storage.
    hashed, err := bcrypt.GenerateFromPassword([]byte(otp), bcrypt.DefaultCost)
    if err != nil {
        return "", apperr.NewInternal("failed to hash OTP", err)
    }

    entry := &model.OTPEntry{
        HashedOTP: string(hashed),
        Attempts:  0,
    }
    if err := s.otpRepo.Store(ctx, phone, entry); err != nil {
        return "", apperr.NewInternal("failed to store OTP", err)
    }

    return otp, nil
}

// VerifyOTP checks the code against the stored bcrypt hash, enforces max attempts,
// upserts the user, and returns a fresh token pair.
func (s *authServiceImpl) VerifyOTP(ctx context.Context, phone, code string) (*model.TokenPair, error) {
    entry, err := s.otpRepo.Get(ctx, phone)
    if err != nil {
        return nil, apperr.NewInternal("failed to retrieve OTP", err)
    }
    if entry == nil {
        return nil, &apperr.AppError{Code: apperr.CodeOTPExpired, Message: "OTP expired or not found", HTTPStatus: 400}
    }

    // Check max attempts.
    if entry.Attempts >= s.maxAttempts {
        _ = s.otpRepo.Delete(ctx, phone)
        return nil, &apperr.AppError{Code: apperr.CodeOTPMaxAttempts, Message: "too many OTP attempts", HTTPStatus: 429}
    }

    // Increment attempt counter.
    attempts, err := s.otpRepo.IncrementAttempts(ctx, phone)
    if err != nil {
        return nil, apperr.NewInternal("failed to increment attempts", err)
    }
    _ = attempts

    // Verify bcrypt.
    if err := bcrypt.CompareHashAndPassword([]byte(entry.HashedOTP), []byte(code)); err != nil {
        return nil, &apperr.AppError{Code: apperr.CodeOTPInvalid, Message: "invalid OTP", HTTPStatus: 400}
    }

    // OTP valid — delete it.
    _ = s.otpRepo.Delete(ctx, phone)

    // Upsert user by phone.
    user, err := s.userRepo.UpsertByPhone(ctx, phone)
    if err != nil {
        return nil, apperr.NewInternal("failed to upsert user", err)
    }

    // Generate token pair.
    return s.issueTokenPair(ctx, user.ID, user.Phone)
}

// RefreshTokens validates the provided opaque refresh token, revokes it,
// and issues a new token pair (rotation).
func (s *authServiceImpl) RefreshTokens(ctx context.Context, rawToken string) (*model.TokenPair, error) {
    hash := sha256Hash(rawToken)

    stored, err := s.tokenRepo.GetByTokenHash(ctx, hash)
    if err != nil {
        return nil, apperr.NewInternal("failed to lookup refresh token", err)
    }
    if stored == nil {
        return nil, apperr.NewUnauthorized("invalid refresh token")
    }
    if stored.Revoked || time.Now().After(stored.ExpiresAt) {
        return nil, apperr.NewUnauthorized("refresh token expired or revoked")
    }

    // Revoke old token.
    if err := s.tokenRepo.RevokeByID(ctx, stored.ID); err != nil {
        return nil, apperr.NewInternal("failed to revoke old refresh token", err)
    }

    // Look up user to get phone (for JWT claims).
    user, err := s.userRepo.GetByID(ctx, stored.UserID)
    if err != nil {
        return nil, apperr.NewInternal("failed to get user", err)
    }

    return s.issueTokenPair(ctx, user.ID, user.Phone)
}

// Logout revokes the given refresh token.
func (s *authServiceImpl) Logout(ctx context.Context, rawToken string) error {
    hash := sha256Hash(rawToken)
    stored, err := s.tokenRepo.GetByTokenHash(ctx, hash)
    if err != nil || stored == nil {
        return nil // Idempotent: if token not found, treat as already logged out.
    }
    return s.tokenRepo.RevokeByID(ctx, stored.ID)
}

// ValidateToken delegates to the JWT manager.
func (s *authServiceImpl) ValidateToken(ctx context.Context, token string) (string, string, error) {
    claims, err := s.jwtManager.ValidateToken(token)
    if err != nil {
        return "", "", err
    }
    return claims.UserID, claims.Phone, nil
}

// issueTokenPair creates both an access JWT and an opaque refresh token.
func (s *authServiceImpl) issueTokenPair(ctx context.Context, userID, phone string) (*model.TokenPair, error) {
    accessToken, err := s.jwtManager.CreateAccessToken(userID, phone)
    if err != nil {
        return nil, apperr.NewInternal("failed to create access token", err)
    }

    // Generate opaque refresh token: 32 random bytes -> hex string.
    rawBytes := make([]byte, 32)
    if _, err := rand.Read(rawBytes); err != nil {
        return nil, apperr.NewInternal("failed to generate refresh token", err)
    }
    rawRefreshToken := hex.EncodeToString(rawBytes)

    // Store SHA-256 hash in database (never store the raw token).
    rt := &model.RefreshToken{
        UserID:    userID,
        TokenHash: sha256Hash(rawRefreshToken),
        ExpiresAt: time.Now().Add(s.refreshTTL),
        Revoked:   false,
    }
    if err := s.tokenRepo.Create(ctx, rt); err != nil {
        return nil, apperr.NewInternal("failed to persist refresh token", err)
    }

    return &model.TokenPair{
        AccessToken:  accessToken,
        RefreshToken: rawRefreshToken,
        ExpiresIn:    int64(s.accessTTL.Seconds()),
    }, nil
}

func sha256Hash(s string) string {
    h := sha256.Sum256([]byte(s))
    return hex.EncodeToString(h[:])
}
```

#### f) Handler Layer

```go
// auth-service/internal/handler/http_handler.go
package handler

import (
    "github.com/gin-gonic/gin"
    "github.com/rs/zerolog"

    "github.com/whatsapp-clone/backend/auth-service/internal/model"
    "github.com/whatsapp-clone/backend/auth-service/internal/service"
    apperr "github.com/whatsapp-clone/backend/pkg/errors"
    "github.com/whatsapp-clone/backend/pkg/response"
    "github.com/whatsapp-clone/backend/pkg/validator"
)

type HTTPHandler struct {
    authSvc service.AuthService
    log     zerolog.Logger
}

func NewHTTPHandler(authSvc service.AuthService, log zerolog.Logger) *HTTPHandler {
    return &HTTPHandler{authSvc: authSvc, log: log}
}

func (h *HTTPHandler) RegisterRoutes(r *gin.RouterGroup) {
    r.POST("/otp/send", h.SendOTP)
    r.POST("/otp/verify", h.VerifyOTP)
    r.POST("/token/refresh", h.RefreshTokens)
    r.POST("/logout", h.Logout)
}

func (h *HTTPHandler) SendOTP(c *gin.Context) {
    var req model.SendOTPRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        response.Error(c, apperr.NewBadRequest("invalid request body"))
        return
    }
    if err := validator.ValidatePhone(req.Phone); err != nil {
        response.Error(c, apperr.NewValidation(err.Error()))
        return
    }

    otp, err := h.authSvc.SendOTP(c.Request.Context(), req.Phone)
    if err != nil {
        response.Error(c, err)
        return
    }

    // In production, OTP is sent via SMS. In dev, return it in the response.
    response.OK(c, gin.H{"message": "OTP sent", "otp_dev": otp})
}

func (h *HTTPHandler) VerifyOTP(c *gin.Context) {
    var req model.VerifyOTPRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        response.Error(c, apperr.NewBadRequest("invalid request body"))
        return
    }

    tokens, err := h.authSvc.VerifyOTP(c.Request.Context(), req.Phone, req.Code)
    if err != nil {
        response.Error(c, err)
        return
    }

    response.OK(c, tokens)
}

func (h *HTTPHandler) RefreshTokens(c *gin.Context) {
    var req model.RefreshRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        response.Error(c, apperr.NewBadRequest("invalid request body"))
        return
    }

    tokens, err := h.authSvc.RefreshTokens(c.Request.Context(), req.RefreshToken)
    if err != nil {
        response.Error(c, err)
        return
    }

    response.OK(c, tokens)
}

func (h *HTTPHandler) Logout(c *gin.Context) {
    var req model.RefreshRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        response.Error(c, apperr.NewBadRequest("invalid request body"))
        return
    }

    if err := h.authSvc.Logout(c.Request.Context(), req.RefreshToken); err != nil {
        response.Error(c, err)
        return
    }

    response.NoContent(c)
}
```

```go
// auth-service/internal/handler/grpc_handler.go
package handler

import (
    "context"

    "github.com/whatsapp-clone/backend/auth-service/internal/service"
    authv1 "github.com/whatsapp-clone/backend/proto/auth/v1"
    "google.golang.org/grpc/codes"
    "google.golang.org/grpc/status"
)

type GRPCHandler struct {
    authv1.UnimplementedAuthServiceServer
    authSvc service.AuthService
}

func NewGRPCHandler(authSvc service.AuthService) *GRPCHandler {
    return &GRPCHandler{authSvc: authSvc}
}

func (h *GRPCHandler) ValidateToken(ctx context.Context, req *authv1.ValidateTokenRequest) (*authv1.ValidateTokenResponse, error) {
    if req.Token == "" {
        return nil, status.Error(codes.InvalidArgument, "token is required")
    }

    userID, phone, err := h.authSvc.ValidateToken(ctx, req.Token)
    if err != nil {
        return &authv1.ValidateTokenResponse{Valid: false}, nil
    }

    return &authv1.ValidateTokenResponse{
        Valid:  true,
        UserId: userID,
        Phone:  phone,
    }, nil
}
```

#### g) Initialization / Bootstrap (`main.go`)

```go
// auth-service/cmd/main.go
package main

import (
    "context"
    "net"
    "net/http"
    "os"
    "os/signal"
    "syscall"
    "time"

    "github.com/gin-gonic/gin"
    "github.com/jackc/pgx/v5/pgxpool"
    "github.com/redis/go-redis/v9"
    "google.golang.org/grpc"

    "github.com/whatsapp-clone/backend/auth-service/config"
    "github.com/whatsapp-clone/backend/auth-service/internal/handler"
    "github.com/whatsapp-clone/backend/auth-service/internal/repository"
    "github.com/whatsapp-clone/backend/auth-service/internal/service"
    pkgcfg "github.com/whatsapp-clone/backend/pkg/config"
    "github.com/whatsapp-clone/backend/pkg/health"
    "github.com/whatsapp-clone/backend/pkg/jwt"
    "github.com/whatsapp-clone/backend/pkg/logger"
    "github.com/whatsapp-clone/backend/pkg/middleware"
)

func main() {
    var cfg config.Config
    if err := pkgcfg.Load(&cfg); err != nil {
        panic(err)
    }

    log := logger.New("auth-service", cfg.LogLevel)

    // PostgreSQL pool.
    pool, err := pgxpool.New(context.Background(), cfg.PostgresDSN)
    if err != nil {
        log.Fatal().Err(err).Msg("failed to connect to PostgreSQL")
    }
    defer pool.Close()

    // Redis client.
    rdb := redis.NewClient(&redis.Options{
        Addr:     cfg.RedisAddr,
        Password: cfg.RedisPassword,
    })
    defer rdb.Close()

    // JWT manager.
    jwtMgr := jwt.NewManager(cfg.JWTSecret, cfg.AccessTokenTTL)

    // Repositories.
    userRepo := repository.NewPostgresUserRepository(pool)
    tokenRepo := repository.NewPostgresRefreshTokenRepository(pool)
    otpRepo := repository.NewRedisOTPRepository(rdb, cfg.OTPTTL)

    // Service.
    authSvc := service.NewAuthService(
        userRepo, tokenRepo, otpRepo, jwtMgr,
        cfg.OTPLength, cfg.OTPMaxAttempts,
        cfg.AccessTokenTTL, cfg.RefreshTokenTTL,
    )

    // HTTP server.
    r := gin.New()
    r.Use(middleware.Recovery(log), middleware.RequestID(), middleware.Logger(log))

    hc := health.NewHandler()
    hc.AddChecker("postgres", func(ctx context.Context) error { return pool.Ping(ctx) })
    hc.AddChecker("redis", func(ctx context.Context) error { return rdb.Ping(ctx).Err() })
    hc.RegisterRoutes(r)

    httpHandler := handler.NewHTTPHandler(authSvc, log)
    httpHandler.RegisterRoutes(r.Group("/api/v1/auth"))

    httpSrv := &http.Server{Addr: cfg.HTTPPort, Handler: r}
    go func() {
        if err := httpSrv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            log.Fatal().Err(err).Msg("HTTP server failed")
        }
    }()

    // gRPC server.
    grpcSrv := grpc.NewServer()
    grpcHandler := handler.NewGRPCHandler(authSvc)
    authv1.RegisterAuthServiceServer(grpcSrv, grpcHandler)

    lis, err := net.Listen("tcp", cfg.GRPCPort)
    if err != nil {
        log.Fatal().Err(err).Msg("failed to listen for gRPC")
    }
    go func() {
        if err := grpcSrv.Serve(lis); err != nil {
            log.Fatal().Err(err).Msg("gRPC server failed")
        }
    }()

    log.Info().Str("http", cfg.HTTPPort).Str("grpc", cfg.GRPCPort).Msg("auth-service started")

    // Graceful shutdown.
    quit := make(chan os.Signal, 1)
    signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
    <-quit

    log.Info().Msg("shutting down auth-service")
    grpcSrv.GracefulStop()
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()
    httpSrv.Shutdown(ctx)
}
```

---

### 3.3 user-service

**Ports:** `:8082` HTTP, `:9082` gRPC

#### a) Configuration Struct

```go
// user-service/config/config.go
package config

import "time"

type Config struct {
    HTTPPort        string        `env:"USER_HTTP_PORT"         envDefault:":8082"`
    GRPCPort        string        `env:"USER_GRPC_PORT"         envDefault:":9082"`
    PostgresDSN     string        `env:"USER_POSTGRES_DSN"      envRequired:"true"`
    RedisAddr       string        `env:"USER_REDIS_ADDR"        envDefault:"redis:6379"`
    RedisPassword   string        `env:"USER_REDIS_PASSWORD"    envDefault:""`
    PresenceTTL     time.Duration `env:"USER_PRESENCE_TTL"      envDefault:"60s"`
    LogLevel        string        `env:"USER_LOG_LEVEL"         envDefault:"info"`
}
```

#### b) Domain Models

```go
// user-service/internal/model/user.go
package model

import "time"

type User struct {
    ID          string    `json:"id"           db:"id"`
    Phone       string    `json:"phone"        db:"phone"`
    DisplayName string    `json:"display_name" db:"display_name"`
    AvatarURL   string    `json:"avatar_url"   db:"avatar_url"`
    StatusText  string    `json:"status_text"  db:"status_text"`
    CreatedAt   time.Time `json:"created_at"   db:"created_at"`
    UpdatedAt   time.Time `json:"updated_at"   db:"updated_at"`
}

type UpdateProfileRequest struct {
    DisplayName *string `json:"display_name"`
    AvatarURL   *string `json:"avatar_url"`
    StatusText  *string `json:"status_text"`
}

type ContactSyncRequest struct {
    Phones []string `json:"phones" binding:"required"`
}

type ContactSyncResult struct {
    Phone  string `json:"phone"`
    UserID string `json:"user_id"`
    DisplayName string `json:"display_name"`
    AvatarURL   string `json:"avatar_url"`
}
```

```go
// user-service/internal/model/contact.go
package model

import "time"

type Contact struct {
    ID        string    `json:"id"         db:"id"`
    UserID    string    `json:"user_id"    db:"user_id"`
    ContactID string    `json:"contact_id" db:"contact_id"`
    Nickname  string    `json:"nickname"   db:"nickname"`
    IsBlocked bool      `json:"is_blocked" db:"is_blocked"`
    CreatedAt time.Time `json:"created_at" db:"created_at"`
}
```

```go
// user-service/internal/model/privacy.go
package model

import "time"

type PrivacyVisibility string

const (
    VisibilityEveryone PrivacyVisibility = "everyone"
    VisibilityContacts PrivacyVisibility = "contacts"
    VisibilityNobody   PrivacyVisibility = "nobody"
)

type PrivacySettings struct {
    UserID       string            `json:"user_id"        db:"user_id"`
    LastSeen     PrivacyVisibility `json:"last_seen"      db:"last_seen"`
    ProfilePhoto PrivacyVisibility `json:"profile_photo"  db:"profile_photo"`
    About        PrivacyVisibility `json:"about"          db:"about"`
    ReadReceipts bool              `json:"read_receipts"  db:"read_receipts"`
    UpdatedAt    time.Time         `json:"updated_at"     db:"updated_at"`
}
```

```go
// user-service/internal/model/device_token.go
package model

import "time"

type DeviceToken struct {
    ID        string    `json:"id"        db:"id"`
    UserID    string    `json:"user_id"   db:"user_id"`
    Token     string    `json:"token"     db:"token"`
    Platform  string    `json:"platform"  db:"platform"` // "android", "ios", "web"
    CreatedAt time.Time `json:"created_at" db:"created_at"`
    UpdatedAt time.Time `json:"updated_at" db:"updated_at"`
}
```

#### c) Repository Interfaces

```go
// user-service/internal/repository/user_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/user-service/internal/model"
)

type UserRepository interface {
    GetByID(ctx context.Context, id string) (*model.User, error)
    GetByIDs(ctx context.Context, ids []string) ([]*model.User, error)
    GetByPhone(ctx context.Context, phone string) (*model.User, error)
    GetByPhones(ctx context.Context, phones []string) ([]*model.ContactSyncResult, error)
    Update(ctx context.Context, id string, req *model.UpdateProfileRequest) (*model.User, error)
}
```

```go
// user-service/internal/repository/contact_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/user-service/internal/model"
)

type ContactRepository interface {
    Upsert(ctx context.Context, contact *model.Contact) error
    GetByUserID(ctx context.Context, userID string) ([]*model.Contact, error)
    IsContact(ctx context.Context, userID, contactID string) (bool, error)
    Block(ctx context.Context, userID, contactID string) error
    Unblock(ctx context.Context, userID, contactID string) error
    IsBlocked(ctx context.Context, userID, contactID string) (bool, error)
    Delete(ctx context.Context, userID, contactID string) error
}
```

```go
// user-service/internal/repository/privacy_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/user-service/internal/model"
)

type PrivacyRepository interface {
    Get(ctx context.Context, userID string) (*model.PrivacySettings, error)
    Upsert(ctx context.Context, settings *model.PrivacySettings) error
}
```

```go
// user-service/internal/repository/device_token_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/user-service/internal/model"
)

type DeviceTokenRepository interface {
    Upsert(ctx context.Context, token *model.DeviceToken) error
    GetByUserID(ctx context.Context, userID string) ([]*model.DeviceToken, error)
    DeleteByToken(ctx context.Context, token string) error
    DeleteByUserID(ctx context.Context, userID string) error
}
```

```go
// user-service/internal/repository/presence_repository.go
package repository

import (
    "context"
    "time"
)

type PresenceRepository interface {
    // SetOnline marks the user as online with the given TTL.
    SetOnline(ctx context.Context, userID string, ttl time.Duration) error

    // IsOnline returns true if the user has an active presence key.
    IsOnline(ctx context.Context, userID string) (bool, error)

    // SetLastSeen stores the last-seen timestamp (called when presence expires or user disconnects).
    SetLastSeen(ctx context.Context, userID string, t time.Time) error

    // GetLastSeen retrieves the last-seen timestamp for a user.
    GetLastSeen(ctx context.Context, userID string) (time.Time, error)

    // Remove deletes the presence key (explicit offline).
    Remove(ctx context.Context, userID string) error
}
```

#### d) Service Interface

```go
// user-service/internal/service/user_service.go
package service

import (
    "context"

    "github.com/whatsapp-clone/backend/user-service/internal/model"
)

type UserService interface {
    GetProfile(ctx context.Context, callerID, targetID string) (*model.User, error)
    UpdateProfile(ctx context.Context, userID string, req *model.UpdateProfileRequest) (*model.User, error)
    ContactSync(ctx context.Context, userID string, phones []string) ([]*model.ContactSyncResult, error)
    GetContacts(ctx context.Context, userID string) ([]*model.Contact, error)
    BlockUser(ctx context.Context, userID, targetID string) error
    UnblockUser(ctx context.Context, userID, targetID string) error
    GetPrivacySettings(ctx context.Context, userID string) (*model.PrivacySettings, error)
    UpdatePrivacySettings(ctx context.Context, settings *model.PrivacySettings) error
    RegisterDeviceToken(ctx context.Context, token *model.DeviceToken) error
    RemoveDeviceToken(ctx context.Context, token string) error
    SetPresence(ctx context.Context, userID string, online bool) error
    CheckPresence(ctx context.Context, userID string) (online bool, lastSeen *time.Time, err error)
}
```

#### e) Key Business Logic — Privacy-filtered GetProfile

```go
// user-service/internal/service/user_service_impl.go  (excerpt)

// GetProfile retrieves a user profile, filtering fields based on the caller's
// relationship to the target and the target's privacy settings.
func (s *userServiceImpl) GetProfile(ctx context.Context, callerID, targetID string) (*model.User, error) {
    // Check if the target has blocked the caller.
    blocked, err := s.contactRepo.IsBlocked(ctx, targetID, callerID)
    if err != nil {
        return nil, apperr.NewInternal("failed to check block status", err)
    }
    if blocked {
        return nil, apperr.NewForbidden("user not available")
    }

    user, err := s.userRepo.GetByID(ctx, targetID)
    if err != nil {
        return nil, err
    }
    if user == nil {
        return nil, apperr.NewNotFound("user not found")
    }

    // Load privacy settings of the target.
    privacy, err := s.privacyRepo.Get(ctx, targetID)
    if err != nil {
        return nil, apperr.NewInternal("failed to load privacy settings", err)
    }
    if privacy == nil {
        return user, nil // Default: show everything.
    }

    // Determine relationship: is the caller in the target's contacts?
    isContact, err := s.contactRepo.IsContact(ctx, targetID, callerID)
    if err != nil {
        return nil, apperr.NewInternal("failed to check contact status", err)
    }

    // Apply privacy filters.
    if !canSee(privacy.ProfilePhoto, isContact, callerID == targetID) {
        user.AvatarURL = ""
    }
    if !canSee(privacy.About, isContact, callerID == targetID) {
        user.StatusText = ""
    }

    return user, nil
}

// canSee determines if a field is visible based on privacy setting and relationship.
func canSee(vis model.PrivacyVisibility, isContact, isSelf bool) bool {
    if isSelf {
        return true
    }
    switch vis {
    case model.VisibilityEveryone:
        return true
    case model.VisibilityContacts:
        return isContact
    case model.VisibilityNobody:
        return false
    default:
        return true
    }
}
```

#### f) Key Business Logic — ContactSync

```go
// ContactSync looks up registered users by batch of phone numbers.
func (s *userServiceImpl) ContactSync(ctx context.Context, userID string, phones []string) ([]*model.ContactSyncResult, error) {
    if len(phones) == 0 {
        return nil, nil
    }
    if len(phones) > 500 {
        return nil, apperr.NewBadRequest("max 500 phones per sync request")
    }

    // Batch lookup: SELECT id, phone, display_name, avatar_url FROM users WHERE phone IN ($1, $2, ...).
    results, err := s.userRepo.GetByPhones(ctx, phones)
    if err != nil {
        return nil, apperr.NewInternal("failed to sync contacts", err)
    }

    // Auto-add as contacts.
    for _, r := range results {
        if r.UserID != userID {
            _ = s.contactRepo.Upsert(ctx, &model.Contact{
                UserID:    userID,
                ContactID: r.UserID,
            })
        }
    }

    return results, nil
}
```

#### g) gRPC Handler

```go
// user-service/internal/handler/grpc_handler.go
package handler

import (
    "context"

    "github.com/whatsapp-clone/backend/user-service/internal/service"
    userv1 "github.com/whatsapp-clone/backend/proto/user/v1"
    "google.golang.org/grpc/codes"
    "google.golang.org/grpc/status"
    "google.golang.org/protobuf/types/known/timestamppb"
)

type GRPCHandler struct {
    userv1.UnimplementedUserServiceServer
    userSvc service.UserService
}

func NewGRPCHandler(userSvc service.UserService) *GRPCHandler {
    return &GRPCHandler{userSvc: userSvc}
}

func (h *GRPCHandler) GetUser(ctx context.Context, req *userv1.GetUserRequest) (*userv1.GetUserResponse, error) {
    if req.UserId == "" {
        return nil, status.Error(codes.InvalidArgument, "user_id required")
    }
    user, err := h.userSvc.GetProfile(ctx, "", req.UserId) // no caller context for internal gRPC
    if err != nil {
        return nil, status.Error(codes.NotFound, err.Error())
    }
    return &userv1.GetUserResponse{
        User: &userv1.UserProfile{
            UserId:      user.ID,
            Phone:       user.Phone,
            DisplayName: user.DisplayName,
            AvatarUrl:   user.AvatarURL,
            StatusText:  user.StatusText,
            CreatedAt:   timestamppb.New(user.CreatedAt),
            UpdatedAt:   timestamppb.New(user.UpdatedAt),
        },
    }, nil
}

func (h *GRPCHandler) GetUsers(ctx context.Context, req *userv1.GetUsersRequest) (*userv1.GetUsersResponse, error) {
    if len(req.UserIds) == 0 {
        return &userv1.GetUsersResponse{}, nil
    }
    users, err := h.userSvc.GetUsersByIDs(ctx, req.UserIds)
    if err != nil {
        return nil, status.Error(codes.Internal, err.Error())
    }
    var profiles []*userv1.UserProfile
    for _, u := range users {
        profiles = append(profiles, &userv1.UserProfile{
            UserId:      u.ID,
            Phone:       u.Phone,
            DisplayName: u.DisplayName,
            AvatarUrl:   u.AvatarURL,
            StatusText:  u.StatusText,
            CreatedAt:   timestamppb.New(u.CreatedAt),
            UpdatedAt:   timestamppb.New(u.UpdatedAt),
        })
    }
    return &userv1.GetUsersResponse{Users: profiles}, nil
}

func (h *GRPCHandler) CheckPresence(ctx context.Context, req *userv1.CheckPresenceRequest) (*userv1.CheckPresenceResponse, error) {
    online, lastSeen, err := h.userSvc.CheckPresence(ctx, req.UserId)
    if err != nil {
        return nil, status.Error(codes.Internal, err.Error())
    }
    resp := &userv1.CheckPresenceResponse{Online: online}
    if lastSeen != nil {
        resp.LastSeen = timestamppb.New(*lastSeen)
    }
    return resp, nil
}
```

#### h) HTTP Handler

```go
// user-service/internal/handler/http_handler.go
package handler

import (
    "github.com/gin-gonic/gin"
    "github.com/rs/zerolog"

    "github.com/whatsapp-clone/backend/user-service/internal/model"
    "github.com/whatsapp-clone/backend/user-service/internal/service"
    apperr "github.com/whatsapp-clone/backend/pkg/errors"
    "github.com/whatsapp-clone/backend/pkg/response"
)

type HTTPHandler struct {
    userSvc service.UserService
    log     zerolog.Logger
}

func NewHTTPHandler(userSvc service.UserService, log zerolog.Logger) *HTTPHandler {
    return &HTTPHandler{userSvc: userSvc, log: log}
}

func (h *HTTPHandler) RegisterRoutes(r *gin.RouterGroup) {
    r.GET("/me", h.GetMyProfile)
    r.PUT("/me", h.UpdateMyProfile)
    r.GET("/:id", h.GetUserProfile)
    r.POST("/contacts/sync", h.ContactSync)
    r.GET("/contacts", h.GetContacts)
    r.POST("/contacts/:id/block", h.BlockUser)
    r.DELETE("/contacts/:id/block", h.UnblockUser)
    r.GET("/privacy", h.GetPrivacySettings)
    r.PUT("/privacy", h.UpdatePrivacySettings)
    r.POST("/devices", h.RegisterDevice)
    r.DELETE("/devices/:token", h.RemoveDevice)
}

func (h *HTTPHandler) GetMyProfile(c *gin.Context) {
    userID := c.GetString("user_id")
    user, err := h.userSvc.GetProfile(c.Request.Context(), userID, userID)
    if err != nil {
        response.Error(c, err)
        return
    }
    response.OK(c, user)
}

func (h *HTTPHandler) UpdateMyProfile(c *gin.Context) {
    userID := c.GetString("user_id")
    var req model.UpdateProfileRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        response.Error(c, apperr.NewBadRequest("invalid request body"))
        return
    }
    user, err := h.userSvc.UpdateProfile(c.Request.Context(), userID, &req)
    if err != nil {
        response.Error(c, err)
        return
    }
    response.OK(c, user)
}

func (h *HTTPHandler) GetUserProfile(c *gin.Context) {
    callerID := c.GetString("user_id")
    targetID := c.Param("id")
    user, err := h.userSvc.GetProfile(c.Request.Context(), callerID, targetID)
    if err != nil {
        response.Error(c, err)
        return
    }
    response.OK(c, user)
}

func (h *HTTPHandler) ContactSync(c *gin.Context) {
    userID := c.GetString("user_id")
    var req model.ContactSyncRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        response.Error(c, apperr.NewBadRequest("invalid request body"))
        return
    }
    results, err := h.userSvc.ContactSync(c.Request.Context(), userID, req.Phones)
    if err != nil {
        response.Error(c, err)
        return
    }
    response.OK(c, results)
}

func (h *HTTPHandler) GetContacts(c *gin.Context) {
    userID := c.GetString("user_id")
    contacts, err := h.userSvc.GetContacts(c.Request.Context(), userID)
    if err != nil {
        response.Error(c, err)
        return
    }
    response.OK(c, contacts)
}

func (h *HTTPHandler) BlockUser(c *gin.Context) {
    userID := c.GetString("user_id")
    targetID := c.Param("id")
    if err := h.userSvc.BlockUser(c.Request.Context(), userID, targetID); err != nil {
        response.Error(c, err)
        return
    }
    response.NoContent(c)
}

func (h *HTTPHandler) UnblockUser(c *gin.Context) {
    userID := c.GetString("user_id")
    targetID := c.Param("id")
    if err := h.userSvc.UnblockUser(c.Request.Context(), userID, targetID); err != nil {
        response.Error(c, err)
        return
    }
    response.NoContent(c)
}

func (h *HTTPHandler) GetPrivacySettings(c *gin.Context) {
    userID := c.GetString("user_id")
    settings, err := h.userSvc.GetPrivacySettings(c.Request.Context(), userID)
    if err != nil {
        response.Error(c, err)
        return
    }
    response.OK(c, settings)
}

func (h *HTTPHandler) UpdatePrivacySettings(c *gin.Context) {
    userID := c.GetString("user_id")
    var settings model.PrivacySettings
    if err := c.ShouldBindJSON(&settings); err != nil {
        response.Error(c, apperr.NewBadRequest("invalid request body"))
        return
    }
    settings.UserID = userID
    if err := h.userSvc.UpdatePrivacySettings(c.Request.Context(), &settings); err != nil {
        response.Error(c, err)
        return
    }
    response.NoContent(c)
}

func (h *HTTPHandler) RegisterDevice(c *gin.Context) {
    userID := c.GetString("user_id")
    var dt model.DeviceToken
    if err := c.ShouldBindJSON(&dt); err != nil {
        response.Error(c, apperr.NewBadRequest("invalid request body"))
        return
    }
    dt.UserID = userID
    if err := h.userSvc.RegisterDeviceToken(c.Request.Context(), &dt); err != nil {
        response.Error(c, err)
        return
    }
    response.Created(c, dt)
}

func (h *HTTPHandler) RemoveDevice(c *gin.Context) {
    token := c.Param("token")
    if err := h.userSvc.RemoveDeviceToken(c.Request.Context(), token); err != nil {
        response.Error(c, err)
        return
    }
    response.NoContent(c)
}
```

---

### 3.4 chat-service

**Ports:** `:8083` HTTP, `:9083` gRPC

#### a) Configuration Struct

```go
// chat-service/config/config.go
package config

type Config struct {
    HTTPPort       string `env:"CHAT_HTTP_PORT"        envDefault:":8083"`
    GRPCPort       string `env:"CHAT_GRPC_PORT"        envDefault:":9083"`
    PostgresDSN    string `env:"CHAT_POSTGRES_DSN"     envRequired:"true"`
    NATSUrl        string `env:"CHAT_NATS_URL"         envDefault:"nats://nats:4222"`
    MessageGRPC    string `env:"CHAT_MESSAGE_GRPC_ADDR" envDefault:"message-service:9084"`
    LogLevel       string `env:"CHAT_LOG_LEVEL"        envDefault:"info"`
}
```

#### b) Domain Models

```go
// chat-service/internal/model/chat.go
package model

import "time"

type ChatType string

const (
    ChatTypeDirect ChatType = "direct"
    ChatTypeGroup  ChatType = "group"
)

type Chat struct {
    ID        string    `json:"id"         db:"id"`
    Type      ChatType  `json:"type"       db:"type"`
    CreatedAt time.Time `json:"created_at" db:"created_at"`
    UpdatedAt time.Time `json:"updated_at" db:"updated_at"`
}

type ChatParticipant struct {
    ID        string     `json:"id"         db:"id"`
    ChatID    string     `json:"chat_id"    db:"chat_id"`
    UserID    string     `json:"user_id"    db:"user_id"`
    Role      string     `json:"role"       db:"role"` // "admin" or "member"
    IsMuted   bool       `json:"is_muted"   db:"is_muted"`
    MuteUntil *time.Time `json:"mute_until" db:"mute_until"`
    IsPinned  bool       `json:"is_pinned"  db:"is_pinned"`
    JoinedAt  time.Time  `json:"joined_at"  db:"joined_at"`
}

type Group struct {
    ChatID      string    `json:"chat_id"       db:"chat_id"`
    Name        string    `json:"name"          db:"name"`
    Description string    `json:"description"   db:"description"`
    AvatarURL   string    `json:"avatar_url"    db:"avatar_url"`
    CreatedBy   string    `json:"created_by"    db:"created_by"`
    IsAdminOnly bool      `json:"is_admin_only" db:"is_admin_only"`
    CreatedAt   time.Time `json:"created_at"    db:"created_at"`
    UpdatedAt   time.Time `json:"updated_at"    db:"updated_at"`
}
```

```go
// chat-service/internal/model/requests.go
package model

type CreateDirectChatRequest struct {
    OtherUserID string `json:"other_user_id" binding:"required"`
}

type CreateGroupRequest struct {
    Name        string   `json:"name"        binding:"required"`
    Description string   `json:"description"`
    MemberIDs   []string `json:"member_ids"  binding:"required"`
}

type UpdateGroupRequest struct {
    Name        *string `json:"name"`
    Description *string `json:"description"`
    AvatarURL   *string `json:"avatar_url"`
    IsAdminOnly *bool   `json:"is_admin_only"`
}

type AddMemberRequest struct {
    UserID string `json:"user_id" binding:"required"`
}

type ChatListItem struct {
    Chat           Chat             `json:"chat"`
    Participants   []ChatParticipant `json:"participants"`
    Group          *Group           `json:"group,omitempty"`
    LastMessage    *MessagePreview  `json:"last_message,omitempty"`
    UnreadCount    int64            `json:"unread_count"`
}

type MessagePreview struct {
    MessageID string `json:"message_id"`
    SenderID  string `json:"sender_id"`
    Type      string `json:"type"`
    Body      string `json:"body"`
    CreatedAt int64  `json:"created_at"`
}
```

#### c) Repository Interfaces

```go
// chat-service/internal/repository/chat_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/chat-service/internal/model"
)

type ChatRepository interface {
    // CreateDirect creates a direct chat with two participants in a single transaction.
    CreateDirect(ctx context.Context, chat *model.Chat, participants [2]model.ChatParticipant) error

    // FindDirectChat finds an existing direct chat between two users.
    FindDirectChat(ctx context.Context, userID1, userID2 string) (*model.Chat, error)

    // CreateGroup creates a group chat with initial participants and group metadata
    // atomically in a single transaction.
    CreateGroup(ctx context.Context, chat *model.Chat, group *model.Group, participants []model.ChatParticipant) error

    // GetByID retrieves a chat by ID.
    GetByID(ctx context.Context, chatID string) (*model.Chat, error)

    // GetUserChats retrieves all chat IDs that a user is a participant of.
    GetUserChats(ctx context.Context, userID string) ([]string, error)

    // GetParticipants returns all participants for a chat.
    GetParticipants(ctx context.Context, chatID string) ([]model.ChatParticipant, error)

    // GetParticipant returns a specific participant in a chat.
    GetParticipant(ctx context.Context, chatID, userID string) (*model.ChatParticipant, error)

    // AddParticipant adds a user to a chat.
    AddParticipant(ctx context.Context, p *model.ChatParticipant) error

    // RemoveParticipant removes a user from a chat.
    RemoveParticipant(ctx context.Context, chatID, userID string) error

    // UpdateParticipantRole changes a participant's role (admin/member).
    UpdateParticipantRole(ctx context.Context, chatID, userID, role string) error

    // UpdateMute sets mute status for a participant.
    UpdateMute(ctx context.Context, chatID, userID string, isMuted bool, muteUntil *time.Time) error

    // UpdatePin sets pin status for a participant.
    UpdatePin(ctx context.Context, chatID, userID string, isPinned bool) error

    // GetGroup returns group metadata.
    GetGroup(ctx context.Context, chatID string) (*model.Group, error)

    // UpdateGroup updates group metadata.
    UpdateGroup(ctx context.Context, chatID string, req *model.UpdateGroupRequest) error
}
```

#### d) Service Interface

```go
// chat-service/internal/service/chat_service.go
package service

import (
    "context"

    "github.com/whatsapp-clone/backend/chat-service/internal/model"
)

type ChatService interface {
    // CreateDirectChat creates or returns an existing direct chat between two users.
    CreateDirectChat(ctx context.Context, callerID string, req *model.CreateDirectChatRequest) (*model.Chat, error)

    // CreateGroup creates a new group chat with the caller as admin.
    CreateGroup(ctx context.Context, callerID string, req *model.CreateGroupRequest) (*model.Chat, *model.Group, error)

    // ListChats returns all chats for a user with last message previews and unread counts.
    ListChats(ctx context.Context, userID string) ([]*model.ChatListItem, error)

    // GetChat retrieves a single chat by ID with full details.
    GetChat(ctx context.Context, callerID, chatID string) (*model.ChatListItem, error)

    // AddMember adds a user to a group chat (admin only).
    AddMember(ctx context.Context, callerID, chatID, targetUserID string) error

    // RemoveMember removes a user from a group chat (admin only, or self-removal).
    RemoveMember(ctx context.Context, callerID, chatID, targetUserID string) error

    // PromoteMember promotes a member to admin (admin only).
    PromoteMember(ctx context.Context, callerID, chatID, targetUserID string) error

    // DemoteMember demotes an admin to member (admin only).
    DemoteMember(ctx context.Context, callerID, chatID, targetUserID string) error

    // UpdateGroup updates group metadata (admin only).
    UpdateGroup(ctx context.Context, callerID, chatID string, req *model.UpdateGroupRequest) error

    // MuteChat mutes/unmutes a chat for the caller.
    MuteChat(ctx context.Context, userID, chatID string, mute bool, muteUntil *time.Time) error

    // PinChat pins/unpins a chat for the caller.
    PinChat(ctx context.Context, userID, chatID string, pin bool) error
}
```

#### e) Key Business Logic — Direct Chat Idempotency

```go
// chat-service/internal/service/chat_service_impl.go (excerpt)

func (s *chatServiceImpl) CreateDirectChat(ctx context.Context, callerID string, req *model.CreateDirectChatRequest) (*model.Chat, error) {
    if callerID == req.OtherUserID {
        return nil, apperr.NewBadRequest("cannot create chat with yourself")
    }

    // Idempotency: check if a direct chat already exists between these two users.
    existing, err := s.chatRepo.FindDirectChat(ctx, callerID, req.OtherUserID)
    if err != nil {
        return nil, apperr.NewInternal("failed to check existing chat", err)
    }
    if existing != nil {
        return existing, nil
    }

    chatID := uuid.New().String()
    now := time.Now()

    chat := &model.Chat{
        ID:        chatID,
        Type:      model.ChatTypeDirect,
        CreatedAt: now,
        UpdatedAt: now,
    }

    participants := [2]model.ChatParticipant{
        {ID: uuid.New().String(), ChatID: chatID, UserID: callerID, Role: "member", JoinedAt: now},
        {ID: uuid.New().String(), ChatID: chatID, UserID: req.OtherUserID, Role: "member", JoinedAt: now},
    }

    if err := s.chatRepo.CreateDirect(ctx, chat, participants); err != nil {
        return nil, apperr.NewInternal("failed to create direct chat", err)
    }

    // Publish NATS event.
    s.publishEvent("chat.created", map[string]interface{}{
        "chat_id":      chatID,
        "type":         "direct",
        "participants": []string{callerID, req.OtherUserID},
    })

    return chat, nil
}
```

#### f) Key Business Logic — Group Creation (Transaction)

```go
func (s *chatServiceImpl) CreateGroup(ctx context.Context, callerID string, req *model.CreateGroupRequest) (*model.Chat, *model.Group, error) {
    chatID := uuid.New().String()
    now := time.Now()

    chat := &model.Chat{
        ID:        chatID,
        Type:      model.ChatTypeGroup,
        CreatedAt: now,
        UpdatedAt: now,
    }

    group := &model.Group{
        ChatID:    chatID,
        Name:      req.Name,
        Description: req.Description,
        CreatedBy: callerID,
        CreatedAt: now,
        UpdatedAt: now,
    }

    // Build participant list: caller is admin, all others are members.
    participants := make([]model.ChatParticipant, 0, len(req.MemberIDs)+1)
    participants = append(participants, model.ChatParticipant{
        ID: uuid.New().String(), ChatID: chatID, UserID: callerID, Role: "admin", JoinedAt: now,
    })
    for _, memberID := range req.MemberIDs {
        if memberID == callerID {
            continue // Skip duplicate.
        }
        participants = append(participants, model.ChatParticipant{
            ID: uuid.New().String(), ChatID: chatID, UserID: memberID, Role: "member", JoinedAt: now,
        })
    }

    // Atomic transaction: inserts chats + chat_participants + groups.
    if err := s.chatRepo.CreateGroup(ctx, chat, group, participants); err != nil {
        return nil, nil, apperr.NewInternal("failed to create group", err)
    }

    s.publishEvent("chat.created", map[string]interface{}{
        "chat_id": chatID,
        "type":    "group",
        "name":    req.Name,
        "members": append(req.MemberIDs, callerID),
    })

    for _, memberID := range req.MemberIDs {
        s.publishEvent("group.member.added", map[string]interface{}{
            "chat_id":  chatID,
            "user_id":  memberID,
            "added_by": callerID,
        })
    }

    return chat, group, nil
}
```

#### g) Key Business Logic — List Chats with Message Preview

```go
func (s *chatServiceImpl) ListChats(ctx context.Context, userID string) ([]*model.ChatListItem, error) {
    // Get all chat IDs the user participates in.
    chatIDs, err := s.chatRepo.GetUserChats(ctx, userID)
    if err != nil {
        return nil, apperr.NewInternal("failed to get user chats", err)
    }
    if len(chatIDs) == 0 {
        return []*model.ChatListItem{}, nil
    }

    // Fetch last messages from message-service via gRPC (batch).
    lastMsgsResp, err := s.messageClient.GetLastMessages(ctx, &messagev1.GetLastMessagesRequest{
        ChatIds: chatIDs,
    })
    if err != nil {
        // Non-fatal: we can show chats without message previews.
        lastMsgsResp = &messagev1.GetLastMessagesResponse{Messages: map[string]*messagev1.MessagePreview{}}
    }

    // Fetch unread counts from message-service via gRPC (batch).
    unreadResp, err := s.messageClient.GetUnreadCounts(ctx, &messagev1.GetUnreadCountsRequest{
        UserId:  userID,
        ChatIds: chatIDs,
    })
    if err != nil {
        unreadResp = &messagev1.GetUnreadCountsResponse{Counts: map[string]int64{}}
    }

    // Assemble chat list.
    items := make([]*model.ChatListItem, 0, len(chatIDs))
    for _, chatID := range chatIDs {
        chat, err := s.chatRepo.GetByID(ctx, chatID)
        if err != nil || chat == nil {
            continue
        }

        participants, _ := s.chatRepo.GetParticipants(ctx, chatID)

        item := &model.ChatListItem{
            Chat:         *chat,
            Participants: participants,
            UnreadCount:  unreadResp.Counts[chatID],
        }

        if chat.Type == model.ChatTypeGroup {
            group, _ := s.chatRepo.GetGroup(ctx, chatID)
            item.Group = group
        }

        if preview, ok := lastMsgsResp.Messages[chatID]; ok {
            item.LastMessage = &model.MessagePreview{
                MessageID: preview.MessageId,
                SenderID:  preview.SenderId,
                Type:      preview.Type,
                Body:      preview.Body,
                CreatedAt: preview.CreatedAt.AsTime().UnixMilli(),
            }
        }

        items = append(items, item)
    }

    return items, nil
}
```

#### h) NATS Event Publisher

```go
// chat-service/internal/service/publisher.go
package service

import (
    "encoding/json"

    "github.com/nats-io/nats.go"
    "github.com/rs/zerolog"
)

type eventPublisher struct {
    js  nats.JetStreamContext
    log zerolog.Logger
}

func (p *eventPublisher) publishEvent(subject string, payload interface{}) {
    data, err := json.Marshal(payload)
    if err != nil {
        p.log.Error().Err(err).Str("subject", subject).Msg("failed to marshal NATS event")
        return
    }
    if _, err := p.js.Publish(subject, data); err != nil {
        p.log.Error().Err(err).Str("subject", subject).Msg("failed to publish NATS event")
    }
}
```

---

### 3.5 message-service

**Ports:** `:8084` HTTP, `:9084` gRPC

#### a) Configuration Struct

```go
// message-service/config/config.go
package config

type Config struct {
    HTTPPort    string `env:"MESSAGE_HTTP_PORT"     envDefault:":8084"`
    GRPCPort    string `env:"MESSAGE_GRPC_PORT"     envDefault:":9084"`
    MongoURI    string `env:"MESSAGE_MONGO_URI"     envRequired:"true"`
    MongoDB     string `env:"MESSAGE_MONGO_DB"      envDefault:"whatsapp"`
    NATSUrl     string `env:"MESSAGE_NATS_URL"      envDefault:"nats://nats:4222"`
    LogLevel    string `env:"MESSAGE_LOG_LEVEL"     envDefault:"info"`
}
```

#### b) Domain Models

```go
// message-service/internal/model/message.go
package model

import "time"

type MessageType string

const (
    MessageTypeText     MessageType = "text"
    MessageTypeImage    MessageType = "image"
    MessageTypeVideo    MessageType = "video"
    MessageTypeAudio    MessageType = "audio"
    MessageTypeDocument MessageType = "document"
    MessageTypeLocation MessageType = "location"
)

type MessageStatus string

const (
    StatusSent      MessageStatus = "sent"
    StatusDelivered MessageStatus = "delivered"
    StatusRead      MessageStatus = "read"
)

type Message struct {
    MessageID        string                    `json:"message_id"         bson:"message_id"`
    ChatID           string                    `json:"chat_id"            bson:"chat_id"`
    SenderID         string                    `json:"sender_id"          bson:"sender_id"`
    ClientMsgID      string                    `json:"client_msg_id"      bson:"client_msg_id"`
    Type             MessageType               `json:"type"               bson:"type"`
    ReplyToMessageID string                    `json:"reply_to_message_id,omitempty" bson:"reply_to_message_id,omitempty"`
    ForwardedFrom    *ForwardedFrom            `json:"forwarded_from,omitempty"      bson:"forwarded_from,omitempty"`
    Payload          MessagePayload            `json:"payload"            bson:"payload"`
    Status           map[string]RecipientStatus `json:"status"            bson:"status"` // keyed by user_id
    IsDeleted        bool                      `json:"is_deleted"         bson:"is_deleted"`
    IsStarredBy      []string                  `json:"is_starred_by"      bson:"is_starred_by"`
    CreatedAt        time.Time                 `json:"created_at"         bson:"created_at"`
    UpdatedAt        time.Time                 `json:"updated_at"         bson:"updated_at"`
}

type ForwardedFrom struct {
    ChatID    string `json:"chat_id"    bson:"chat_id"`
    MessageID string `json:"message_id" bson:"message_id"`
}

type MessagePayload struct {
    Body       string `json:"body,omitempty"        bson:"body,omitempty"`
    MediaID    string `json:"media_id,omitempty"    bson:"media_id,omitempty"`
    Caption    string `json:"caption,omitempty"     bson:"caption,omitempty"`
    Filename   string `json:"filename,omitempty"    bson:"filename,omitempty"`
    DurationMs int64  `json:"duration_ms,omitempty" bson:"duration_ms,omitempty"`
}

type RecipientStatus struct {
    Status    MessageStatus `json:"status"     bson:"status"`
    UpdatedAt time.Time     `json:"updated_at" bson:"updated_at"`
}
```

```go
// message-service/internal/model/requests.go
package model

type SendMessageRequest struct {
    ChatID           string          `json:"chat_id"            binding:"required"`
    Type             MessageType     `json:"type"               binding:"required"`
    Payload          MessagePayload  `json:"payload"            binding:"required"`
    ClientMsgID      string          `json:"client_msg_id"      binding:"required"`
    ReplyToMessageID string          `json:"reply_to_message_id"`
    ForwardedFrom    *ForwardedFrom  `json:"forwarded_from"`
}

type UpdateStatusRequest struct {
    Status string `json:"status" binding:"required"` // "delivered" or "read"
}

type ListMessagesQuery struct {
    ChatID    string `form:"chat_id"    binding:"required"`
    Cursor    string `form:"cursor"`     // ISO timestamp of last message seen
    CursorID  string `form:"cursor_id"`  // message_id for tie-breaking
    Limit     int    `form:"limit"`      // default 50, max 100
}

type SearchMessagesQuery struct {
    ChatID string `form:"chat_id" binding:"required"`
    Query  string `form:"q"       binding:"required"`
    Limit  int    `form:"limit"`
}
```

#### c) Repository Interface

```go
// message-service/internal/repository/message_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/message-service/internal/model"
)

type MessageRepository interface {
    // Insert creates a new message. Uses client_msg_id unique index for idempotency.
    // Returns the existing message if client_msg_id already exists.
    Insert(ctx context.Context, msg *model.Message) (*model.Message, error)

    // GetByID retrieves a single message by message_id.
    GetByID(ctx context.Context, messageID string) (*model.Message, error)

    // ListByChatID returns messages for a chat using cursor-based pagination.
    // Cursor is (created_at, message_id) for deterministic ordering.
    ListByChatID(ctx context.Context, chatID string, cursorTime *time.Time, cursorID string, limit int) ([]*model.Message, error)

    // UpdateStatus updates the status map entry for a specific recipient.
    UpdateStatus(ctx context.Context, messageID, userID string, status model.RecipientStatus) error

    // SoftDelete marks a message as deleted (sets is_deleted=true, clears payload).
    SoftDelete(ctx context.Context, messageID, senderID string) error

    // StarMessage adds userID to is_starred_by array.
    StarMessage(ctx context.Context, messageID, userID string) error

    // UnstarMessage removes userID from is_starred_by array.
    UnstarMessage(ctx context.Context, messageID, userID string) error

    // Search performs a full-text search within a chat using MongoDB $text index.
    Search(ctx context.Context, chatID, query string, limit int) ([]*model.Message, error)

    // GetLastPerChat returns the latest message for each given chat ID.
    GetLastPerChat(ctx context.Context, chatIDs []string) (map[string]*model.Message, error)

    // CountUnread returns the count of messages in each chat where
    // the user's status is "sent" (not delivered or read).
    CountUnread(ctx context.Context, userID string, chatIDs []string) (map[string]int64, error)
}
```

#### d) Service Interface

```go
// message-service/internal/service/message_service.go
package service

import (
    "context"

    "github.com/whatsapp-clone/backend/message-service/internal/model"
)

type MessageService interface {
    SendMessage(ctx context.Context, senderID string, req *model.SendMessageRequest) (*model.Message, error)
    GetMessages(ctx context.Context, query *model.ListMessagesQuery) ([]*model.Message, error)
    UpdateStatus(ctx context.Context, messageID, userID, status string) error
    DeleteMessage(ctx context.Context, messageID, senderID string) error
    StarMessage(ctx context.Context, messageID, userID string) error
    UnstarMessage(ctx context.Context, messageID, userID string) error
    ForwardMessage(ctx context.Context, senderID, targetChatID, sourceMessageID string) (*model.Message, error)
    SearchMessages(ctx context.Context, chatID, query string, limit int) ([]*model.Message, error)
    GetLastMessages(ctx context.Context, chatIDs []string) (map[string]*model.Message, error)
    GetUnreadCounts(ctx context.Context, userID string, chatIDs []string) (map[string]int64, error)
}
```

#### e) Key Business Logic — SendMessage with Dedup

```go
// message-service/internal/service/message_service_impl.go (excerpt)

func (s *messageServiceImpl) SendMessage(ctx context.Context, senderID string, req *model.SendMessageRequest) (*model.Message, error) {
    now := time.Now()
    msgID := uuid.New().String()

    msg := &model.Message{
        MessageID:        msgID,
        ChatID:           req.ChatID,
        SenderID:         senderID,
        ClientMsgID:      req.ClientMsgID,
        Type:             req.Type,
        ReplyToMessageID: req.ReplyToMessageID,
        ForwardedFrom:    req.ForwardedFrom,
        Payload:          req.Payload,
        Status:           make(map[string]model.RecipientStatus),
        IsDeleted:        false,
        IsStarredBy:      []string{},
        CreatedAt:        now,
        UpdatedAt:        now,
    }

    // Insert with client_msg_id dedup (unique index).
    // If client_msg_id already exists, the repo returns the existing message.
    result, err := s.messageRepo.Insert(ctx, msg)
    if err != nil {
        return nil, apperr.NewInternal("failed to insert message", err)
    }

    // Publish NATS event for real-time delivery.
    s.publishEvent("msg.new", map[string]interface{}{
        "message_id":   result.MessageID,
        "chat_id":      result.ChatID,
        "sender_id":    result.SenderID,
        "type":         result.Type,
        "payload":      result.Payload,
        "created_at":   result.CreatedAt,
    })

    return result, nil
}
```

#### f) Key Business Logic — Cursor-Based Pagination

```go
func (s *messageServiceImpl) GetMessages(ctx context.Context, query *model.ListMessagesQuery) ([]*model.Message, error) {
    limit := query.Limit
    if limit <= 0 || limit > 100 {
        limit = 50
    }

    var cursorTime *time.Time
    if query.Cursor != "" {
        t, err := time.Parse(time.RFC3339Nano, query.Cursor)
        if err != nil {
            return nil, apperr.NewBadRequest("invalid cursor format, expected RFC3339Nano")
        }
        cursorTime = &t
    }

    // MongoDB query:
    // db.messages.find({
    //   chat_id: chatID,
    //   is_deleted: false,
    //   $or: [
    //     { created_at: { $lt: cursorTime } },
    //     { created_at: cursorTime, message_id: { $lt: cursorID } }
    //   ]
    // }).sort({ created_at: -1, message_id: -1 }).limit(limit)
    msgs, err := s.messageRepo.ListByChatID(ctx, query.ChatID, cursorTime, query.CursorID, limit)
    if err != nil {
        return nil, apperr.NewInternal("failed to list messages", err)
    }
    return msgs, nil
}
```

#### g) Key Business Logic — Forward

```go
func (s *messageServiceImpl) ForwardMessage(ctx context.Context, senderID, targetChatID, sourceMessageID string) (*model.Message, error) {
    original, err := s.messageRepo.GetByID(ctx, sourceMessageID)
    if err != nil || original == nil {
        return nil, apperr.NewNotFound("source message not found")
    }

    forwardReq := &model.SendMessageRequest{
        ChatID:      targetChatID,
        Type:        original.Type,
        Payload:     original.Payload,
        ClientMsgID: uuid.New().String(), // New client_msg_id for the forward.
        ForwardedFrom: &model.ForwardedFrom{
            ChatID:    original.ChatID,
            MessageID: original.MessageID,
        },
    }

    return s.SendMessage(ctx, senderID, forwardReq)
}
```

#### h) gRPC Handler

```go
// message-service/internal/handler/grpc_handler.go
package handler

import (
    "context"

    "github.com/whatsapp-clone/backend/message-service/internal/model"
    "github.com/whatsapp-clone/backend/message-service/internal/service"
    messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"
    "google.golang.org/grpc/codes"
    "google.golang.org/grpc/status"
    "google.golang.org/protobuf/types/known/timestamppb"
)

type GRPCHandler struct {
    messagev1.UnimplementedMessageServiceServer
    msgSvc service.MessageService
}

func NewGRPCHandler(msgSvc service.MessageService) *GRPCHandler {
    return &GRPCHandler{msgSvc: msgSvc}
}

func (h *GRPCHandler) SendMessage(ctx context.Context, req *messagev1.SendMessageRequest) (*messagev1.SendMessageResponse, error) {
    sendReq := &model.SendMessageRequest{
        ChatID:           req.ChatId,
        Type:             model.MessageType(req.Type),
        ClientMsgID:      req.ClientMsgId,
        ReplyToMessageID: req.ReplyToMessageId,
        Payload: model.MessagePayload{
            Body:       req.Payload.Body,
            MediaID:    req.Payload.MediaId,
            Caption:    req.Payload.Caption,
            Filename:   req.Payload.Filename,
            DurationMs: req.Payload.DurationMs,
        },
    }
    if req.ForwardedFrom != nil {
        sendReq.ForwardedFrom = &model.ForwardedFrom{
            ChatID:    req.ForwardedFrom.ChatId,
            MessageID: req.ForwardedFrom.MessageId,
        }
    }

    msg, err := h.msgSvc.SendMessage(ctx, req.SenderId, sendReq)
    if err != nil {
        return nil, status.Error(codes.Internal, err.Error())
    }

    return &messagev1.SendMessageResponse{
        MessageId: msg.MessageID,
        CreatedAt: timestamppb.New(msg.CreatedAt),
    }, nil
}

func (h *GRPCHandler) UpdateMessageStatus(ctx context.Context, req *messagev1.UpdateMessageStatusRequest) (*messagev1.UpdateMessageStatusResponse, error) {
    err := h.msgSvc.UpdateStatus(ctx, req.MessageId, req.UserId, req.Status)
    if err != nil {
        return nil, status.Error(codes.Internal, err.Error())
    }
    return &messagev1.UpdateMessageStatusResponse{Success: true}, nil
}

func (h *GRPCHandler) GetLastMessages(ctx context.Context, req *messagev1.GetLastMessagesRequest) (*messagev1.GetLastMessagesResponse, error) {
    msgs, err := h.msgSvc.GetLastMessages(ctx, req.ChatIds)
    if err != nil {
        return nil, status.Error(codes.Internal, err.Error())
    }

    result := make(map[string]*messagev1.MessagePreview, len(msgs))
    for chatID, msg := range msgs {
        body := msg.Payload.Body
        if len(body) > 100 {
            body = body[:100]
        }
        if body == "" {
            body = "[" + string(msg.Type) + "]"
        }
        result[chatID] = &messagev1.MessagePreview{
            MessageId: msg.MessageID,
            SenderId:  msg.SenderID,
            Type:      string(msg.Type),
            Body:      body,
            CreatedAt: timestamppb.New(msg.CreatedAt),
        }
    }

    return &messagev1.GetLastMessagesResponse{Messages: result}, nil
}

func (h *GRPCHandler) GetUnreadCounts(ctx context.Context, req *messagev1.GetUnreadCountsRequest) (*messagev1.GetUnreadCountsResponse, error) {
    counts, err := h.msgSvc.GetUnreadCounts(ctx, req.UserId, req.ChatIds)
    if err != nil {
        return nil, status.Error(codes.Internal, err.Error())
    }
    return &messagev1.GetUnreadCountsResponse{Counts: counts}, nil
}
```

---

### 3.6 notification-service

**Port:** `:8085`

#### a) Configuration Struct

```go
// notification-service/config/config.go
package config

import "time"

type Config struct {
    HTTPPort            string        `env:"NOTIF_HTTP_PORT"             envDefault:":8085"`
    PostgresDSN         string        `env:"NOTIF_POSTGRES_DSN"          envRequired:"true"`
    RedisAddr           string        `env:"NOTIF_REDIS_ADDR"            envDefault:"redis:6379"`
    RedisPassword       string        `env:"NOTIF_REDIS_PASSWORD"        envDefault:""`
    NATSUrl             string        `env:"NOTIF_NATS_URL"              envDefault:"nats://nats:4222"`
    FCMCredentialsJSON  string        `env:"NOTIF_FCM_CREDENTIALS_JSON"  envRequired:"true"` // path to service account JSON
    GroupDebounceWindow time.Duration `env:"NOTIF_GROUP_DEBOUNCE"        envDefault:"3s"`
    MaxRetries          int           `env:"NOTIF_MAX_RETRIES"           envDefault:"3"`
    LogLevel            string        `env:"NOTIF_LOG_LEVEL"             envDefault:"info"`
}
```

#### b) Domain Models

```go
// notification-service/internal/model/notification.go
package model

type PushPayload struct {
    Type      string            `json:"type"`       // "message", "group_invite"
    ChatID    string            `json:"chat_id"`
    SenderID  string            `json:"sender_id"`
    Title     string            `json:"title"`
    Body      string            `json:"body"`
    Data      map[string]string `json:"data"`
}

type GroupNotifBuffer struct {
    ChatID    string
    Messages  []PushPayload
    Timer     *time.Timer
}
```

#### c) Repository Interfaces

```go
// notification-service/internal/repository/device_token_repository.go
package repository

import (
    "context"
)

type DeviceTokenRepository interface {
    // GetTokensByUserID returns all device tokens for a user.
    GetTokensByUserID(ctx context.Context, userID string) ([]string, error)

    // DeleteToken removes a stale token (e.g., after FCM 404).
    DeleteToken(ctx context.Context, token string) error
}
```

```go
// notification-service/internal/repository/mute_repository.go
package repository

import "context"

type MuteRepository interface {
    // IsMuted checks if a user has muted a chat.
    IsMuted(ctx context.Context, userID, chatID string) (bool, error)
}
```

```go
// notification-service/internal/repository/presence_repository.go
package repository

import "context"

type PresenceRepository interface {
    // IsOnline checks if the user is currently connected (has active presence key).
    IsOnline(ctx context.Context, userID string) (bool, error)
}
```

#### d) Service Interface

```go
// notification-service/internal/service/notification_service.go
package service

import "context"

type NotificationService interface {
    // HandleNewMessage processes a msg.new NATS event and sends push notifications
    // to all recipients who are offline and have not muted the chat.
    HandleNewMessage(ctx context.Context, chatID, senderID, msgType, body string, recipientIDs []string) error

    // HandleGroupMemberAdded sends a push notification to the added member.
    HandleGroupMemberAdded(ctx context.Context, chatID, userID, addedBy string) error

    // SendPush sends an FCM data-only push to all device tokens for a user.
    SendPush(ctx context.Context, userID string, payload map[string]string) error
}
```

#### e) Key Business Logic — NATS Consumer & Group Batching

```go
// notification-service/internal/service/consumer.go (excerpt)

// StartConsumers initializes NATS JetStream durable consumers for notification events.
func (s *notifServiceImpl) StartConsumers(ctx context.Context) error {
    js := s.js

    // Consumer for msg.new events.
    _, err := js.Subscribe("msg.new", func(natsMsg *nats.Msg) {
        var event struct {
            MessageID string   `json:"message_id"`
            ChatID    string   `json:"chat_id"`
            SenderID  string   `json:"sender_id"`
            Type      string   `json:"type"`
            Payload   struct {
                Body string `json:"body"`
            } `json:"payload"`
        }
        if err := json.Unmarshal(natsMsg.Data, &event); err != nil {
            s.log.Error().Err(err).Msg("failed to unmarshal msg.new event")
            natsMsg.Nak()
            return
        }

        // Fetch chat participants from the chat-service to know who to notify.
        // (In practice, the event payload should include participant IDs or the
        // notification-service queries the DB directly.)
        recipientIDs := s.getRecipientIDs(ctx, event.ChatID, event.SenderID)

        if err := s.HandleNewMessage(ctx, event.ChatID, event.SenderID, event.Type, event.Payload.Body, recipientIDs); err != nil {
            s.log.Error().Err(err).Msg("failed to handle msg.new notification")
            natsMsg.Nak()
            return
        }
        natsMsg.Ack()
    }, nats.Durable("notif-msg-consumer"), nats.ManualAck(), nats.AckWait(30*time.Second))

    if err != nil {
        return fmt.Errorf("failed to subscribe to msg.new: %w", err)
    }

    // Consumer for group.member.added events.
    _, err = js.Subscribe("group.member.added", func(natsMsg *nats.Msg) {
        var event struct {
            ChatID  string `json:"chat_id"`
            UserID  string `json:"user_id"`
            AddedBy string `json:"added_by"`
        }
        if err := json.Unmarshal(natsMsg.Data, &event); err != nil {
            natsMsg.Nak()
            return
        }
        if err := s.HandleGroupMemberAdded(ctx, event.ChatID, event.UserID, event.AddedBy); err != nil {
            natsMsg.Nak()
            return
        }
        natsMsg.Ack()
    }, nats.Durable("notif-group-consumer"), nats.ManualAck())

    if err != nil {
        return fmt.Errorf("failed to subscribe to group.member.added: %w", err)
    }

    return nil
}
```

```go
// HandleNewMessage checks presence and mute status, then batches group notifications.
func (s *notifServiceImpl) HandleNewMessage(ctx context.Context, chatID, senderID, msgType, body string, recipientIDs []string) error {
    for _, recipientID := range recipientIDs {
        if recipientID == senderID {
            continue
        }

        // Check if user is online (skip push if online — they get WebSocket delivery).
        online, err := s.presenceRepo.IsOnline(ctx, recipientID)
        if err == nil && online {
            continue
        }

        // Check if user has muted this chat.
        muted, err := s.muteRepo.IsMuted(ctx, recipientID, chatID)
        if err == nil && muted {
            continue
        }

        payload := map[string]string{
            "type":      "message",
            "chat_id":   chatID,
            "sender_id": senderID,
            "msg_type":  msgType,
            "body":      truncate(body, 200),
        }

        // For group chats, use debounce buffer to batch notifications.
        if s.isGroupChat(ctx, chatID) {
            s.bufferGroupNotification(recipientID, chatID, payload)
        } else {
            if err := s.SendPush(ctx, recipientID, payload); err != nil {
                s.log.Error().Err(err).Str("user_id", recipientID).Msg("failed to send push")
            }
        }
    }
    return nil
}

// bufferGroupNotification accumulates notifications per (user, chat) and flushes
// after the debounce window (3 seconds) with a summary.
func (s *notifServiceImpl) bufferGroupNotification(userID, chatID string, payload map[string]string) {
    key := userID + ":" + chatID

    s.bufferMu.Lock()
    defer s.bufferMu.Unlock()

    buf, exists := s.groupBuffers[key]
    if !exists {
        buf = &groupBuffer{payloads: []map[string]string{}}
        s.groupBuffers[key] = buf
    }

    buf.payloads = append(buf.payloads, payload)

    // Reset timer on each new message.
    if buf.timer != nil {
        buf.timer.Stop()
    }
    buf.timer = time.AfterFunc(s.debounceWindow, func() {
        s.flushGroupBuffer(userID, chatID, key)
    })
}

func (s *notifServiceImpl) flushGroupBuffer(userID, chatID, key string) {
    s.bufferMu.Lock()
    buf, exists := s.groupBuffers[key]
    if !exists {
        s.bufferMu.Unlock()
        return
    }
    delete(s.groupBuffers, key)
    s.bufferMu.Unlock()

    count := len(buf.payloads)
    summary := map[string]string{
        "type":    "message",
        "chat_id": chatID,
        "body":    fmt.Sprintf("%d new messages", count),
    }

    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()
    _ = s.SendPush(ctx, userID, summary)
}
```

#### f) Key Business Logic — FCM HTTP v1 API Call

```go
// notification-service/internal/service/fcm.go
package service

import (
    "bytes"
    "context"
    "encoding/json"
    "fmt"
    "io"
    "net/http"

    "golang.org/x/oauth2/google"
)

type fcmClient struct {
    httpClient *http.Client
    projectID  string
    tokenRepo  repository.DeviceTokenRepository
}

func NewFCMClient(credentialsJSON []byte, tokenRepo repository.DeviceTokenRepository) (*fcmClient, error) {
    cfg, err := google.JWTConfigFromJSON(credentialsJSON,
        "https://www.googleapis.com/auth/firebase.messaging",
    )
    if err != nil {
        return nil, fmt.Errorf("failed to parse FCM credentials: %w", err)
    }

    // Extract project_id from the credentials JSON.
    var creds struct {
        ProjectID string `json:"project_id"`
    }
    json.Unmarshal(credentialsJSON, &creds)

    return &fcmClient{
        httpClient: cfg.Client(context.Background()),
        projectID:  creds.ProjectID,
        tokenRepo:  tokenRepo,
    }, nil
}

// Send sends a data-only FCM message to a single device token.
func (f *fcmClient) Send(ctx context.Context, deviceToken string, data map[string]string) error {
    url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", f.projectID)

    payload := map[string]interface{}{
        "message": map[string]interface{}{
            "token": deviceToken,
            "data":  data,
        },
    }

    body, _ := json.Marshal(payload)
    req, _ := http.NewRequestWithContext(ctx, "POST", url, bytes.NewReader(body))
    req.Header.Set("Content-Type", "application/json")

    resp, err := f.httpClient.Do(req)
    if err != nil {
        return fmt.Errorf("FCM request failed: %w", err)
    }
    defer resp.Body.Close()

    if resp.StatusCode == http.StatusNotFound || resp.StatusCode == http.StatusGone {
        // Stale token — remove from database.
        _ = f.tokenRepo.DeleteToken(ctx, deviceToken)
        return fmt.Errorf("stale FCM token, removed")
    }

    if resp.StatusCode != http.StatusOK {
        respBody, _ := io.ReadAll(resp.Body)
        return fmt.Errorf("FCM error %d: %s", resp.StatusCode, string(respBody))
    }

    return nil
}
```

---

### 3.7 media-service

**Ports:** `:8086` HTTP, `:9086` gRPC

#### a) Configuration Struct

```go
// media-service/config/config.go
package config

import "time"

type Config struct {
    HTTPPort          string        `env:"MEDIA_HTTP_PORT"          envDefault:":8086"`
    GRPCPort          string        `env:"MEDIA_GRPC_PORT"         envDefault:":9086"`
    MongoURI          string        `env:"MEDIA_MONGO_URI"         envRequired:"true"`
    MongoDB           string        `env:"MEDIA_MONGO_DB"          envDefault:"whatsapp"`
    MinIOEndpoint     string        `env:"MEDIA_MINIO_ENDPOINT"    envDefault:"minio:9000"`
    MinIOAccessKey    string        `env:"MEDIA_MINIO_ACCESS_KEY"  envRequired:"true"`
    MinIOSecretKey    string        `env:"MEDIA_MINIO_SECRET_KEY"  envRequired:"true"`
    MinIOBucket       string        `env:"MEDIA_MINIO_BUCKET"      envDefault:"media"`
    MinIOUseSSL       bool          `env:"MEDIA_MINIO_USE_SSL"     envDefault:"false"`
    PresignedURLTTL   time.Duration `env:"MEDIA_PRESIGNED_TTL"     envDefault:"1h"`
    FFmpegPath        string        `env:"MEDIA_FFMPEG_PATH"       envDefault:"/usr/bin/ffmpeg"`
    MaxImageSize      int64         `env:"MEDIA_MAX_IMAGE_SIZE"    envDefault:"16777216"`   // 16MB
    MaxVideoSize      int64         `env:"MEDIA_MAX_VIDEO_SIZE"    envDefault:"67108864"`   // 64MB
    MaxAudioSize      int64         `env:"MEDIA_MAX_AUDIO_SIZE"    envDefault:"16777216"`   // 16MB
    MaxDocSize        int64         `env:"MEDIA_MAX_DOC_SIZE"      envDefault:"104857600"`  // 100MB
    CleanupInterval   time.Duration `env:"MEDIA_CLEANUP_INTERVAL"  envDefault:"1h"`
    LogLevel          string        `env:"MEDIA_LOG_LEVEL"         envDefault:"info"`
}
```

#### b) Domain Models

```go
// media-service/internal/model/media.go
package model

import "time"

type Media struct {
    MediaID          string    `json:"media_id"           bson:"media_id"`
    UploaderID       string    `json:"uploader_id"        bson:"uploader_id"`
    FileType         string    `json:"file_type"          bson:"file_type"` // "image", "video", "audio", "document"
    MimeType         string    `json:"mime_type"          bson:"mime_type"`
    OriginalFilename string    `json:"original_filename"  bson:"original_filename"`
    SizeBytes        int64     `json:"size_bytes"         bson:"size_bytes"`
    Width            int       `json:"width"              bson:"width"`
    Height           int       `json:"height"             bson:"height"`
    DurationMs       int64     `json:"duration_ms"        bson:"duration_ms"`
    ChecksumSHA256   string    `json:"checksum_sha256"    bson:"checksum_sha256"`
    StorageKey       string    `json:"storage_key"        bson:"storage_key"`
    ThumbnailKey     string    `json:"thumbnail_key"      bson:"thumbnail_key"`
    CreatedAt        time.Time `json:"created_at"         bson:"created_at"`
}

type UploadResponse struct {
    MediaID      string `json:"media_id"`
    FileType     string `json:"file_type"`
    URL          string `json:"url"`
    ThumbnailURL string `json:"thumbnail_url,omitempty"`
}
```

#### c) Repository Interface

```go
// media-service/internal/repository/media_repository.go
package repository

import (
    "context"

    "github.com/whatsapp-clone/backend/media-service/internal/model"
)

type MediaRepository interface {
    Insert(ctx context.Context, media *model.Media) error
    GetByID(ctx context.Context, mediaID string) (*model.Media, error)
    Delete(ctx context.Context, mediaID string) error
    // FindOrphaned returns media IDs not referenced by any message, older than the given age.
    FindOrphaned(ctx context.Context, olderThan time.Time) ([]*model.Media, error)
}
```

```go
// media-service/internal/repository/storage_repository.go
package repository

import (
    "context"
    "io"
    "time"
)

type StorageRepository interface {
    Upload(ctx context.Context, key string, reader io.Reader, size int64, contentType string) error
    Delete(ctx context.Context, key string) error
    PresignedURL(ctx context.Context, key string, expiry time.Duration) (string, error)
}
```

#### d) Service Interface

```go
// media-service/internal/service/media_service.go
package service

import (
    "context"
    "mime/multipart"

    "github.com/whatsapp-clone/backend/media-service/internal/model"
)

type MediaService interface {
    Upload(ctx context.Context, uploaderID string, file *multipart.FileHeader) (*model.UploadResponse, error)
    GetMetadata(ctx context.Context, mediaID string) (*model.Media, string, string, error) // media, url, thumbnailURL, error
    StartCleanupJob(ctx context.Context)
}
```

#### e) Key Business Logic — Upload with MIME Validation & Thumbnail

```go
// media-service/internal/service/media_service_impl.go (excerpt)

func (s *mediaServiceImpl) Upload(ctx context.Context, uploaderID string, fh *multipart.FileHeader) (*model.UploadResponse, error) {
    file, err := fh.Open()
    if err != nil {
        return nil, apperr.NewBadRequest("failed to open uploaded file")
    }
    defer file.Close()

    // Read first 512 bytes for MIME type detection via magic bytes.
    buf := make([]byte, 512)
    n, err := file.Read(buf)
    if err != nil {
        return nil, apperr.NewBadRequest("failed to read file")
    }
    mimeType := http.DetectContentType(buf[:n])

    // Reset reader position.
    file.Seek(0, io.SeekStart)

    // Determine file type category and validate size.
    fileType, maxSize := s.classifyMIME(mimeType)
    if fileType == "" {
        return nil, &apperr.AppError{Code: apperr.CodeMediaInvalidType, Message: "unsupported file type: " + mimeType, HTTPStatus: 400}
    }
    if fh.Size > maxSize {
        return nil, &apperr.AppError{Code: apperr.CodeMediaTooLarge, Message: fmt.Sprintf("file too large, max %d bytes", maxSize), HTTPStatus: 413}
    }

    // Compute SHA-256 checksum.
    hasher := sha256.New()
    teeReader := io.TeeReader(file, hasher)

    mediaID := uuid.New().String()
    storageKey := fmt.Sprintf("%s/%s/%s", fileType, time.Now().Format("2006/01/02"), mediaID)

    // Upload original to MinIO.
    if err := s.storageRepo.Upload(ctx, storageKey, teeReader, fh.Size, mimeType); err != nil {
        return nil, apperr.NewInternal("failed to upload to storage", err)
    }
    checksum := hex.EncodeToString(hasher.Sum(nil))

    // Generate thumbnail for images and videos.
    var thumbnailKey string
    if fileType == "image" || fileType == "video" {
        thumbKey, err := s.generateThumbnail(ctx, storageKey, fileType, mediaID)
        if err != nil {
            s.log.Warn().Err(err).Msg("thumbnail generation failed, continuing without")
        } else {
            thumbnailKey = thumbKey
        }
    }

    media := &model.Media{
        MediaID:          mediaID,
        UploaderID:       uploaderID,
        FileType:         fileType,
        MimeType:         mimeType,
        OriginalFilename: fh.Filename,
        SizeBytes:        fh.Size,
        ChecksumSHA256:   checksum,
        StorageKey:       storageKey,
        ThumbnailKey:     thumbnailKey,
        CreatedAt:        time.Now(),
    }

    if err := s.mediaRepo.Insert(ctx, media); err != nil {
        return nil, apperr.NewInternal("failed to save media metadata", err)
    }

    // Generate presigned URLs.
    downloadURL, _ := s.storageRepo.PresignedURL(ctx, storageKey, s.presignedTTL)
    var thumbURL string
    if thumbnailKey != "" {
        thumbURL, _ = s.storageRepo.PresignedURL(ctx, thumbnailKey, s.presignedTTL)
    }

    return &model.UploadResponse{
        MediaID:      mediaID,
        FileType:     fileType,
        URL:          downloadURL,
        ThumbnailURL: thumbURL,
    }, nil
}

// classifyMIME returns the file type category and max allowed size.
func (s *mediaServiceImpl) classifyMIME(mimeType string) (string, int64) {
    switch {
    case strings.HasPrefix(mimeType, "image/"):
        return "image", s.cfg.MaxImageSize
    case strings.HasPrefix(mimeType, "video/"):
        return "video", s.cfg.MaxVideoSize
    case strings.HasPrefix(mimeType, "audio/"):
        return "audio", s.cfg.MaxAudioSize
    case mimeType == "application/pdf",
         mimeType == "application/msword",
         mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
         mimeType == "application/vnd.ms-excel",
         mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
         mimeType == "text/plain",
         mimeType == "application/zip":
        return "document", s.cfg.MaxDocSize
    default:
        return "", 0
    }
}
```

#### f) Thumbnail Generation via FFmpeg

```go
// media-service/internal/service/thumbnail.go
package service

import (
    "bytes"
    "context"
    "fmt"
    "os"
    "os/exec"
    "path/filepath"
)

// generateThumbnail creates a 200px-wide thumbnail. For images, it resizes directly.
// For videos, it extracts the first frame.
func (s *mediaServiceImpl) generateThumbnail(ctx context.Context, storageKey, fileType, mediaID string) (string, error) {
    // Download original to a temp file.
    tmpDir := os.TempDir()
    originalPath := filepath.Join(tmpDir, "orig-"+mediaID)
    thumbPath := filepath.Join(tmpDir, "thumb-"+mediaID+".jpg")
    defer os.Remove(originalPath)
    defer os.Remove(thumbPath)

    // Download from MinIO to temp file.
    url, _ := s.storageRepo.PresignedURL(ctx, storageKey, 5*time.Minute)
    if err := downloadFile(url, originalPath); err != nil {
        return "", err
    }

    var cmd *exec.Cmd
    switch fileType {
    case "image":
        // Resize image to 200px width, maintain aspect ratio.
        cmd = exec.CommandContext(ctx, s.cfg.FFmpegPath,
            "-i", originalPath,
            "-vf", "scale=200:-1",
            "-frames:v", "1",
            "-y", thumbPath,
        )
    case "video":
        // Extract first frame at 200px width.
        cmd = exec.CommandContext(ctx, s.cfg.FFmpegPath,
            "-i", originalPath,
            "-vf", "scale=200:-1",
            "-frames:v", "1",
            "-y", thumbPath,
        )
    default:
        return "", fmt.Errorf("thumbnail not supported for type: %s", fileType)
    }

    var stderr bytes.Buffer
    cmd.Stderr = &stderr
    if err := cmd.Run(); err != nil {
        return "", fmt.Errorf("ffmpeg failed: %s: %w", stderr.String(), err)
    }

    // Upload thumbnail to MinIO.
    thumbKey := "thumbnails/" + mediaID + ".jpg"
    thumbFile, err := os.Open(thumbPath)
    if err != nil {
        return "", err
    }
    defer thumbFile.Close()

    stat, _ := thumbFile.Stat()
    if err := s.storageRepo.Upload(ctx, thumbKey, thumbFile, stat.Size(), "image/jpeg"); err != nil {
        return "", err
    }

    return thumbKey, nil
}
```

#### g) Background Cleanup Job

```go
// media-service/internal/service/cleanup.go
package service

import (
    "context"
    "time"
)

// StartCleanupJob runs a periodic goroutine that finds and removes orphaned media
// (media not referenced by any message, older than 24 hours).
func (s *mediaServiceImpl) StartCleanupJob(ctx context.Context) {
    ticker := time.NewTicker(s.cfg.CleanupInterval)
    go func() {
        for {
            select {
            case <-ctx.Done():
                ticker.Stop()
                return
            case <-ticker.C:
                s.log.Info().Msg("starting orphaned media cleanup")
                cutoff := time.Now().Add(-24 * time.Hour)
                orphaned, err := s.mediaRepo.FindOrphaned(ctx, cutoff)
                if err != nil {
                    s.log.Error().Err(err).Msg("failed to find orphaned media")
                    continue
                }
                for _, m := range orphaned {
                    if err := s.storageRepo.Delete(ctx, m.StorageKey); err != nil {
                        s.log.Error().Err(err).Str("key", m.StorageKey).Msg("failed to delete from storage")
                    }
                    if m.ThumbnailKey != "" {
                        _ = s.storageRepo.Delete(ctx, m.ThumbnailKey)
                    }
                    _ = s.mediaRepo.Delete(ctx, m.MediaID)
                }
                s.log.Info().Int("count", len(orphaned)).Msg("orphaned media cleanup complete")
            }
        }
    }()
}
```

#### h) gRPC Handler

```go
// media-service/internal/handler/grpc_handler.go
package handler

import (
    "context"

    "github.com/whatsapp-clone/backend/media-service/internal/service"
    mediav1 "github.com/whatsapp-clone/backend/proto/media/v1"
    "google.golang.org/grpc/codes"
    "google.golang.org/grpc/status"
)

type GRPCHandler struct {
    mediav1.UnimplementedMediaServiceServer
    mediaSvc service.MediaService
}

func NewGRPCHandler(mediaSvc service.MediaService) *GRPCHandler {
    return &GRPCHandler{mediaSvc: mediaSvc}
}

func (h *GRPCHandler) GetMediaMetadata(ctx context.Context, req *mediav1.GetMediaMetadataRequest) (*mediav1.GetMediaMetadataResponse, error) {
    media, url, thumbURL, err := h.mediaSvc.GetMetadata(ctx, req.MediaId)
    if err != nil {
        return nil, status.Error(codes.NotFound, err.Error())
    }

    return &mediav1.GetMediaMetadataResponse{
        MediaId:      media.MediaID,
        FileType:     media.FileType,
        MimeType:     media.MimeType,
        SizeBytes:    media.SizeBytes,
        Url:          url,
        ThumbnailUrl: thumbURL,
        Width:        int32(media.Width),
        Height:       int32(media.Height),
        DurationMs:   media.DurationMs,
    }, nil
}
```

---

### 3.8 websocket-service

**Port:** `:8087`

#### a) Configuration Struct

```go
// websocket-service/config/config.go
package config

import "time"

type Config struct {
    Port              string        `env:"WS_PORT"              envDefault:":8087"`
    AuthGRPCAddr      string        `env:"WS_AUTH_GRPC_ADDR"    envDefault:"auth-service:9081"`
    RedisAddr         string        `env:"WS_REDIS_ADDR"        envDefault:"redis:6379"`
    RedisPassword     string        `env:"WS_REDIS_PASSWORD"    envDefault:""`
    NATSUrl           string        `env:"WS_NATS_URL"          envDefault:"nats://nats:4222"`
    PingInterval      time.Duration `env:"WS_PING_INTERVAL"     envDefault:"25s"`
    PongTimeout       time.Duration `env:"WS_PONG_TIMEOUT"      envDefault:"60s"`
    WriteTimeout      time.Duration `env:"WS_WRITE_TIMEOUT"     envDefault:"10s"`
    MaxMessageSize    int64         `env:"WS_MAX_MSG_SIZE"      envDefault:"65536"` // 64KB
    PresenceTTL       time.Duration `env:"WS_PRESENCE_TTL"      envDefault:"60s"`
    TypingTTL         time.Duration `env:"WS_TYPING_TTL"        envDefault:"5s"`
    LogLevel          string        `env:"WS_LOG_LEVEL"         envDefault:"info"`
}
```

#### b) Domain Models

```go
// websocket-service/internal/model/event.go
package model

import "encoding/json"

// WSEvent represents a WebSocket message envelope (both client->server and server->client).
type WSEvent struct {
    Type    string          `json:"type"`
    Payload json.RawMessage `json:"payload"`
}

// Client->Server event payloads.

type MessageSendPayload struct {
    ChatID           string         `json:"chat_id"`
    Type             string         `json:"type"`
    Payload          MessageContent `json:"payload"`
    ClientMsgID      string         `json:"client_msg_id"`
    ReplyToMessageID string         `json:"reply_to_message_id,omitempty"`
}

type MessageContent struct {
    Body       string `json:"body,omitempty"`
    MediaID    string `json:"media_id,omitempty"`
    Caption    string `json:"caption,omitempty"`
    Filename   string `json:"filename,omitempty"`
    DurationMs int64  `json:"duration_ms,omitempty"`
}

type MessageStatusPayload struct {
    MessageID string `json:"message_id"`
    Status    string `json:"status"` // "delivered" or "read"
}

type MessageDeletePayload struct {
    MessageID string `json:"message_id"`
}

type TypingPayload struct {
    ChatID string `json:"chat_id"`
}

type PresenceSubscribePayload struct {
    UserIDs []string `json:"user_ids"`
}

// Server->Client event payloads.

type MessageNewPayload struct {
    MessageID   string         `json:"message_id"`
    ChatID      string         `json:"chat_id"`
    SenderID    string         `json:"sender_id"`
    Type        string         `json:"type"`
    Payload     MessageContent `json:"payload"`
    CreatedAt   int64          `json:"created_at"`
}

type MessageSentAckPayload struct {
    ClientMsgID string `json:"client_msg_id"`
    MessageID   string `json:"message_id"`
    CreatedAt   int64  `json:"created_at"`
}

type PresenceEventPayload struct {
    UserID string `json:"user_id"`
    Online bool   `json:"online"`
}
```

```go
// websocket-service/internal/model/client.go
package model

import (
    "sync"
    "time"

    "github.com/gorilla/websocket"
)

// Client represents a single WebSocket connection.
type Client struct {
    Conn     *websocket.Conn
    UserID   string
    Send     chan []byte
    JoinedAt time.Time
}

// Hub maintains the set of active clients and routes messages.
type Hub struct {
    mu       sync.RWMutex
    clients  map[string][]*Client // userID -> list of connections (multi-device)
}

func NewHub() *Hub {
    return &Hub{clients: make(map[string][]*Client)}
}

func (h *Hub) Register(client *Client) {
    h.mu.Lock()
    defer h.mu.Unlock()
    h.clients[client.UserID] = append(h.clients[client.UserID], client)
}

func (h *Hub) Unregister(client *Client) {
    h.mu.Lock()
    defer h.mu.Unlock()
    conns := h.clients[client.UserID]
    for i, c := range conns {
        if c == client {
            h.clients[client.UserID] = append(conns[:i], conns[i+1:]...)
            break
        }
    }
    if len(h.clients[client.UserID]) == 0 {
        delete(h.clients, client.UserID)
    }
}

func (h *Hub) GetClients(userID string) []*Client {
    h.mu.RLock()
    defer h.mu.RUnlock()
    return h.clients[userID]
}

func (h *Hub) IsConnected(userID string) bool {
    h.mu.RLock()
    defer h.mu.RUnlock()
    return len(h.clients[userID]) > 0
}

func (h *Hub) AllUserIDs() []string {
    h.mu.RLock()
    defer h.mu.RUnlock()
    ids := make([]string, 0, len(h.clients))
    for id := range h.clients {
        ids = append(ids, id)
    }
    return ids
}
```

#### c) Service Interface

```go
// websocket-service/internal/service/ws_service.go
package service

import (
    "context"

    "github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

type WebSocketService interface {
    // HandleEvent processes a client->server WebSocket event.
    HandleEvent(ctx context.Context, client *model.Client, event *model.WSEvent) error

    // SendToUser sends an event to all connected clients for a user ID.
    SendToUser(userID string, event *model.WSEvent) error

    // SetPresence updates the user's online presence in Redis.
    SetPresence(ctx context.Context, userID string, online bool) error

    // StartNATSConsumers starts consuming events from NATS for real-time delivery.
    StartNATSConsumers(ctx context.Context) error

    // StartRedisSubscriber starts a Redis pub-sub subscriber for the given user.
    StartRedisSubscriber(ctx context.Context, client *model.Client) error

    // StopRedisSubscriber stops the Redis pub-sub subscriber for the client.
    StopRedisSubscriber(client *model.Client) error
}
```

#### d) Read/Write Pump Pattern

```go
// websocket-service/internal/handler/ws_handler.go
package handler

import (
    "context"
    "encoding/json"
    "net/http"
    "time"

    "github.com/gorilla/websocket"
    "github.com/rs/zerolog"

    "github.com/whatsapp-clone/backend/websocket-service/internal/model"
    "github.com/whatsapp-clone/backend/websocket-service/internal/service"
)

var upgrader = websocket.Upgrader{
    ReadBufferSize:  1024,
    WriteBufferSize: 1024,
    CheckOrigin:     func(r *http.Request) bool { return true },
}

type WSHandler struct {
    hub     *model.Hub
    wsSvc   service.WebSocketService
    authVal service.AuthValidator
    cfg     *config.Config
    log     zerolog.Logger
}

func NewWSHandler(hub *model.Hub, wsSvc service.WebSocketService, authVal service.AuthValidator, cfg *config.Config, log zerolog.Logger) *WSHandler {
    return &WSHandler{hub: hub, wsSvc: wsSvc, authVal: authVal, cfg: cfg, log: log}
}

// ServeWS handles the WebSocket upgrade and starts the read/write pumps.
func (h *WSHandler) ServeWS(w http.ResponseWriter, r *http.Request) {
    // Validate JWT from query parameter or Authorization header.
    token := r.URL.Query().Get("token")
    if token == "" {
        token = r.Header.Get("Authorization")
        if len(token) > 7 {
            token = token[7:] // strip "Bearer "
        }
    }

    userID, _, err := h.authVal.ValidateToken(r.Context(), token)
    if err != nil {
        http.Error(w, "unauthorized", http.StatusUnauthorized)
        return
    }

    conn, err := upgrader.Upgrade(w, r, nil)
    if err != nil {
        h.log.Error().Err(err).Msg("websocket upgrade failed")
        return
    }

    client := &model.Client{
        Conn:     conn,
        UserID:   userID,
        Send:     make(chan []byte, 256),
        JoinedAt: time.Now(),
    }

    h.hub.Register(client)

    // Set presence to online.
    ctx := context.Background()
    _ = h.wsSvc.SetPresence(ctx, userID, true)

    // Start Redis pub-sub for this user's channel.
    _ = h.wsSvc.StartRedisSubscriber(ctx, client)

    // Start read and write pumps.
    go h.writePump(client)
    go h.readPump(client)
}

// readPump reads messages from the WebSocket connection.
func (h *WSHandler) readPump(client *model.Client) {
    defer func() {
        h.hub.Unregister(client)
        h.wsSvc.StopRedisSubscriber(client)

        // Set offline if no more connections for this user.
        if !h.hub.IsConnected(client.UserID) {
            _ = h.wsSvc.SetPresence(context.Background(), client.UserID, false)
        }

        client.Conn.Close()
        close(client.Send)
    }()

    client.Conn.SetReadLimit(h.cfg.MaxMessageSize)
    client.Conn.SetReadDeadline(time.Now().Add(h.cfg.PongTimeout))
    client.Conn.SetPongHandler(func(string) error {
        client.Conn.SetReadDeadline(time.Now().Add(h.cfg.PongTimeout))
        return nil
    })

    for {
        _, message, err := client.Conn.ReadMessage()
        if err != nil {
            if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseNormalClosure) {
                h.log.Warn().Err(err).Str("user_id", client.UserID).Msg("websocket read error")
            }
            return
        }

        var event model.WSEvent
        if err := json.Unmarshal(message, &event); err != nil {
            h.sendError(client, "invalid event format")
            continue
        }

        // Route the event to the service layer.
        if err := h.wsSvc.HandleEvent(context.Background(), client, &event); err != nil {
            h.log.Error().Err(err).Str("type", event.Type).Msg("event handling failed")
            h.sendError(client, err.Error())
        }
    }
}

// writePump writes messages to the WebSocket connection.
func (h *WSHandler) writePump(client *model.Client) {
    ticker := time.NewTicker(h.cfg.PingInterval)
    defer func() {
        ticker.Stop()
        client.Conn.Close()
    }()

    for {
        select {
        case message, ok := <-client.Send:
            client.Conn.SetWriteDeadline(time.Now().Add(h.cfg.WriteTimeout))
            if !ok {
                client.Conn.WriteMessage(websocket.CloseMessage, []byte{})
                return
            }
            if err := client.Conn.WriteMessage(websocket.TextMessage, message); err != nil {
                return
            }
        case <-ticker.C:
            client.Conn.SetWriteDeadline(time.Now().Add(h.cfg.WriteTimeout))
            if err := client.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
                return
            }
        }
    }
}

func (h *WSHandler) sendError(client *model.Client, msg string) {
    event := model.WSEvent{Type: "error"}
    event.Payload, _ = json.Marshal(map[string]string{"message": msg})
    data, _ := json.Marshal(event)
    select {
    case client.Send <- data:
    default:
    }
}
```

#### e) Event Router

```go
// websocket-service/internal/service/event_router.go
package service

import (
    "context"
    "encoding/json"
    "fmt"

    "github.com/whatsapp-clone/backend/websocket-service/internal/model"
    messagev1 "github.com/whatsapp-clone/backend/proto/message/v1"
)

// HandleEvent routes a client->server event to the appropriate handler.
func (s *wsServiceImpl) HandleEvent(ctx context.Context, client *model.Client, event *model.WSEvent) error {
    switch event.Type {
    case "message.send":
        return s.handleMessageSend(ctx, client, event.Payload)
    case "message.delivered":
        return s.handleMessageStatus(ctx, client, event.Payload, "delivered")
    case "message.read":
        return s.handleMessageStatus(ctx, client, event.Payload, "read")
    case "message.delete":
        return s.handleMessageDelete(ctx, client, event.Payload)
    case "typing.start":
        return s.handleTyping(ctx, client, event.Payload, true)
    case "typing.stop":
        return s.handleTyping(ctx, client, event.Payload, false)
    case "presence.subscribe":
        return s.handlePresenceSubscribe(ctx, client, event.Payload)
    case "ping":
        return s.handlePing(client)
    default:
        return fmt.Errorf("unknown event type: %s", event.Type)
    }
}

func (s *wsServiceImpl) handleMessageSend(ctx context.Context, client *model.Client, payload json.RawMessage) error {
    var p model.MessageSendPayload
    if err := json.Unmarshal(payload, &p); err != nil {
        return fmt.Errorf("invalid message.send payload: %w", err)
    }

    // Send message via gRPC to message-service.
    resp, err := s.messageClient.SendMessage(ctx, &messagev1.SendMessageRequest{
        ChatId:           p.ChatID,
        SenderId:         client.UserID,
        Type:             p.Type,
        ClientMsgId:      p.ClientMsgID,
        ReplyToMessageId: p.ReplyToMessageID,
        Payload: &messagev1.MessagePayload{
            Body:       p.Payload.Body,
            MediaId:    p.Payload.MediaID,
            Caption:    p.Payload.Caption,
            Filename:   p.Payload.Filename,
            DurationMs: p.Payload.DurationMs,
        },
    })
    if err != nil {
        return fmt.Errorf("message-service SendMessage failed: %w", err)
    }

    // Send ack back to sender.
    ack := model.WSEvent{Type: "message.sent"}
    ack.Payload, _ = json.Marshal(model.MessageSentAckPayload{
        ClientMsgID: p.ClientMsgID,
        MessageID:   resp.MessageId,
        CreatedAt:   resp.CreatedAt.AsTime().UnixMilli(),
    })
    return s.SendToUser(client.UserID, &ack)
}

func (s *wsServiceImpl) handleMessageStatus(ctx context.Context, client *model.Client, payload json.RawMessage, statusVal string) error {
    var p model.MessageStatusPayload
    if err := json.Unmarshal(payload, &p); err != nil {
        return fmt.Errorf("invalid status payload: %w", err)
    }

    _, err := s.messageClient.UpdateMessageStatus(ctx, &messagev1.UpdateMessageStatusRequest{
        MessageId: p.MessageID,
        UserId:    client.UserID,
        Status:    statusVal,
    })
    return err
}

func (s *wsServiceImpl) handleTyping(ctx context.Context, client *model.Client, payload json.RawMessage, start bool) error {
    var p model.TypingPayload
    if err := json.Unmarshal(payload, &p); err != nil {
        return fmt.Errorf("invalid typing payload: %w", err)
    }

    if start {
        key := fmt.Sprintf("typing:%s:%s", p.ChatID, client.UserID)
        s.rdb.SetEx(ctx, key, "1", s.cfg.TypingTTL)
    } else {
        key := fmt.Sprintf("typing:%s:%s", p.ChatID, client.UserID)
        s.rdb.Del(ctx, key)
    }

    // Publish to Redis channel for the chat so other ws instances pick it up.
    event := model.WSEvent{Type: "typing"}
    event.Payload, _ = json.Marshal(map[string]interface{}{
        "chat_id": p.ChatID,
        "user_id": client.UserID,
        "typing":  start,
    })
    data, _ := json.Marshal(event)
    s.rdb.Publish(ctx, fmt.Sprintf("typing:%s", p.ChatID), data)

    return nil
}

func (s *wsServiceImpl) handlePing(client *model.Client) error {
    pong := model.WSEvent{Type: "pong"}
    pong.Payload, _ = json.Marshal(map[string]int64{"timestamp": time.Now().UnixMilli()})
    return s.SendToUser(client.UserID, &pong)
}

func (s *wsServiceImpl) handlePresenceSubscribe(ctx context.Context, client *model.Client, payload json.RawMessage) error {
    var p model.PresenceSubscribePayload
    if err := json.Unmarshal(payload, &p); err != nil {
        return err
    }

    // Send current presence status for each requested user.
    for _, uid := range p.UserIDs {
        online := s.hub.IsConnected(uid)
        event := model.WSEvent{Type: "presence"}
        event.Payload, _ = json.Marshal(model.PresenceEventPayload{
            UserID: uid,
            Online: online,
        })
        s.SendToUser(client.UserID, &event)
    }
    return nil
}
```

#### f) NATS Consumer for Real-Time Delivery

```go
// websocket-service/internal/service/nats_consumer.go
package service

import (
    "context"
    "encoding/json"
    "time"

    "github.com/nats-io/nats.go"
    "github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

func (s *wsServiceImpl) StartNATSConsumers(ctx context.Context) error {
    // msg.new → deliver to all recipients connected to this instance.
    _, err := s.js.Subscribe("msg.new", func(m *nats.Msg) {
        var event struct {
            MessageID string `json:"message_id"`
            ChatID    string `json:"chat_id"`
            SenderID  string `json:"sender_id"`
            Type      string `json:"type"`
            Payload   struct {
                Body       string `json:"body"`
                MediaID    string `json:"media_id"`
                Caption    string `json:"caption"`
                Filename   string `json:"filename"`
                DurationMs int64  `json:"duration_ms"`
            } `json:"payload"`
            CreatedAt time.Time `json:"created_at"`
        }
        if err := json.Unmarshal(m.Data, &event); err != nil {
            m.Nak()
            return
        }

        // Route to connected recipients via Redis pub-sub (cross-instance).
        wsEvent := model.WSEvent{Type: "message.new"}
        wsEvent.Payload, _ = json.Marshal(model.MessageNewPayload{
            MessageID: event.MessageID,
            ChatID:    event.ChatID,
            SenderID:  event.SenderID,
            Type:      event.Type,
            Payload: model.MessageContent{
                Body:       event.Payload.Body,
                MediaID:    event.Payload.MediaID,
                Caption:    event.Payload.Caption,
                Filename:   event.Payload.Filename,
                DurationMs: event.Payload.DurationMs,
            },
            CreatedAt: event.CreatedAt.UnixMilli(),
        })

        data, _ := json.Marshal(wsEvent)
        // Publish to each participant's Redis pub-sub channel.
        participantIDs := s.getChatParticipants(ctx, event.ChatID)
        for _, uid := range participantIDs {
            s.rdb.Publish(ctx, "user:channel:"+uid, data)
        }

        m.Ack()
    }, nats.Durable("ws-msg-consumer"), nats.ManualAck())

    if err != nil {
        return err
    }

    // msg.status.updated → notify sender about delivery/read receipts.
    _, err = s.js.Subscribe("msg.status.updated", func(m *nats.Msg) {
        // Parse and route to the message sender's channel.
        var event struct {
            MessageID string `json:"message_id"`
            UserID    string `json:"user_id"` // who updated the status
            SenderID  string `json:"sender_id"` // original message sender
            Status    string `json:"status"`
        }
        if err := json.Unmarshal(m.Data, &event); err != nil {
            m.Nak()
            return
        }

        wsEvent := model.WSEvent{Type: "message.status"}
        wsEvent.Payload, _ = json.Marshal(map[string]string{
            "message_id": event.MessageID,
            "user_id":    event.UserID,
            "status":     event.Status,
        })
        data, _ := json.Marshal(wsEvent)
        s.rdb.Publish(context.Background(), "user:channel:"+event.SenderID, data)
        m.Ack()
    }, nats.Durable("ws-status-consumer"), nats.ManualAck())

    if err != nil {
        return err
    }

    // Subscribe to chat.* and group.* events for real-time group updates.
    subjects := []string{"chat.created", "chat.updated", "group.member.added", "group.member.removed"}
    for _, subj := range subjects {
        subject := subj // capture
        _, err = s.js.Subscribe(subject, func(m *nats.Msg) {
            var event map[string]interface{}
            if err := json.Unmarshal(m.Data, &event); err != nil {
                m.Nak()
                return
            }

            wsEvent := model.WSEvent{Type: subject}
            wsEvent.Payload = m.Data

            // Route to affected participants.
            if members, ok := event["participants"].([]interface{}); ok {
                data, _ := json.Marshal(wsEvent)
                for _, mid := range members {
                    if uid, ok := mid.(string); ok {
                        s.rdb.Publish(context.Background(), "user:channel:"+uid, data)
                    }
                }
            }
            m.Ack()
        }, nats.Durable("ws-"+subject+"-consumer"), nats.ManualAck())

        if err != nil {
            return err
        }
    }

    return nil
}
```

#### g) Redis Pub-Sub Subscriber Per Client

```go
// websocket-service/internal/service/redis_subscriber.go
package service

import (
    "context"
    "sync"

    "github.com/redis/go-redis/v9"
    "github.com/whatsapp-clone/backend/websocket-service/internal/model"
)

type subscriberRegistry struct {
    mu   sync.Mutex
    subs map[*model.Client]*redis.PubSub
}

func newSubscriberRegistry() *subscriberRegistry {
    return &subscriberRegistry{subs: make(map[*model.Client]*redis.PubSub)}
}

func (s *wsServiceImpl) StartRedisSubscriber(ctx context.Context, client *model.Client) error {
    channel := "user:channel:" + client.UserID
    pubsub := s.rdb.Subscribe(ctx, channel)

    s.subRegistry.mu.Lock()
    s.subRegistry.subs[client] = pubsub
    s.subRegistry.mu.Unlock()

    go func() {
        ch := pubsub.Channel()
        for msg := range ch {
            select {
            case client.Send <- []byte(msg.Payload):
            default:
                // Client send buffer full — drop message.
            }
        }
    }()

    return nil
}

func (s *wsServiceImpl) StopRedisSubscriber(client *model.Client) error {
    s.subRegistry.mu.Lock()
    pubsub, ok := s.subRegistry.subs[client]
    if ok {
        delete(s.subRegistry.subs, client)
    }
    s.subRegistry.mu.Unlock()

    if pubsub != nil {
        return pubsub.Close()
    }
    return nil
}
```

#### h) Graceful Shutdown

```go
// websocket-service/cmd/main.go (excerpt — shutdown)

func (s *wsServiceImpl) GracefulShutdown() {
    // Close all WebSocket connections.
    for _, uid := range s.hub.AllUserIDs() {
        for _, client := range s.hub.GetClients(uid) {
            client.Conn.WriteMessage(websocket.CloseMessage,
                websocket.FormatCloseMessage(websocket.CloseGoingAway, "server shutting down"))
            client.Conn.Close()
        }
    }

    // Close all Redis pub-sub subscriptions.
    s.subRegistry.mu.Lock()
    for _, pubsub := range s.subRegistry.subs {
        pubsub.Close()
    }
    s.subRegistry.mu.Unlock()
}
```

---

## 4. Database Access Patterns

### 4.1 PostgreSQL — pgxpool Connection Pool

```go
// Shared pattern used by auth-service, user-service, chat-service, notification-service.
package database

import (
    "context"
    "fmt"
    "time"

    "github.com/jackc/pgx/v5/pgxpool"
)

type PostgresConfig struct {
    DSN             string
    MaxConns        int32         // Recommended: 10-20 per service instance
    MinConns        int32         // Recommended: 2-5
    MaxConnLifetime time.Duration // Recommended: 1h
    MaxConnIdleTime time.Duration // Recommended: 30m
}

func NewPostgresPool(ctx context.Context, cfg PostgresConfig) (*pgxpool.Pool, error) {
    poolCfg, err := pgxpool.ParseConfig(cfg.DSN)
    if err != nil {
        return nil, fmt.Errorf("failed to parse PostgreSQL DSN: %w", err)
    }

    poolCfg.MaxConns = cfg.MaxConns
    poolCfg.MinConns = cfg.MinConns
    poolCfg.MaxConnLifetime = cfg.MaxConnLifetime
    poolCfg.MaxConnIdleTime = cfg.MaxConnIdleTime

    pool, err := pgxpool.NewWithConfig(ctx, poolCfg)
    if err != nil {
        return nil, fmt.Errorf("failed to create PostgreSQL pool: %w", err)
    }

    if err := pool.Ping(ctx); err != nil {
        return nil, fmt.Errorf("failed to ping PostgreSQL: %w", err)
    }

    return pool, nil
}
```

### 4.2 PostgreSQL — Transaction Helper

```go
// database/tx.go
package database

import (
    "context"
    "fmt"

    "github.com/jackc/pgx/v5"
    "github.com/jackc/pgx/v5/pgxpool"
)

// WithTx executes fn inside a database transaction.
// If fn returns an error, the transaction is rolled back; otherwise it is committed.
func WithTx(ctx context.Context, pool *pgxpool.Pool, fn func(tx pgx.Tx) error) error {
    tx, err := pool.Begin(ctx)
    if err != nil {
        return fmt.Errorf("begin tx: %w", err)
    }

    if err := fn(tx); err != nil {
        if rbErr := tx.Rollback(ctx); rbErr != nil {
            return fmt.Errorf("rollback failed: %v (original: %w)", rbErr, err)
        }
        return err
    }

    if err := tx.Commit(ctx); err != nil {
        return fmt.Errorf("commit tx: %w", err)
    }
    return nil
}
```

### 4.3 PostgreSQL — Migration Strategy (golang-migrate)

```
backend/migrations/
├── 000001_create_users.up.sql
├── 000001_create_users.down.sql
├── 000002_create_contacts.up.sql
├── 000002_create_contacts.down.sql
├── 000003_create_chats.up.sql
├── 000003_create_chats.down.sql
├── 000004_create_chat_participants.up.sql
├── 000004_create_chat_participants.down.sql
├── 000005_create_groups.up.sql
├── 000005_create_groups.down.sql
├── 000006_create_device_tokens.up.sql
├── 000006_create_device_tokens.down.sql
├── 000007_create_refresh_tokens.up.sql
├── 000007_create_refresh_tokens.down.sql
├── 000008_create_privacy_settings.up.sql
├── 000008_create_privacy_settings.down.sql
```

Example migration:

```sql
-- 000001_create_users.up.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone        VARCHAR(20)  NOT NULL UNIQUE,
    display_name VARCHAR(64)  NOT NULL DEFAULT '',
    avatar_url   TEXT         NOT NULL DEFAULT '',
    status_text  VARCHAR(140) NOT NULL DEFAULT '',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users(phone);
```

```sql
-- 000001_create_users.down.sql
DROP TABLE IF EXISTS users;
```

```sql
-- 000003_create_chats.up.sql
CREATE TABLE chats (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type       VARCHAR(10) NOT NULL CHECK (type IN ('direct', 'group')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

```sql
-- 000004_create_chat_participants.up.sql
CREATE TABLE chat_participants (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chat_id    UUID         NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role       VARCHAR(10)  NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    is_muted   BOOLEAN      NOT NULL DEFAULT FALSE,
    mute_until TIMESTAMPTZ,
    is_pinned  BOOLEAN      NOT NULL DEFAULT FALSE,
    joined_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(chat_id, user_id)
);

CREATE INDEX idx_chat_participants_user_id ON chat_participants(user_id);
CREATE INDEX idx_chat_participants_chat_id ON chat_participants(chat_id);
```

```sql
-- 000007_create_refresh_tokens.up.sql
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash) WHERE revoked = FALSE;
```

Migration runner (called in service main.go or as a separate CLI):

```go
// database/migrate.go
package database

import (
    "fmt"

    "github.com/golang-migrate/migrate/v4"
    _ "github.com/golang-migrate/migrate/v4/database/postgres"
    _ "github.com/golang-migrate/migrate/v4/source/file"
)

func RunMigrations(dsn, migrationsPath string) error {
    m, err := migrate.New("file://"+migrationsPath, dsn)
    if err != nil {
        return fmt.Errorf("migration init: %w", err)
    }
    if err := m.Up(); err != nil && err != migrate.ErrNoChange {
        return fmt.Errorf("migration up: %w", err)
    }
    return nil
}
```

### 4.4 MongoDB — Client Setup & Index Creation

```go
// database/mongo.go
package database

import (
    "context"
    "fmt"
    "time"

    "go.mongodb.org/mongo-driver/mongo"
    "go.mongodb.org/mongo-driver/mongo/options"
    "go.mongodb.org/mongo-driver/bson"
)

type MongoConfig struct {
    URI       string
    Database  string
    MaxPool   uint64 // Recommended: 20-50
    Timeout   time.Duration
}

func NewMongoClient(ctx context.Context, cfg MongoConfig) (*mongo.Client, error) {
    opts := options.Client().
        ApplyURI(cfg.URI).
        SetMaxPoolSize(cfg.MaxPool).
        SetConnectTimeout(cfg.Timeout)

    client, err := mongo.Connect(ctx, opts)
    if err != nil {
        return nil, fmt.Errorf("mongo connect: %w", err)
    }

    if err := client.Ping(ctx, nil); err != nil {
        return nil, fmt.Errorf("mongo ping: %w", err)
    }

    return client, nil
}

// EnsureMessageIndexes creates required indexes on the messages collection.
func EnsureMessageIndexes(ctx context.Context, coll *mongo.Collection) error {
    indexes := []mongo.IndexModel{
        {
            Keys:    bson.D{{Key: "chat_id", Value: 1}, {Key: "created_at", Value: -1}},
            Options: options.Index().SetName("idx_chat_created"),
        },
        {
            Keys:    bson.D{{Key: "client_msg_id", Value: 1}},
            Options: options.Index().SetUnique(true).SetName("idx_client_msg_id"),
        },
        {
            Keys:    bson.D{{Key: "chat_id", Value: 1}, {Key: "payload.body", Value: "text"}},
            Options: options.Index().SetName("idx_chat_text_search"),
        },
    }

    _, err := coll.Indexes().CreateMany(ctx, indexes)
    return err
}

// EnsureMediaIndexes creates required indexes on the media collection.
func EnsureMediaIndexes(ctx context.Context, coll *mongo.Collection) error {
    indexes := []mongo.IndexModel{
        {
            Keys:    bson.D{{Key: "media_id", Value: 1}},
            Options: options.Index().SetUnique(true).SetName("idx_media_id"),
        },
    }

    _, err := coll.Indexes().CreateMany(ctx, indexes)
    return err
}
```

### 4.5 Redis — Client Setup & Pipeline

```go
// database/redis.go
package database

import (
    "context"
    "fmt"
    "time"

    "github.com/redis/go-redis/v9"
)

type RedisConfig struct {
    Addr         string
    Password     string
    DB           int
    PoolSize     int           // Recommended: 10-20
    MinIdleConns int           // Recommended: 5
    ReadTimeout  time.Duration // Recommended: 3s
    WriteTimeout time.Duration // Recommended: 3s
}

func NewRedisClient(cfg RedisConfig) (*redis.Client, error) {
    rdb := redis.NewClient(&redis.Options{
        Addr:         cfg.Addr,
        Password:     cfg.Password,
        DB:           cfg.DB,
        PoolSize:     cfg.PoolSize,
        MinIdleConns: cfg.MinIdleConns,
        ReadTimeout:  cfg.ReadTimeout,
        WriteTimeout: cfg.WriteTimeout,
    })

    ctx := context.Background()
    if err := rdb.Ping(ctx).Err(); err != nil {
        return nil, fmt.Errorf("redis ping: %w", err)
    }

    return rdb, nil
}

// PipelineExample demonstrates batched Redis operations using pipelines.
// Used by notification-service to check presence for multiple users at once.
func CheckPresenceBatch(ctx context.Context, rdb *redis.Client, userIDs []string) (map[string]bool, error) {
    pipe := rdb.Pipeline()
    cmds := make(map[string]*redis.StringCmd, len(userIDs))

    for _, uid := range userIDs {
        cmds[uid] = pipe.Get(ctx, "presence:"+uid)
    }

    _, err := pipe.Exec(ctx)
    if err != nil && err != redis.Nil {
        return nil, err
    }

    result := make(map[string]bool, len(userIDs))
    for uid, cmd := range cmds {
        val, err := cmd.Result()
        result[uid] = err == nil && val == "online"
    }
    return result, nil
}
```

### 4.6 Connection Pool Sizing Recommendations

| Database   | Setting            | Development | Production (per pod) |
|------------|--------------------|-------------|----------------------|
| PostgreSQL | MaxConns           | 5           | 15-20                |
| PostgreSQL | MinConns           | 1           | 3-5                  |
| PostgreSQL | MaxConnLifetime    | 1h          | 1h                   |
| PostgreSQL | MaxConnIdleTime    | 15m         | 30m                  |
| MongoDB    | MaxPoolSize        | 10          | 30-50                |
| Redis      | PoolSize           | 5           | 15-20                |
| Redis      | MinIdleConns       | 2           | 5                    |

---

## 5. Error Handling Strategy

### 5.1 AppError Type Hierarchy

All application errors are represented by `pkg/errors.AppError`, which carries:
- **Code**: machine-readable constant (e.g. `NOT_FOUND`, `VALIDATION_ERROR`)
- **Message**: human-readable description safe for API consumers
- **HTTPStatus**: corresponding HTTP status code
- **Err**: wrapped underlying error (for logging, never exposed to clients)

### 5.2 Error Wrapping Pattern

```go
// Repository layer: wrap database errors with context.
func (r *pgUserRepo) GetByID(ctx context.Context, id string) (*model.User, error) {
    var user model.User
    err := r.pool.QueryRow(ctx,
        "SELECT id, phone, display_name, avatar_url, status_text, created_at, updated_at FROM users WHERE id = $1",
        id,
    ).Scan(&user.ID, &user.Phone, &user.DisplayName, &user.AvatarURL, &user.StatusText, &user.CreatedAt, &user.UpdatedAt)

    if err != nil {
        if errors.Is(err, pgx.ErrNoRows) {
            return nil, nil // Not found — return nil, nil (service layer decides the error).
        }
        return nil, fmt.Errorf("UserRepository.GetByID(%s): %w", id, err)
    }
    return &user, nil
}

// Service layer: translate nil results to AppError, wrap unexpected errors.
func (s *userServiceImpl) GetProfile(ctx context.Context, callerID, targetID string) (*model.User, error) {
    user, err := s.userRepo.GetByID(ctx, targetID)
    if err != nil {
        return nil, apperr.NewInternal("failed to fetch user", err)
    }
    if user == nil {
        return nil, apperr.NewNotFound("user not found")
    }
    return user, nil
}

// Handler layer: use response.Error to map AppError to HTTP response.
func (h *HTTPHandler) GetUserProfile(c *gin.Context) {
    user, err := h.userSvc.GetProfile(c.Request.Context(), callerID, targetID)
    if err != nil {
        response.Error(c, err) // Automatically maps AppError.HTTPStatus
        return
    }
    response.OK(c, user)
}
```

### 5.3 Error Propagation Flow

```
Repository → returns (result, error)
    ↓ nil result = not found, wrap DB errors with fmt.Errorf
Service → converts to *AppError
    ↓ nil check → NewNotFound, DB error → NewInternal
Handler → calls response.Error(c, err)
    ↓ type-asserts *AppError → HTTP status + JSON body
    ↓ unknown error → 500 Internal Server Error
```

### 5.4 gRPC Error Mapping

```go
// pkg/errors/grpc.go
package errors

import (
    "google.golang.org/grpc/codes"
    "google.golang.org/grpc/status"
)

// ToGRPCError converts an AppError to a gRPC status error.
func ToGRPCError(err error) error {
    appErr, ok := err.(*AppError)
    if !ok {
        return status.Error(codes.Internal, "internal error")
    }
    switch appErr.HTTPStatus {
    case 400:
        return status.Error(codes.InvalidArgument, appErr.Message)
    case 401:
        return status.Error(codes.Unauthenticated, appErr.Message)
    case 403:
        return status.Error(codes.PermissionDenied, appErr.Message)
    case 404:
        return status.Error(codes.NotFound, appErr.Message)
    case 409:
        return status.Error(codes.AlreadyExists, appErr.Message)
    case 422:
        return status.Error(codes.InvalidArgument, appErr.Message)
    case 429:
        return status.Error(codes.ResourceExhausted, appErr.Message)
    default:
        return status.Error(codes.Internal, appErr.Message)
    }
}

// FromGRPCError converts a gRPC status error back to an AppError.
func FromGRPCError(err error) *AppError {
    st, ok := status.FromError(err)
    if !ok {
        return NewInternal("unknown gRPC error", err)
    }
    switch st.Code() {
    case codes.NotFound:
        return NewNotFound(st.Message())
    case codes.InvalidArgument:
        return NewBadRequest(st.Message())
    case codes.Unauthenticated:
        return NewUnauthorized(st.Message())
    case codes.PermissionDenied:
        return NewForbidden(st.Message())
    case codes.AlreadyExists:
        return NewConflict(st.Message())
    case codes.ResourceExhausted:
        return NewTooManyRequests(st.Message())
    default:
        return NewInternal(st.Message(), err)
    }
}
```

---

## 6. Middleware Chain

### 6.1 api-gateway Middleware Order

The order is critical — each middleware wraps the next:

```
Request Flow:
  ┌──────────────┐
  │  Recovery     │  1. Catches panics, returns 500
  ├──────────────┤
  │  RequestID    │  2. Generates/propagates X-Request-ID
  ├──────────────┤
  │  Logger       │  3. Logs request start + completion with latency
  ├──────────────┤
  │  CORS         │  4. Handles preflight OPTIONS + CORS headers
  ├──────────────┤
  │  RateLimit    │  5. Redis INCR counter per IP:endpoint (60 req/min)
  ├──────────────┤
  │  Auth         │  6. Validates JWT via auth-service gRPC, injects user_id
  ├──────────────┤
  │  Proxy        │  7. Reverse-proxies to downstream service
  └──────────────┘
```

```go
// api-gateway middleware registration
r.Use(
    middleware.Recovery(log),   // 1
    middleware.RequestID(),      // 2
    middleware.Logger(log),      // 3
    middleware.CORS(origins),    // 4
)

// Public routes (no auth/rate-limit):
publicGroup := r.Group("/api/v1/auth")

// Protected routes:
protectedGroup := r.Group("/api/v1")
protectedGroup.Use(
    handler.RateLimitMiddleware(rateLimiter, cfg.RateLimitRPS), // 5
    handler.AuthMiddleware(authValidator),                       // 6
)
```

### 6.2 Per-Service Middleware Order

Each downstream service applies its own middleware stack (lighter — no auth, no rate limit):

```go
// Per-service (auth-service, user-service, etc.)
r.Use(
    middleware.Recovery(log),   // 1. Panic recovery
    middleware.RequestID(),      // 2. Request ID propagation
    middleware.Logger(log),      // 3. Request logging
)
```

---

## 7. Graceful Shutdown Pattern

```go
// pkg/server/shutdown.go
package server

import (
    "context"
    "net"
    "net/http"
    "os"
    "os/signal"
    "syscall"
    "time"

    "github.com/nats-io/nats.go"
    "github.com/jackc/pgx/v5/pgxpool"
    "github.com/redis/go-redis/v9"
    "github.com/rs/zerolog"
    "go.mongodb.org/mongo-driver/mongo"
    "google.golang.org/grpc"
)

// GracefulShutdown coordinates the ordered shutdown of all server components.
// The shutdown order is:
// 1. Stop accepting new connections (HTTP + gRPC)
// 2. Drain NATS subscriptions (process in-flight messages)
// 3. Wait for in-flight HTTP/gRPC requests to complete (with timeout)
// 4. Close database connections (Redis, PostgreSQL, MongoDB)
func GracefulShutdown(
    log zerolog.Logger,
    httpSrv *http.Server,
    grpcSrv *grpc.Server,
    nc *nats.Conn,
    pgPool *pgxpool.Pool,
    rdb *redis.Client,
    mongoCli *mongo.Client,
    timeout time.Duration,
) {
    quit := make(chan os.Signal, 1)
    signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
    sig := <-quit

    log.Info().Str("signal", sig.String()).Msg("shutdown signal received")

    ctx, cancel := context.WithTimeout(context.Background(), timeout)
    defer cancel()

    // 1. HTTP server: stop accepting new connections, finish in-flight requests.
    if httpSrv != nil {
        log.Info().Msg("shutting down HTTP server")
        if err := httpSrv.Shutdown(ctx); err != nil {
            log.Error().Err(err).Msg("HTTP server shutdown error")
        }
    }

    // 2. gRPC server: graceful stop (finishes in-flight RPCs).
    if grpcSrv != nil {
        log.Info().Msg("shutting down gRPC server")
        grpcSrv.GracefulStop()
    }

    // 3. NATS: drain subscriptions (processes buffered messages, then disconnects).
    if nc != nil {
        log.Info().Msg("draining NATS connection")
        if err := nc.Drain(); err != nil {
            log.Error().Err(err).Msg("NATS drain error")
        }
    }

    // 4. Close database connections.
    if rdb != nil {
        log.Info().Msg("closing Redis connection")
        if err := rdb.Close(); err != nil {
            log.Error().Err(err).Msg("Redis close error")
        }
    }

    if pgPool != nil {
        log.Info().Msg("closing PostgreSQL pool")
        pgPool.Close()
    }

    if mongoCli != nil {
        log.Info().Msg("closing MongoDB connection")
        if err := mongoCli.Disconnect(ctx); err != nil {
            log.Error().Err(err).Msg("MongoDB disconnect error")
        }
    }

    log.Info().Msg("shutdown complete")
}
```

Usage in a service's `main.go`:

```go
func main() {
    // ... setup code ...

    // Start HTTP server.
    httpSrv := &http.Server{Addr: cfg.HTTPPort, Handler: r}
    go func() {
        if err := httpSrv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            log.Fatal().Err(err).Msg("HTTP server failed")
        }
    }()

    // Start gRPC server.
    grpcSrv := grpc.NewServer()
    // ... register services ...
    lis, _ := net.Listen("tcp", cfg.GRPCPort)
    go func() {
        if err := grpcSrv.Serve(lis); err != nil {
            log.Fatal().Err(err).Msg("gRPC server failed")
        }
    }()

    log.Info().Msg("service started")

    // Block until shutdown signal, then shut down everything in order.
    server.GracefulShutdown(log, httpSrv, grpcSrv, nc, pgPool, rdb, mongoCli, 15*time.Second)
}
```

---

## 8. Testing Strategy

### 8.1 Unit Test Patterns (Interface Mocking)

All business logic depends on interfaces, enabling straightforward mocking with `testify/mock` or `mockgen`.

```go
// user-service/internal/service/user_service_test.go
package service_test

import (
    "context"
    "testing"

    "github.com/stretchr/testify/assert"
    "github.com/stretchr/testify/mock"

    "github.com/whatsapp-clone/backend/user-service/internal/model"
    "github.com/whatsapp-clone/backend/user-service/internal/service"
)

// --- Mock Definitions ---

type MockUserRepository struct {
    mock.Mock
}

func (m *MockUserRepository) GetByID(ctx context.Context, id string) (*model.User, error) {
    args := m.Called(ctx, id)
    if args.Get(0) == nil {
        return nil, args.Error(1)
    }
    return args.Get(0).(*model.User), args.Error(1)
}

func (m *MockUserRepository) GetByIDs(ctx context.Context, ids []string) ([]*model.User, error) {
    args := m.Called(ctx, ids)
    return args.Get(0).([]*model.User), args.Error(1)
}

func (m *MockUserRepository) GetByPhone(ctx context.Context, phone string) (*model.User, error) {
    args := m.Called(ctx, phone)
    if args.Get(0) == nil {
        return nil, args.Error(1)
    }
    return args.Get(0).(*model.User), args.Error(1)
}

func (m *MockUserRepository) GetByPhones(ctx context.Context, phones []string) ([]*model.ContactSyncResult, error) {
    args := m.Called(ctx, phones)
    return args.Get(0).([]*model.ContactSyncResult), args.Error(1)
}

func (m *MockUserRepository) Update(ctx context.Context, id string, req *model.UpdateProfileRequest) (*model.User, error) {
    args := m.Called(ctx, id, req)
    return args.Get(0).(*model.User), args.Error(1)
}

type MockContactRepository struct {
    mock.Mock
}

func (m *MockContactRepository) IsBlocked(ctx context.Context, userID, contactID string) (bool, error) {
    args := m.Called(ctx, userID, contactID)
    return args.Bool(0), args.Error(1)
}

func (m *MockContactRepository) IsContact(ctx context.Context, userID, contactID string) (bool, error) {
    args := m.Called(ctx, userID, contactID)
    return args.Bool(0), args.Error(1)
}

// ... (other methods omitted for brevity) ...

type MockPrivacyRepository struct {
    mock.Mock
}

func (m *MockPrivacyRepository) Get(ctx context.Context, userID string) (*model.PrivacySettings, error) {
    args := m.Called(ctx, userID)
    if args.Get(0) == nil {
        return nil, args.Error(1)
    }
    return args.Get(0).(*model.PrivacySettings), args.Error(1)
}

// --- Test Cases ---

func TestGetProfile_Success(t *testing.T) {
    ctx := context.Background()
    userRepo := new(MockUserRepository)
    contactRepo := new(MockContactRepository)
    privacyRepo := new(MockPrivacyRepository)

    svc := service.NewUserService(userRepo, contactRepo, privacyRepo, nil, nil)

    expectedUser := &model.User{
        ID:          "user-1",
        Phone:       "+14155552671",
        DisplayName: "Alice",
        AvatarURL:   "https://cdn.example.com/alice.jpg",
        StatusText:  "Hello!",
    }

    contactRepo.On("IsBlocked", ctx, "user-1", "caller-1").Return(false, nil)
    userRepo.On("GetByID", ctx, "user-1").Return(expectedUser, nil)
    privacyRepo.On("Get", ctx, "user-1").Return(nil, nil) // No privacy settings

    user, err := svc.GetProfile(ctx, "caller-1", "user-1")

    assert.NoError(t, err)
    assert.Equal(t, "Alice", user.DisplayName)
    assert.Equal(t, "https://cdn.example.com/alice.jpg", user.AvatarURL)
    userRepo.AssertExpectations(t)
    contactRepo.AssertExpectations(t)
}

func TestGetProfile_BlockedUser(t *testing.T) {
    ctx := context.Background()
    userRepo := new(MockUserRepository)
    contactRepo := new(MockContactRepository)
    privacyRepo := new(MockPrivacyRepository)

    svc := service.NewUserService(userRepo, contactRepo, privacyRepo, nil, nil)

    contactRepo.On("IsBlocked", ctx, "user-1", "caller-1").Return(true, nil)

    user, err := svc.GetProfile(ctx, "caller-1", "user-1")

    assert.Nil(t, user)
    assert.Error(t, err)
    assert.Contains(t, err.Error(), "not available")
}

func TestGetProfile_PrivacyFiltering(t *testing.T) {
    ctx := context.Background()
    userRepo := new(MockUserRepository)
    contactRepo := new(MockContactRepository)
    privacyRepo := new(MockPrivacyRepository)

    svc := service.NewUserService(userRepo, contactRepo, privacyRepo, nil, nil)

    expectedUser := &model.User{
        ID:          "user-1",
        DisplayName: "Alice",
        AvatarURL:   "https://cdn.example.com/alice.jpg",
        StatusText:  "Hello!",
    }

    privacy := &model.PrivacySettings{
        UserID:       "user-1",
        ProfilePhoto: model.VisibilityContacts,
        About:        model.VisibilityNobody,
    }

    contactRepo.On("IsBlocked", ctx, "user-1", "caller-1").Return(false, nil)
    userRepo.On("GetByID", ctx, "user-1").Return(expectedUser, nil)
    privacyRepo.On("Get", ctx, "user-1").Return(privacy, nil)
    contactRepo.On("IsContact", ctx, "user-1", "caller-1").Return(false, nil)

    user, err := svc.GetProfile(ctx, "caller-1", "user-1")

    assert.NoError(t, err)
    assert.Equal(t, "", user.AvatarURL, "avatar should be hidden for non-contacts")
    assert.Equal(t, "", user.StatusText, "about should be hidden for nobody")
}
```

### 8.2 Integration Test Patterns (testcontainers-go)

```go
// integration_test.go
package integration_test

import (
    "context"
    "testing"
    "time"

    "github.com/stretchr/testify/require"
    "github.com/testcontainers/testcontainers-go"
    "github.com/testcontainers/testcontainers-go/modules/postgres"
    "github.com/testcontainers/testcontainers-go/modules/redis"
    "github.com/testcontainers/testcontainers-go/wait"
    "github.com/jackc/pgx/v5/pgxpool"
    goredis "github.com/redis/go-redis/v9"
)

func setupPostgres(t *testing.T) (*pgxpool.Pool, func()) {
    ctx := context.Background()

    pgContainer, err := postgres.Run(ctx, "postgres:16-alpine",
        postgres.WithDatabase("testdb"),
        postgres.WithUsername("test"),
        postgres.WithPassword("test"),
        testcontainers.WithWaitStrategy(
            wait.ForLog("database system is ready to accept connections").
                WithOccurrence(2).WithStartupTimeout(30*time.Second),
        ),
    )
    require.NoError(t, err)

    connStr, err := pgContainer.ConnectionString(ctx, "sslmode=disable")
    require.NoError(t, err)

    pool, err := pgxpool.New(ctx, connStr)
    require.NoError(t, err)

    // Run migrations.
    _, err = pool.Exec(ctx, `
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
        CREATE TABLE users (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            phone VARCHAR(20) NOT NULL UNIQUE,
            display_name VARCHAR(64) NOT NULL DEFAULT '',
            avatar_url TEXT NOT NULL DEFAULT '',
            status_text VARCHAR(140) NOT NULL DEFAULT '',
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );
    `)
    require.NoError(t, err)

    cleanup := func() {
        pool.Close()
        pgContainer.Terminate(ctx)
    }

    return pool, cleanup
}

func setupRedis(t *testing.T) (*goredis.Client, func()) {
    ctx := context.Background()

    redisContainer, err := redis.Run(ctx, "redis:7-alpine")
    require.NoError(t, err)

    endpoint, err := redisContainer.Endpoint(ctx, "")
    require.NoError(t, err)

    rdb := goredis.NewClient(&goredis.Options{Addr: endpoint})

    cleanup := func() {
        rdb.Close()
        redisContainer.Terminate(ctx)
    }

    return rdb, cleanup
}

func TestUserRepository_Integration(t *testing.T) {
    if testing.Short() {
        t.Skip("skipping integration test")
    }

    pool, cleanup := setupPostgres(t)
    defer cleanup()

    repo := repository.NewPostgresUserRepository(pool)
    ctx := context.Background()

    // Test UpsertByPhone.
    user, err := repo.UpsertByPhone(ctx, "+14155552671")
    require.NoError(t, err)
    require.NotEmpty(t, user.ID)
    require.Equal(t, "+14155552671", user.Phone)

    // Test idempotency.
    user2, err := repo.UpsertByPhone(ctx, "+14155552671")
    require.NoError(t, err)
    require.Equal(t, user.ID, user2.ID)

    // Test GetByID.
    fetched, err := repo.GetByID(ctx, user.ID)
    require.NoError(t, err)
    require.Equal(t, user.Phone, fetched.Phone)
}
```

### 8.3 gRPC Test Patterns (bufconn)

```go
// auth-service/internal/handler/grpc_handler_test.go
package handler_test

import (
    "context"
    "net"
    "testing"

    "github.com/stretchr/testify/assert"
    "github.com/stretchr/testify/mock"
    "google.golang.org/grpc"
    "google.golang.org/grpc/credentials/insecure"
    "google.golang.org/grpc/test/bufconn"

    "github.com/whatsapp-clone/backend/auth-service/internal/handler"
    authv1 "github.com/whatsapp-clone/backend/proto/auth/v1"
)

const bufSize = 1024 * 1024

func setupGRPCServer(t *testing.T, authSvc *MockAuthService) (authv1.AuthServiceClient, func()) {
    lis := bufconn.Listen(bufSize)
    srv := grpc.NewServer()
    authv1.RegisterAuthServiceServer(srv, handler.NewGRPCHandler(authSvc))

    go func() {
        if err := srv.Serve(lis); err != nil {
            t.Logf("gRPC server error: %v", err)
        }
    }()

    conn, err := grpc.NewClient("passthrough:///bufconn",
        grpc.WithContextDialer(func(ctx context.Context, _ string) (net.Conn, error) {
            return lis.DialContext(ctx)
        }),
        grpc.WithTransportCredentials(insecure.NewCredentials()),
    )
    assert.NoError(t, err)

    client := authv1.NewAuthServiceClient(conn)
    cleanup := func() {
        conn.Close()
        srv.Stop()
        lis.Close()
    }

    return client, cleanup
}

type MockAuthService struct {
    mock.Mock
}

func (m *MockAuthService) ValidateToken(ctx context.Context, token string) (string, string, error) {
    args := m.Called(ctx, token)
    return args.String(0), args.String(1), args.Error(2)
}

// ... other AuthService methods ...

func TestValidateToken_GRPC_Valid(t *testing.T) {
    authSvc := new(MockAuthService)
    authSvc.On("ValidateToken", mock.Anything, "valid-token").
        Return("user-123", "+14155552671", nil)

    client, cleanup := setupGRPCServer(t, authSvc)
    defer cleanup()

    resp, err := client.ValidateToken(context.Background(), &authv1.ValidateTokenRequest{
        Token: "valid-token",
    })

    assert.NoError(t, err)
    assert.True(t, resp.Valid)
    assert.Equal(t, "user-123", resp.UserId)
    assert.Equal(t, "+14155552671", resp.Phone)
}

func TestValidateToken_GRPC_Invalid(t *testing.T) {
    authSvc := new(MockAuthService)
    authSvc.On("ValidateToken", mock.Anything, "bad-token").
        Return("", "", fmt.Errorf("invalid token"))

    client, cleanup := setupGRPCServer(t, authSvc)
    defer cleanup()

    resp, err := client.ValidateToken(context.Background(), &authv1.ValidateTokenRequest{
        Token: "bad-token",
    })

    assert.NoError(t, err) // gRPC call succeeds, but Valid=false
    assert.False(t, resp.Valid)
}
```

---

## 9. Migration Strategy

### 9.1 PostgreSQL Migrations (golang-migrate)

**File naming convention:**

```
{sequence}_{description}.{direction}.sql
```

- `sequence`: 6-digit zero-padded number (`000001`, `000002`, ...)
- `description`: snake_case description of what the migration does
- `direction`: `up` for apply, `down` for rollback

**Full migration file list:**

| File | Description |
|------|-------------|
| `000001_create_users.up.sql` | Creates `users` table with UUID PK, phone unique index |
| `000001_create_users.down.sql` | Drops `users` table |
| `000002_create_contacts.up.sql` | Creates `contacts` table with FK to users, UNIQUE(user_id, contact_id) |
| `000002_create_contacts.down.sql` | Drops `contacts` table |
| `000003_create_chats.up.sql` | Creates `chats` table with type CHECK constraint |
| `000003_create_chats.down.sql` | Drops `chats` table |
| `000004_create_chat_participants.up.sql` | Creates `chat_participants` with FKs, role CHECK, UNIQUE(chat_id, user_id) |
| `000004_create_chat_participants.down.sql` | Drops `chat_participants` table |
| `000005_create_groups.up.sql` | Creates `groups` table with FK to chats(id) as PK |
| `000005_create_groups.down.sql` | Drops `groups` table |
| `000006_create_device_tokens.up.sql` | Creates `device_tokens` with unique token index |
| `000006_create_device_tokens.down.sql` | Drops `device_tokens` table |
| `000007_create_refresh_tokens.up.sql` | Creates `refresh_tokens` with unique token_hash, partial index on non-revoked |
| `000007_create_refresh_tokens.down.sql` | Drops `refresh_tokens` table |
| `000008_create_privacy_settings.up.sql` | Creates `privacy_settings` with user_id as PK FK |
| `000008_create_privacy_settings.down.sql` | Drops `privacy_settings` table |

**Running migrations in production:**

```bash
# Via CLI
migrate -path ./migrations -database "postgres://user:pass@host:5432/whatsapp?sslmode=disable" up

# Via Go code (in main.go or init container)
database.RunMigrations(cfg.PostgresDSN, "./migrations")
```

### 9.2 MongoDB Index Creation

MongoDB indexes are created at service startup rather than via a separate migration tool. This is idempotent — `CreateMany` with existing indexes is a no-op.

```go
// Called in message-service main.go and media-service main.go
func main() {
    // ... setup MongoDB client ...
    
    msgColl := mongoClient.Database(cfg.MongoDB).Collection("messages")
    if err := database.EnsureMessageIndexes(ctx, msgColl); err != nil {
        log.Fatal().Err(err).Msg("failed to create message indexes")
    }

    mediaColl := mongoClient.Database(cfg.MongoDB).Collection("media")
    if err := database.EnsureMediaIndexes(ctx, mediaColl); err != nil {
        log.Fatal().Err(err).Msg("failed to create media indexes")
    }
}
```

---

## 10. Configuration Management

### 10.1 Environment Variable Naming Convention

```
{SERVICE_NAME}_{VARIABLE_NAME}
```

Examples:
- `AUTH_JWT_SECRET` — auth-service JWT signing secret
- `AUTH_POSTGRES_DSN` — auth-service PostgreSQL connection string
- `GATEWAY_REDIS_ADDR` — api-gateway Redis address
- `MESSAGE_MONGO_URI` — message-service MongoDB URI
- `WS_PING_INTERVAL` — websocket-service ping interval
- `NOTIF_FCM_CREDENTIALS_JSON` — notification-service FCM credentials path
- `MEDIA_MINIO_ENDPOINT` — media-service MinIO endpoint

### 10.2 Config Loading Pattern with Defaults

Each service has its own `config/config.go` with a struct tagged with `env` and `envDefault`:

```go
type Config struct {
    HTTPPort    string        `env:"AUTH_HTTP_PORT"        envDefault:":8081"`
    GRPCPort    string        `env:"AUTH_GRPC_PORT"        envDefault:":9081"`
    PostgresDSN string        `env:"AUTH_POSTGRES_DSN"     envRequired:"true"`
    JWTSecret   string        `env:"AUTH_JWT_SECRET"       envRequired:"true"`
    LogLevel    string        `env:"AUTH_LOG_LEVEL"        envDefault:"info"`
    AccessTTL   time.Duration `env:"AUTH_ACCESS_TOKEN_TTL" envDefault:"15m"`
}
```

Loading is done via the shared `pkg/config.Load`:

```go
var cfg config.Config
if err := pkgcfg.Load(&cfg); err != nil {
    log.Fatal().Err(err).Msg("failed to load config")
}
```

### 10.3 Per-Environment Overrides

Environment-specific configuration is handled via:

1. **Local development**: `.env` file loaded by Docker Compose or `direnv`
2. **Kubernetes**: ConfigMaps for non-sensitive values, Secrets for sensitive values
3. **Helm values**: `values.yaml`, `values-staging.yaml`, `values-production.yaml`

```yaml
# docker-compose.yml (development)
services:
  auth-service:
    environment:
      AUTH_POSTGRES_DSN: "postgres://whatsapp:whatsapp@postgres:5432/whatsapp?sslmode=disable"
      AUTH_REDIS_ADDR: "redis:6379"
      AUTH_JWT_SECRET: "dev-secret-change-in-production"
      AUTH_LOG_LEVEL: "debug"
      AUTH_ACCESS_TOKEN_TTL: "1h"  # Longer TTL for dev convenience
```

```yaml
# helm/values-production.yaml
authService:
  env:
    AUTH_LOG_LEVEL: "info"
    AUTH_ACCESS_TOKEN_TTL: "15m"
    AUTH_OTP_MAX_ATTEMPTS: "3"
  secretEnv:
    AUTH_JWT_SECRET:
      secretName: auth-secrets
      key: jwt-secret
    AUTH_POSTGRES_DSN:
      secretName: auth-secrets
      key: postgres-dsn
```

### 10.4 Complete Environment Variable Reference

| Variable | Service | Required | Default | Description |
|----------|---------|----------|---------|-------------|
| `GATEWAY_PORT` | api-gateway | No | `:8080` | HTTP listen port |
| `AUTH_GRPC_ADDR` | api-gateway | No | `auth-service:9081` | Auth gRPC target |
| `GATEWAY_REDIS_ADDR` | api-gateway | No | `redis:6379` | Redis for rate limiting |
| `GATEWAY_RATE_LIMIT_RPS` | api-gateway | No | `60` | Requests per minute per IP |
| `GATEWAY_CORS_ORIGINS` | api-gateway | No | `*` | Comma-separated CORS origins |
| `AUTH_HTTP_PORT` | auth-service | No | `:8081` | HTTP listen port |
| `AUTH_GRPC_PORT` | auth-service | No | `:9081` | gRPC listen port |
| `AUTH_POSTGRES_DSN` | auth-service | Yes | — | PostgreSQL connection string |
| `AUTH_REDIS_ADDR` | auth-service | No | `redis:6379` | Redis for OTP storage |
| `AUTH_JWT_SECRET` | auth-service | Yes | — | HS256 signing key |
| `AUTH_ACCESS_TOKEN_TTL` | auth-service | No | `15m` | Access token lifetime |
| `AUTH_REFRESH_TOKEN_TTL` | auth-service | No | `720h` | Refresh token lifetime (30d) |
| `AUTH_OTP_TTL` | auth-service | No | `5m` | OTP expiry |
| `AUTH_OTP_MAX_ATTEMPTS` | auth-service | No | `5` | Max OTP verification attempts |
| `USER_HTTP_PORT` | user-service | No | `:8082` | HTTP listen port |
| `USER_GRPC_PORT` | user-service | No | `:9082` | gRPC listen port |
| `USER_POSTGRES_DSN` | user-service | Yes | — | PostgreSQL connection string |
| `USER_REDIS_ADDR` | user-service | No | `redis:6379` | Redis for presence |
| `USER_PRESENCE_TTL` | user-service | No | `60s` | Presence key TTL |
| `CHAT_HTTP_PORT` | chat-service | No | `:8083` | HTTP listen port |
| `CHAT_GRPC_PORT` | chat-service | No | `:9083` | gRPC listen port |
| `CHAT_POSTGRES_DSN` | chat-service | Yes | — | PostgreSQL connection string |
| `CHAT_NATS_URL` | chat-service | No | `nats://nats:4222` | NATS server URL |
| `CHAT_MESSAGE_GRPC_ADDR` | chat-service | No | `message-service:9084` | Message gRPC target |
| `MESSAGE_HTTP_PORT` | message-service | No | `:8084` | HTTP listen port |
| `MESSAGE_GRPC_PORT` | message-service | No | `:9084` | gRPC listen port |
| `MESSAGE_MONGO_URI` | message-service | Yes | — | MongoDB connection string |
| `MESSAGE_MONGO_DB` | message-service | No | `whatsapp` | MongoDB database name |
| `MESSAGE_NATS_URL` | message-service | No | `nats://nats:4222` | NATS server URL |
| `NOTIF_HTTP_PORT` | notification-service | No | `:8085` | HTTP listen port |
| `NOTIF_POSTGRES_DSN` | notification-service | Yes | — | PostgreSQL connection string |
| `NOTIF_REDIS_ADDR` | notification-service | No | `redis:6379` | Redis for presence checks |
| `NOTIF_NATS_URL` | notification-service | No | `nats://nats:4222` | NATS server URL |
| `NOTIF_FCM_CREDENTIALS_JSON` | notification-service | Yes | — | Path to FCM service account JSON |
| `NOTIF_GROUP_DEBOUNCE` | notification-service | No | `3s` | Group notification debounce window |
| `MEDIA_HTTP_PORT` | media-service | No | `:8086` | HTTP listen port |
| `MEDIA_GRPC_PORT` | media-service | No | `:9086` | gRPC listen port |
| `MEDIA_MONGO_URI` | media-service | Yes | — | MongoDB connection string |
| `MEDIA_MINIO_ENDPOINT` | media-service | No | `minio:9000` | MinIO endpoint |
| `MEDIA_MINIO_ACCESS_KEY` | media-service | Yes | — | MinIO access key |
| `MEDIA_MINIO_SECRET_KEY` | media-service | Yes | — | MinIO secret key |
| `MEDIA_MINIO_BUCKET` | media-service | No | `media` | MinIO bucket name |
| `MEDIA_PRESIGNED_TTL` | media-service | No | `1h` | Presigned URL expiry |
| `MEDIA_FFMPEG_PATH` | media-service | No | `/usr/bin/ffmpeg` | FFmpeg binary path |
| `WS_PORT` | websocket-service | No | `:8087` | WebSocket listen port |
| `WS_AUTH_GRPC_ADDR` | websocket-service | No | `auth-service:9081` | Auth gRPC target |
| `WS_REDIS_ADDR` | websocket-service | No | `redis:6379` | Redis for pub-sub + presence |
| `WS_NATS_URL` | websocket-service | No | `nats://nats:4222` | NATS server URL |
| `WS_PING_INTERVAL` | websocket-service | No | `25s` | WebSocket ping interval |
| `WS_PONG_TIMEOUT` | websocket-service | No | `60s` | Kill connection after no pong |
| `WS_MAX_MSG_SIZE` | websocket-service | No | `65536` | Max WebSocket message size (bytes) |

---

*End of Low-Level Design Document.*
