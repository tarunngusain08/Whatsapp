FROM golang:1.24-alpine AS builder
WORKDIR /app
ENV GOWORK=off

# Copy shared module files for caching
COPY pkg/go.mod pkg/go.sum ./pkg/
COPY proto/go.mod proto/go.sum ./proto/

# Copy service module files
COPY api-gateway/go.mod api-gateway/go.sum ./api-gateway/

# Download all dependencies including shared modules
RUN cd pkg && go mod download && \
    cd ../proto && go mod download && \
    cd ../api-gateway && go mod download

# Copy all source code
COPY pkg/ ./pkg/
COPY proto/ ./proto/
COPY api-gateway/ ./api-gateway/

# Build
RUN cd api-gateway && CGO_ENABLED=0 GOOS=linux go build -o /api-gateway ./cmd/main.go

FROM alpine:3.19
RUN apk --no-cache add ca-certificates tzdata
WORKDIR /app
COPY --from=builder /api-gateway .
EXPOSE 8080
CMD ["./api-gateway"]
