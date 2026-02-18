# WhatsApp Clone

A production-grade, real-time messaging platform built with a microservices backend (Go) and a modern Android client (Kotlin/Jetpack Compose). The system replicates core WhatsApp functionality — instant messaging, group chats, media sharing, typing indicators, read receipts, presence tracking, and push notifications — all running on Kubernetes.

---

## Table of Contents

- [Features](#features)
- [Architecture at a Glance](#architecture-at-a-glance)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [Getting Started](#getting-started)
- [Running the Backend](#running-the-backend)
- [Running the Android Client](#running-the-android-client)
- [Documentation](#documentation)
- [License](#license)

---

## Features

### Messaging
- Real-time 1:1 and group messaging over WebSocket
- Message types: text, image, video, audio, document, location
- Reply, forward, delete (for me / for everyone)
- Reactions (emoji)
- Star / bookmark messages
- Full-text search (in-chat and global)
- Disappearing messages (24h, 7d, 90d)

### Delivery & Status
- Three-tier delivery tracking: **sent** → **delivered** → **read**
- Read receipts with per-recipient granularity
- Typing indicators (real-time)
- Online/offline presence with last-seen timestamps

### Groups
- Group creation with admin/member roles
- Add/remove participants, role management
- Admin-only messaging mode
- Group avatars, names, descriptions

### Media
- Upload and share images, videos, audio, documents
- Server-side thumbnail generation (FFmpeg)
- Presigned download URLs via S3-compatible storage (MinIO)
- Orphan media cleanup

### Notifications
- Firebase Cloud Messaging (FCM) push notifications
- Presence-aware delivery (skip online users)
- Mute-aware (respects per-chat mute settings)
- Batched notifications (3s window to prevent storms)

### Offline & Sync
- Local-first architecture on the client (Room database)
- Optimistic UI updates — messages appear instantly
- Pending message queue with automatic retry on reconnect
- Full sync on reconnection (chats, messages, status)

### Security
- Phone-based OTP authentication
- JWT access + refresh token flow
- Encrypted token storage on device
- Rate limiting at the API gateway
- Input validation across all services

---

## Architecture at a Glance

```
┌─────────────┐         ┌──────────────────────────────────────────────────────┐
│  Android     │  HTTPS  │                   API Gateway                        │
│  Client      │────────▶│  (Auth · Rate Limiting · Routing · CORS)             │
│  (Compose)   │◀────────│  NodePort :30080                                     │
└──────┬───────┘   WS    └───────┬──────┬──────┬──────┬──────┬──────┬──────────┘
       │                         │      │      │      │      │      │
       │              ┌──────────┘  ┌───┘  ┌───┘  ┌───┘  ┌───┘  ┌───┘
       │              ▼             ▼      ▼      ▼      ▼      ▼
       │         ┌────────┐  ┌─────────┐ ┌────┐ ┌───────┐ ┌─────┐ ┌──────────┐
       │         │  Auth  │  │  User   │ │Chat│ │Message│ │Media│ │Notifica- │
       │         │Service │  │ Service │ │Svc │ │Service│ │ Svc │ │tion Svc  │
       │         └───┬────┘  └────┬────┘ └──┬─┘ └───┬───┘ └──┬──┘ └────┬─────┘
       │             │            │         │       │        │         │
       │         ┌───▼────────────▼─────────▼───────▼────────▼─────────▼──┐
       │         │              Infrastructure                             │
       │         │  PostgreSQL · MongoDB · Redis · NATS · MinIO            │
       │         └─────────────────────────────────────────────────────────┘
       │
       │    WebSocket
       └───────────────▶  WebSocket Service  ◀──── NATS events
                          (Redis Pub-Sub fan-out)
```

**Communication patterns:**
| Pattern | Used For |
|---------|----------|
| REST / HTTP | Client → API Gateway → Services |
| gRPC | Service-to-service (internal) |
| NATS JetStream | Event streaming (async, durable) |
| Redis Pub-Sub | Real-time fan-out to WebSocket instances |
| WebSocket | Bidirectional real-time client ↔ server |

---

## Tech Stack

### Backend
| Component | Technology |
|-----------|------------|
| Language | Go 1.22+ |
| HTTP Framework | Gin |
| RPC Framework | gRPC + Protocol Buffers |
| Relational DB | PostgreSQL 16 |
| Document DB | MongoDB 7 |
| Cache / Pub-Sub | Redis 7 |
| Message Broker | NATS JetStream |
| Object Storage | MinIO (S3-compatible) |
| Auth | JWT (access + refresh) with OTP |
| Push Notifications | Firebase Cloud Messaging |
| Observability | Prometheus, Grafana, Jaeger (OpenTelemetry) |
| Deployment | Kubernetes (Helm), Docker |

### Client (Android)
| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Local Database | Room (SQLite) |
| Networking | Retrofit + OkHttp |
| Real-Time | OkHttp WebSocket |
| DI | Hilt (Dagger) |
| Navigation | Jetpack Navigation Compose |
| Serialization | Kotlinx Serialization |
| Image Loading | Coil |

---

## Repository Structure

```
.
├── backend/
│   ├── api-gateway/           # Single entry point — auth, rate limiting, routing
│   ├── auth-service/          # OTP, JWT tokens, session management
│   ├── user-service/          # Profiles, contacts, presence, privacy
│   ├── chat-service/          # Chat & group management, participants
│   ├── message-service/       # Message CRUD, delivery status, search
│   ├── media-service/         # File upload, thumbnails, presigned URLs
│   ├── notification-service/  # FCM push notifications
│   ├── websocket-service/     # Real-time WebSocket connections
│   ├── pkg/                   # Shared Go packages (logger, metrics, tracing, etc.)
│   ├── proto/                 # gRPC .proto definitions
│   ├── migrations/            # PostgreSQL & MongoDB migration scripts
│   ├── helm/                  # Kubernetes Helm chart
│   ├── observability/         # Prometheus & Grafana configuration
│   ├── tests/                 # Integration tests
│   └── docker-compose.yml     # Local development environment
│
├── client/                    # Android app (Kotlin / Jetpack Compose)
│   ├── app/                   # Main application module, navigation, DI
│   ├── core/
│   │   ├── common/            # Shared utilities, result types
│   │   ├── database/          # Room DB, entities, DAOs
│   │   ├── network/           # Retrofit APIs, WebSocket, token management
│   │   └── ui/                # Shared composables, theme
│   └── feature/
│       ├── auth/              # Login, OTP, profile setup
│       ├── chat/              # Chat list, chat detail, messaging
│       ├── contacts/          # Contact picker, contact info
│       ├── group/             # Group creation, info, participants
│       ├── media/             # Media viewer
│       ├── profile/           # Profile editing
│       └── settings/          # App settings, privacy, notifications
│
├── docs/                      # Project documentation
└── LICENSE
```

---

## Getting Started

### Prerequisites

- **Go** 1.22+
- **Docker** & **Docker Compose** (for local development)
- **kubectl** & **Helm 3** (for Kubernetes deployment)
- **Android Studio** Hedgehog+ (for the client app)
- **JDK 17**

### Running the Backend

#### Option 1: Docker Compose (Local Development)

```bash
cd backend
docker-compose up --build
```

This starts all 8 services plus PostgreSQL, MongoDB, Redis, NATS, and MinIO.

The API gateway is available at `http://localhost:8080`.

#### Option 2: Kubernetes (Helm)

```bash
cd backend/helm

# Install the chart
helm install whatsapp ./whatsapp -n whatsapp --create-namespace

# Verify pods
kubectl get pods -n whatsapp
```

The API gateway is exposed via NodePort on port `30080`.

### Running the Android Client

1. Open the `client/` directory in Android Studio.
2. Sync Gradle and let dependencies download.
3. Update the server URL in the app settings (or via the debug settings screen) to point to your backend.
4. Build and run on an emulator or physical device.

---

## Documentation

Detailed documentation is available in the [`docs/`](docs/) directory:

| Document | Description |
|----------|-------------|
| [Architecture Overview](docs/architecture.md) | System design, communication patterns, data flow |
| [Backend Services](docs/backend-services.md) | Deep dive into each microservice |
| [Client App](docs/client-app.md) | Android app architecture, modules, data flow |
| [Infrastructure](docs/infrastructure.md) | Kubernetes, Helm, storage, networking |
| [API Reference](docs/api-reference.md) | Complete REST and WebSocket API documentation |

---

## License

This project is available under the terms specified in the [LICENSE](LICENSE) file.
