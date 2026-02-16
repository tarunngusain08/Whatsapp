FROM golang:1.24-alpine AS builder
WORKDIR /app
ENV GOWORK=off

# Copy shared module files for caching
COPY pkg/go.mod pkg/go.sum ./pkg/
COPY proto/go.mod proto/go.sum ./proto/

# Copy service module files
COPY notification-service/go.mod notification-service/go.sum ./notification-service/

# Download all dependencies including shared modules
RUN cd pkg && go mod download && \
    cd ../proto && go mod download && \
    cd ../notification-service && go mod download

# Copy all source code
COPY pkg/ ./pkg/
COPY proto/ ./proto/
COPY notification-service/ ./notification-service/

# Build
RUN cd notification-service && CGO_ENABLED=0 GOOS=linux go build -o /notification-service ./cmd/main.go

FROM alpine:3.19
RUN apk --no-cache add ca-certificates tzdata
WORKDIR /app
COPY --from=builder /notification-service .
EXPOSE 8085
CMD ["./notification-service"]
