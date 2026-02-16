FROM golang:1.24-alpine AS builder
WORKDIR /app
ENV GOWORK=off

# Copy shared module files for caching
COPY pkg/go.mod pkg/go.sum ./pkg/
COPY proto/go.mod proto/go.sum ./proto/

# Copy service module files
COPY media-service/go.mod media-service/go.sum ./media-service/

# Download all dependencies including shared modules
RUN cd pkg && go mod download && \
    cd ../proto && go mod download && \
    cd ../media-service && go mod download

# Copy all source code
COPY pkg/ ./pkg/
COPY proto/ ./proto/
COPY media-service/ ./media-service/

# Build
RUN cd media-service && CGO_ENABLED=0 GOOS=linux go build -o /media-service ./cmd/main.go

FROM alpine:3.19
RUN apk --no-cache add ca-certificates tzdata ffmpeg
WORKDIR /app
COPY --from=builder /media-service .
EXPOSE 8086 9086
CMD ["./media-service"]
