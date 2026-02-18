# Architecture Overview

This document describes the high-level system architecture of the WhatsApp Clone — a distributed, real-time messaging platform composed of eight Go microservices, an Android client, and a Kubernetes-based deployment layer.

---

## Design Principles

1. **Microservices with single responsibility** — each service owns one business domain and its data store.
2. **Event-driven** — NATS JetStream decouples write-path services from read-path consumers.
3. **Local-first client** — the Android app caches everything in Room; network is a sync layer, not the source of truth for UI.
4. **Polyglot persistence** — PostgreSQL for relational data, MongoDB for document data, Redis for ephemeral state, MinIO for binary objects.
5. **Observability built in** — every service emits Prometheus metrics, OpenTelemetry traces, and structured JSON logs.

---

## System Architecture Diagram

```
                         ┌───────────────────────┐
                         │    Android Client      │
                         │  (Kotlin · Compose)    │
                         └──────────┬─────────────┘
                                    │
                           HTTPS / WebSocket
                                    │
                         ┌──────────▼─────────────┐
                         │      API Gateway        │
                         │  ┌─────────────────┐    │
                         │  │ JWT Validation   │    │  NodePort :30080
                         │  │ Rate Limiting    │    │
                         │  │ Request Routing  │    │
                         │  └─────────────────┘    │
                         └──┬──┬──┬──┬──┬──┬──┬────┘
                            │  │  │  │  │  │  │
            ┌───────────────┘  │  │  │  │  │  └──────────────────┐
            ▼                  ▼  │  ▼  │  ▼                     ▼
    ┌──────────────┐  ┌─────────┐ │ ┌──────────┐ │ ┌──────────┐  ┌───────────────┐
    │ Auth Service │  │  User   │ │ │ Message  │ │ │  Media   │  │  WebSocket    │
    │              │  │ Service │ │ │ Service  │ │ │ Service  │  │  Service      │
    │ OTP · JWT    │  │Profiles │ │ │ CRUD     │ │ │ Upload   │  │ Real-time     │
    │ Tokens       │  │Contacts │ │ │ Status   │ │ │ Thumbs   │  │ Connections   │
    │              │  │Presence │ │ │ Search   │ │ │ URLs     │  │ Presence      │
    └──────┬───────┘  └────┬────┘ │ └─────┬────┘ │ └────┬─────┘  └───────┬───────┘
           │               │      │       │      │      │                │
           │               │      ▼       │      ▼      │                │
           │               │ ┌────────┐   │ ┌────────────┐               │
           │               │ │  Chat  │   │ │Notification│               │
           │               │ │Service │   │ │  Service   │               │
           │               │ │Groups  │   │ │  FCM Push  │               │
           │               │ │Perms   │   │ │  Batching  │               │
           │               │ └───┬────┘   │ └──────┬─────┘               │
           │               │     │        │        │                     │
    ═══════╪═══════════════╪═════╪════════╪════════╪═════════════════════╪════
           │               │     │        │        │                     │
           ▼               ▼     ▼        ▼        ▼                     ▼
    ┌────────────┐   ┌──────────┐  ┌──────────┐  ┌──────┐         ┌──────────┐
    │ PostgreSQL │   │  Redis   │  │  MongoDB  │  │ NATS │         │  MinIO   │
    │            │   │          │  │           │  │      │         │          │
    │ Users      │   │ OTP      │  │ Messages  │  │Events│         │ Media    │
    │ Chats      │   │ Presence │  │ Media     │  │Queue │         │ Files    │
    │ Contacts   │   │ Rate Lim │  │ Metadata  │  │      │         │          │
    │ Auth       │   │ Pub-Sub  │  │           │  │      │         │          │
    └────────────┘   └──────────┘  └───────────┘  └──────┘         └──────────┘
```

---

## Communication Patterns

### 1. Client ↔ API Gateway (REST + WebSocket)

All client traffic enters through the API Gateway on port `30080`. The gateway:
- Validates JWT tokens via gRPC call to `auth-service`
- Applies per-IP rate limiting via Redis
- Proxies requests to the appropriate downstream service
- Upgrades `/ws` connections to WebSocket and proxies to `websocket-service`

### 2. Service-to-Service (gRPC)

Internal services communicate synchronously via gRPC for request/response operations:

| Caller | Callee | Purpose |
|--------|--------|---------|
| api-gateway | auth-service | `ValidateToken` — JWT verification |
| message-service | chat-service | `CheckChatPermission`, `GetChatParticipants` |
| message-service | user-service | `GetUser`, `GetUsers` |
| chat-service | message-service | `GetLastMessages`, `GetUnreadCounts` |
| websocket-service | auth-service | `ValidateToken` on WS connect |
| websocket-service | message-service | `SendMessage`, `UpdateMessageStatus` |
| websocket-service | chat-service | `GetChatParticipants` |

Proto definitions live in `backend/proto/` and are compiled into Go stubs.

### 3. Event Streaming (NATS JetStream)

Asynchronous, durable event streaming decouples producers from consumers:

**Streams:**
- `MESSAGES` — subjects: `msg.new`, `msg.status.updated`, `msg.deleted`
- `CHATS` — subjects: `chat.created`, `chat.updated`, `group.member.added`, `group.member.removed`

**Publishers:**
- `chat-service` → `chat.*`, `group.*`
- `message-service` → `msg.*`

**Consumers (durable, manual ACK):**
- `websocket-service` — routes events to connected clients
- `notification-service` — sends FCM push for offline users

### 4. Real-Time Fan-Out (Redis Pub-Sub)

The `websocket-service` uses Redis pub-sub to fan out events across multiple service instances:

```
NATS event arrives
       │
       ▼
websocket-service instance A
       │
       ├─► Redis PUBLISH  user:channel:<userId>
       │
websocket-service instance B (subscribed)
       │
       └─► Deliver to local WebSocket connection
```

Each instance subscribes to Redis channels for the users it holds connections for. This allows horizontal scaling of WebSocket instances without sticky sessions.

### 5. Client WebSocket Protocol

The WebSocket connection carries a JSON event protocol:

**Client → Server:**
- `message.send` — send a message
- `message.delete` — delete a message
- `typing.start` / `typing.stop` — typing indicators
- `presence.subscribe` — subscribe to a user's presence

**Server → Client:**
- `message.new` — incoming message
- `message.status.updated` — delivery/read status change
- `message.deleted` — message deletion notification
- `chat.created` / `chat.updated` — chat lifecycle events
- `group.member.added` / `group.member.removed` — group changes
- `presence.updated` — online/offline transitions

---

## Data Flow: Sending a Message

A complete trace of sending a text message from one user to another:

```
1. User A taps Send
   └─► Android saves to Room (status: "pending", generates clientMsgId)
   └─► UI updates instantly (optimistic)

2. Android sends via WebSocket
   └─► { type: "message.send", chatId, content, clientMsgId }

3. WebSocket Service receives
   └─► gRPC → chat-service.CheckChatPermission (verify membership)
   └─► gRPC → message-service.SendMessage

4. Message Service processes
   └─► Stores in MongoDB
   └─► Publishes NATS event: msg.new { messageId, chatId, senderId, ... }
   └─► Returns messageId to WebSocket Service

5. WebSocket Service sends confirmation to User A
   └─► { type: "message.sent", clientMsgId, messageId }
   └─► Android updates Room: status "pending" → "sent"

6. NATS event (msg.new) consumed by:
   ├─► WebSocket Service
   │   └─► Looks up participants → Redis PUBLISH user:channel:<userB>
   │   └─► User B's WebSocket receives message.new event
   │   └─► Android inserts into Room → UI updates
   │
   └─► Notification Service
       └─► Checks Redis: is User B online?
       └─► If offline → sends FCM push notification

7. User B's device receives and auto-acknowledges delivery
   └─► message-service.UpdateMessageStatus(delivered)
   └─► NATS: msg.status.updated
   └─► User A sees double-check (delivered)

8. User B opens the chat and reads
   └─► POST /messages/read { messageIds }
   └─► message-service.UpdateMessageStatus(read)
   └─► NATS: msg.status.updated
   └─► User A sees blue double-check (read)
```

---

## Database Ownership

Each service owns its data — no shared databases:

| Service | PostgreSQL | MongoDB | Redis | MinIO | NATS |
|---------|:----------:|:-------:|:-----:|:-----:|:----:|
| auth-service | users, refresh_tokens | | OTP store | | |
| user-service | users, contacts, privacy, devices | | presence | | |
| chat-service | chats, participants, groups | | | | publish |
| message-service | | messages | | | publish |
| media-service | | media metadata | | media files | |
| notification-service | device_tokens (read) | | presence (read) | | consume |
| websocket-service | | | pub-sub, presence | | consume |
| api-gateway | | | rate limiting | | |

---

## Observability Stack

```
Services ──metrics──► Prometheus ──► Grafana (dashboards)
Services ──traces───► Jaeger (via OpenTelemetry)
Services ──logs────► stdout (structured JSON, collected by K8s)
```

- **Metrics**: each service exposes `/metrics` in Prometheus format
- **Tracing**: OpenTelemetry SDK propagates trace context across HTTP, gRPC, and NATS
- **Logging**: structured JSON via Zerolog, includes request IDs for correlation

---

## Security Model

1. **Authentication**: phone-based OTP → JWT access token (24h) + refresh token (stored in PostgreSQL)
2. **Token validation**: API Gateway validates every request via gRPC to auth-service; the validated `userId` is injected into downstream headers
3. **Rate limiting**: Redis-backed token bucket at the API Gateway
4. **Authorization**: services enforce ownership checks (e.g., "is this user a member of the chat?")
5. **Client-side**: tokens stored in Android EncryptedSharedPreferences
6. **Transport**: HTTPS in production, internal gRPC can use mTLS

---

## Scalability Considerations

| Component | Scaling Strategy |
|-----------|-----------------|
| API Gateway | Horizontal (stateless, behind load balancer) |
| Application Services | Horizontal (stateless, with replicas) |
| WebSocket Service | Horizontal (Redis pub-sub for fan-out) |
| PostgreSQL | Vertical, or read replicas |
| MongoDB | Replica set or sharding |
| Redis | Cluster mode |
| NATS | Cluster mode (built-in) |
| MinIO | Distributed mode |
