# Infrastructure & Deployment

This document covers the Kubernetes deployment architecture, Helm chart configuration, storage, networking, observability, and operational considerations for the WhatsApp Clone platform.

---

## Deployment Overview

The entire platform runs in a single Kubernetes namespace (`whatsapp`) with:
- **8 application services** as Deployments
- **4 stateful infrastructure components** as StatefulSets (PostgreSQL, MongoDB, NATS, MinIO)
- **1 stateless infrastructure component** as a Deployment (Redis)
- Persistent storage via PersistentVolumeClaims
- Configuration via ConfigMaps and Secrets
- External access via NodePort

```
┌──────────────────────── whatsapp namespace ─────────────────────────┐
│                                                                      │
│  Deployments (stateless)          StatefulSets (stateful)            │
│  ┌──────────────────────┐        ┌───────────────────────┐          │
│  │ api-gateway       ×1 │        │ postgres-0         ×1 │          │
│  │ auth-service      ×1 │        │  └─ PVC: 5Gi          │          │
│  │ user-service      ×1 │        │ mongodb-0          ×1 │          │
│  │ chat-service      ×1 │        │  └─ PVC: 5Gi          │          │
│  │ message-service   ×1 │        │ nats-0             ×1 │          │
│  │ media-service     ×1 │        │  └─ PVC: 1Gi          │          │
│  │ notification-svc  ×1 │        │ minio-0            ×1 │          │
│  │ websocket-svc     ×1 │        │  └─ PVC: 10Gi         │          │
│  │ redis             ×1 │        └───────────────────────┘          │
│  └──────────────────────┘                                            │
│                                                                      │
│  ConfigMap: whatsapp-config      Secret: whatsapp-secrets            │
└──────────────────────────────────────────────────────────────────────┘
         │
    NodePort :30080 (api-gateway)
         │
    External traffic
```

---

## Helm Chart Structure

The Helm chart lives at `backend/helm/whatsapp/`:

```
helm/whatsapp/
├── Chart.yaml                    # Chart metadata
├── values.yaml                   # Default configuration values
└── templates/
    ├── _helpers.tpl              # Template helpers
    ├── namespace.yaml            # Namespace creation
    ├── configmap.yaml            # Shared configuration
    ├── secrets.yaml              # Sensitive values
    │
    ├── api-gateway/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── auth-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── user-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── chat-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── message-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── notification-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── media-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── websocket-service/
    │   ├── deployment.yaml
    │   └── service.yaml
    │
    ├── postgres/
    │   ├── statefulset.yaml
    │   ├── service.yaml
    │   └── pvc.yaml
    ├── mongodb/
    │   ├── statefulset.yaml
    │   ├── service.yaml
    │   └── pvc.yaml
    ├── redis/
    │   ├── deployment.yaml
    │   └── service.yaml
    ├── nats/
    │   ├── statefulset.yaml
    │   └── service.yaml
    └── minio/
        ├── statefulset.yaml
        ├── service.yaml
        └── pvc.yaml
```

### Installing

```bash
cd backend/helm

# Fresh install
helm install whatsapp ./whatsapp -n whatsapp --create-namespace

# Upgrade after changes
helm upgrade whatsapp ./whatsapp -n whatsapp

# Uninstall
helm uninstall whatsapp -n whatsapp
```

---

## Service Discovery & Networking

### Internal DNS

All services discover each other via Kubernetes DNS. Within the `whatsapp` namespace, services are addressable by short name:

| Service | DNS Name | Port(s) |
|---------|----------|---------|
| api-gateway | `api-gateway` | 8080 |
| auth-service | `auth-service` | 8081 (HTTP), 9081 (gRPC) |
| user-service | `user-service` | 8082 (HTTP), 9082 (gRPC) |
| chat-service | `chat-service` | 8083 (HTTP), 9083 (gRPC) |
| message-service | `message-service` | 8084 (HTTP), 9084 (gRPC) |
| notification-service | `notification-service` | 8085 |
| media-service | `media-service` | 8086 (HTTP), 9086 (gRPC) |
| websocket-service | `websocket-service` | 8087 |
| postgres | `postgres` | 5432 |
| mongodb | `mongodb` | 27017 |
| redis | `redis` | 6379 |
| nats | `nats` | 4222 (client), 8222 (monitor) |
| minio | `minio` | 9000 (API), 9001 (console) |

Full FQDN format: `<service>.<namespace>.svc.cluster.local` (e.g., `postgres.whatsapp.svc.cluster.local`)

### External Access

The API Gateway is the only service exposed externally, via NodePort:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
spec:
  type: NodePort
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30080
  selector:
    app: api-gateway
```

The Android client connects to `http://<node-ip>:30080`.

### Service Types

| Component | K8s Service Type | Reason |
|-----------|-----------------|--------|
| api-gateway | NodePort | External access for clients |
| All other services | ClusterIP | Internal-only communication |

---

## Persistent Storage

### PersistentVolumeClaims

| Component | PVC Name | Size | Mount Path | Access Mode |
|-----------|----------|------|------------|-------------|
| PostgreSQL | `postgres-data` | 5Gi | `/var/lib/postgresql/data` | ReadWriteOnce |
| MongoDB | `mongodb-data` | 5Gi | `/data/db` | ReadWriteOnce |
| NATS | (volumeClaimTemplate) | 1Gi | `/data` | ReadWriteOnce |
| MinIO | `minio-data` | 10Gi | `/data` | ReadWriteOnce |

**Note**: Redis runs without persistent storage. Its data (rate limits, OTPs, presence, typing indicators) is ephemeral by design — a Redis restart means users need to re-authenticate WebSocket connections and presence data rebuilds from heartbeats.

### Why These Storage Sizes?

- **PostgreSQL (5Gi)**: stores user accounts, chats, contacts, and group metadata. Row data is small; 5Gi handles millions of users for dev/staging.
- **MongoDB (5Gi)**: stores messages and media metadata. Messages are compact documents; media binaries live in MinIO.
- **NATS (1Gi)**: JetStream needs persistent storage for durable streams. 1Gi is sufficient for the message and chat event streams with appropriate retention policies.
- **MinIO (10Gi)**: stores actual media files (images, videos, audio, documents). 10Gi is a starting point — production deployments should scale this significantly.

---

## Configuration

### ConfigMap (`whatsapp-config`)

Shared, non-sensitive configuration:

```yaml
data:
  LOG_LEVEL: "debug"
  LOG_FORMAT: "pretty"
```

### Secrets (`whatsapp-secrets`)

Sensitive values (base64-encoded):

```yaml
data:
  postgres-password: <base64>
  jwt-secret: <base64>
  minio-access-key: <base64>
  minio-secret-key: <base64>
  redis-password: <base64>
```

### Per-Service Environment Variables

Each service deployment injects specific environment variables:

**API Gateway:**
```yaml
env:
  - name: AUTH_SERVICE_GRPC_ADDR
    value: "auth-service:9081"
  - name: AUTH_SERVICE_HTTP_ADDR
    value: "http://auth-service:8081"
  - name: USER_SERVICE_ADDR
    value: "http://user-service:8082"
  - name: CHAT_SERVICE_ADDR
    value: "http://chat-service:8083"
  - name: MESSAGE_SERVICE_ADDR
    value: "http://message-service:8084"
  - name: MEDIA_SERVICE_ADDR
    value: "http://media-service:8086"
  - name: WEBSOCKET_SERVICE_ADDR
    value: "http://websocket-service:8087"
  - name: REDIS_ADDR
    value: "redis:6379"
```

**Auth Service:**
```yaml
env:
  - name: POSTGRES_DSN
    value: "postgres://whatsapp:$(POSTGRES_PASSWORD)@postgres:5432/whatsapp_auth?sslmode=disable"
  - name: REDIS_ADDR
    value: "redis:6379"
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: whatsapp-secrets
        key: jwt-secret
```

---

## Resource Limits

### Application Services

All application services share the same resource profile:

```yaml
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 250m
    memory: 256Mi
```

### Infrastructure Components

Infrastructure gets more resources due to I/O and memory demands:

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|:-----------:|:---------:|:--------------:|:------------:|
| PostgreSQL | 250m | 500m | 256Mi | 512Mi |
| MongoDB | 250m | 500m | 256Mi | 512Mi |
| Redis | 100m | 250m | 128Mi | 256Mi |
| NATS | 100m | 250m | 128Mi | 256Mi |
| MinIO | 250m | 500m | 256Mi | 512Mi |

---

## Health Checks

Every component has liveness and readiness probes:

### Application Services

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080          # varies per service
  initialDelaySeconds: 15
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

### Infrastructure

| Component | Liveness Probe | Readiness Probe |
|-----------|---------------|-----------------|
| PostgreSQL | `exec: pg_isready` | `exec: pg_isready` |
| MongoDB | `exec: mongosh --eval "db.adminCommand('ping')"` | Same |
| Redis | `exec: redis-cli ping` | Same |
| NATS | `httpGet: /healthz` (port 8222) | Same |
| MinIO | `httpGet: /minio/health/live` | `httpGet: /minio/health/ready` |

---

## Infrastructure Component Details

### PostgreSQL

**Image**: `postgres:16-alpine`
**Used by**: auth-service, user-service, chat-service, notification-service (read-only)

PostgreSQL handles all relational data where ACID guarantees matter:
- **User accounts and authentication**: referential integrity between users and refresh tokens
- **Chat structure**: foreign keys between chats, participants, and groups ensure consistency
- **Contact relationships**: the social graph (who knows who, who blocked who)
- **Privacy settings**: per-user configuration

The database is initialized with migration scripts from `backend/migrations/` that create tables, indexes, and seed data.

### MongoDB

**Image**: `mongo:7`
**Used by**: message-service, media-service

MongoDB handles document-oriented, high-volume data:
- **Messages**: each message is a self-contained document with embedded status maps and reactions. The flexible schema accommodates different message types without schema migrations.
- **Media metadata**: media files have varying attributes (dimensions for images, duration for audio/video) that fit naturally into documents.

### Redis

**Image**: `redis:7-alpine`
**Used by**: api-gateway, auth-service, user-service, websocket-service, notification-service

Redis serves multiple purposes across the system:

| Purpose | Key Pattern | TTL | Used By |
|---------|-------------|-----|---------|
| Rate limiting | `ratelimit:<ip>` | Sliding window | api-gateway |
| OTP storage | `otp:<phone>` | 5 min | auth-service |
| Presence | `presence:<userId>` | 5 min | user-service, notification-service |
| Pub-sub channels | `user:channel:<userId>` | N/A (pub-sub) | websocket-service |
| Typing indicators | `typing:<chatId>:<userId>` | 5 sec | websocket-service |

**Why no persistence**: every data item in Redis is either ephemeral by nature (presence, typing) or reconstructible (rate limit counters reset harmlessly). Losing Redis data on restart is acceptable — OTPs can be re-requested, presence rebuilds from heartbeats, and rate limits reset.

### NATS JetStream

**Image**: `nats:2-alpine`
**Used by**: chat-service (publish), message-service (publish), websocket-service (consume), notification-service (consume)

NATS JetStream provides durable, at-least-once event delivery:

**Streams:**
```
MESSAGES stream
├── msg.new                    # New message created
├── msg.status.updated         # Delivery status changed
└── msg.deleted                # Message deleted

CHATS stream
├── chat.created               # New chat created
├── chat.updated               # Chat metadata updated
├── group.member.added         # Member added to group
└── group.member.removed       # Member removed from group
```

**Consumer groups** ensure each event is processed once per consumer type:
- `ws-consumer` — websocket-service processes events for real-time delivery
- `notif-consumer` — notification-service processes events for push notifications

**Durability**: consumers use manual acknowledgment. If a consumer crashes before ACK, the event is redelivered. JetStream data persists to the 1Gi PVC.

### MinIO

**Image**: `minio/minio:latest`
**Used by**: media-service

MinIO provides S3-compatible object storage for media files:
- **Bucket**: `whatsapp-media`
- **API port**: 9000 (S3-compatible)
- **Console port**: 9001 (web UI for management)

Files are stored with the key pattern: `<mediaId>/<filename>`. Presigned URLs with configurable TTL allow clients to download directly from MinIO without proxying through the application.

---

## Local Development: Docker Compose

For local development, `backend/docker-compose.yml` spins up the entire stack:

```bash
cd backend
docker-compose up --build
```

This starts all 8 services, all 5 infrastructure components, and configures the network (`whatsapp-net`) so services can communicate by name.

The API Gateway is available at `http://localhost:8080`.

Key differences from the Kubernetes deployment:
- No resource limits (uses whatever the Docker daemon allows)
- No persistent volume management (uses Docker volumes)
- No health check-based orchestration
- All services share a single Docker network

---

## Observability

### Prometheus + Grafana

The `backend/observability/` directory contains:
- **Prometheus configuration**: scrape targets for all services' `/metrics` endpoints
- **Grafana dashboards**: pre-configured dashboards for service health, request rates, latencies, and error rates

All Go services expose Prometheus-compatible metrics at `/metrics`:
- HTTP request duration histograms
- Request count by status code
- gRPC method duration
- Active WebSocket connections
- NATS consumer lag
- Database connection pool stats

### Distributed Tracing (OpenTelemetry + Jaeger)

Each service initializes an OpenTelemetry tracer that exports spans to Jaeger. Trace context propagates across:
- HTTP headers (`traceparent`)
- gRPC metadata
- NATS message headers

A single user action (e.g., sending a message) generates a trace spanning: API Gateway → Message Service → Chat Service (gRPC) → MongoDB write → NATS publish.

### Structured Logging

All services use Zerolog for structured JSON logging:
```json
{
  "level": "info",
  "time": "2026-02-18T12:00:00Z",
  "request_id": "abc-123",
  "user_id": "user-456",
  "method": "POST",
  "path": "/api/v1/messages",
  "status": 201,
  "latency_ms": 45,
  "message": "request completed"
}
```

The `request_id` field enables correlation across services for a single client request.

---

## Production Considerations

The current setup is optimized for development and testing. For production, consider:

| Area | Current | Production Recommendation |
|------|---------|--------------------------|
| **External access** | NodePort :30080 | Ingress controller (NGINX/Traefik) with TLS termination |
| **Scaling** | Fixed replicas (1 each) | HPA based on CPU/memory/custom metrics |
| **TLS** | None (plaintext) | TLS everywhere — Ingress, gRPC mTLS, encrypted Redis |
| **Secrets** | K8s Secrets (base64) | External secrets manager (Vault, AWS Secrets Manager) |
| **Database** | Single-instance StatefulSets | Managed services or clustered deployments with replicas |
| **Backups** | None | Automated PVC snapshots, pg_dump, mongodump schedules |
| **Monitoring** | Prometheus/Grafana in docker-compose | Dedicated monitoring namespace, alerting rules |
| **Network policies** | None | Restrict pod-to-pod traffic to only required paths |
| **Image registry** | Local builds | Private container registry with vulnerability scanning |
| **CI/CD** | Manual | GitOps (ArgoCD/Flux) with automated image builds |
