FROM golang:1.24-alpine AS builder
WORKDIR /app
ENV GOWORK=off

# Copy shared module files for caching
COPY pkg/go.mod pkg/go.sum ./pkg/
COPY proto/go.mod proto/go.sum ./proto/

# Copy service module files
COPY chat-service/go.mod chat-service/go.sum ./chat-service/

# Download all dependencies including shared modules
RUN cd pkg && go mod download && \
    cd ../proto && go mod download && \
    cd ../chat-service && go mod download

# Copy all source code
COPY pkg/ ./pkg/
COPY proto/ ./proto/
COPY chat-service/ ./chat-service/

# Build
RUN cd chat-service && CGO_ENABLED=0 GOOS=linux go build -o /chat-service ./cmd/main.go

FROM alpine:3.19
RUN apk --no-cache add ca-certificates tzdata
WORKDIR /app
COPY --from=builder /chat-service .
EXPOSE 8083 9083
CMD ["./chat-service"]
