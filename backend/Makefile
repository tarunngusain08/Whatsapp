.PHONY: build test lint proto migrate-up migrate-down mongo-indexes minio-init \
       docker-up docker-down docker-build docker-logs clean kind-up kind-down ngrok load-test

# ─────────────────────────────────────────────
# Build & Test
# ─────────────────────────────────────────────
SERVICES := api-gateway auth-service user-service chat-service message-service \
            media-service notification-service websocket-service

build:
	@echo "Building all services..."
	@for svc in $(SERVICES); do \
		echo "  → $$svc"; \
		cd $$svc && go build -o ../bin/$$svc ./cmd/... && cd ..; \
	done
	@echo "All services built → ./bin/"

test:
	@echo "Running tests..."
	@for svc in $(SERVICES); do \
		echo "  → $$svc"; \
		cd $$svc && go test ./... -v -race && cd ..; \
	done

lint:
	@echo "Running linter..."
	@for svc in $(SERVICES); do \
		echo "  → $$svc"; \
		cd $$svc && golangci-lint run ./... && cd ..; \
	done

# ─────────────────────────────────────────────
# Proto
# ─────────────────────────────────────────────
proto:
	@echo "Generating protobuf code..."
	@for dir in proto/*/v1; do \
		echo "  → $$dir"; \
		protoc --go_out=. --go_opt=paths=source_relative \
		       --go-grpc_out=. --go-grpc_opt=paths=source_relative \
		       $$dir/*.proto; \
	done
	@echo "Proto generation complete."

# ─────────────────────────────────────────────
# Migrations
# ─────────────────────────────────────────────
POSTGRES_DSN ?= postgres://whatsapp:whatsapp_secret@localhost:5432/whatsapp?sslmode=disable

migrate-up:
	@echo "Running PostgreSQL migrations (up)..."
	migrate -path migrations/postgres -database "$(POSTGRES_DSN)" up
	@echo "Migrations applied."

migrate-down:
	@echo "Rolling back PostgreSQL migrations..."
	migrate -path migrations/postgres -database "$(POSTGRES_DSN)" down 1
	@echo "Migration rolled back."

mongo-indexes:
	@echo "Creating MongoDB indexes..."
	mongosh --host localhost:27017 migrations/mongo/init.js
	@echo "MongoDB indexes created."

minio-init:
	@echo "Initializing MinIO buckets..."
	bash scripts/minio-init.sh
	@echo "MinIO initialized."

# ─────────────────────────────────────────────
# Docker
# ─────────────────────────────────────────────
docker-up:
	docker compose up -d --build

docker-down:
	docker compose down

docker-build:
	docker compose build

docker-logs:
	docker compose logs -f

# ─────────────────────────────────────────────
# Clean
# ─────────────────────────────────────────────
clean:
	@echo "Cleaning build artifacts..."
	rm -rf bin/ tmp/
	@for svc in $(SERVICES); do \
		rm -f $$svc/$$svc; \
	done
	@echo "Clean complete."

# ─────────────────────────────────────────────
# Kubernetes (Kind)
# ─────────────────────────────────────────────
kind-up:
	@echo "Creating Kind cluster..."
	kind create cluster --name whatsapp
	kubectl apply -f helm/
	@echo "Kind cluster ready."

kind-down:
	@echo "Deleting Kind cluster..."
	kind delete cluster --name whatsapp
	@echo "Kind cluster deleted."

# ─────────────────────────────────────────────
# Ngrok
# ─────────────────────────────────────────────
ngrok:
	@echo "Starting ngrok tunnel..."
	bash scripts/ngrok-start.sh

# ─────────────────────────────────────────────
# Load Testing
# ─────────────────────────────────────────────
load-test:
	@echo "Running load test..."
	k6 run tests/load/load_test.js
