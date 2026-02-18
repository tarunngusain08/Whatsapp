# Backend Services — Deep Dive

This document provides an exhaustive breakdown of every microservice in the backend, covering its purpose, technology, endpoints, data models, inter-service communication, and why it exists as a separate service.

---

## Service Map

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Running Pods (K8s)                           │
├─────────────────────────┬───────────┬───────────────────────────────┤
│  Application Services   │   Ports   │  Infrastructure               │
├─────────────────────────┼───────────┼───────────────────────────────┤
│  api-gateway            │ 8080      │  postgres-0        (5432)     │
│  auth-service           │ 8081/9081 │  mongodb-0         (27017)    │
│  user-service           │ 8082/9082 │  redis             (6379)     │
│  chat-service           │ 8083/9083 │  nats-0            (4222)     │
│  message-service        │ 8084/9084 │  minio-0           (9000)     │
│  notification-service   │ 8085      │                               │
│  media-service          │ 8086/9086 │                               │
│  websocket-service      │ 8087      │                               │
└─────────────────────────┴───────────┴───────────────────────────────┘
```

All application services expose HTTP for external consumption (via the gateway) and gRPC for internal service-to-service calls. The exceptions are `notification-service` (HTTP health only, it's a pure NATS consumer) and `websocket-service` (HTTP + WebSocket upgrade, no gRPC server).

---

## 1. API Gateway

**Port**: 8080 (HTTP)
**Framework**: Gin
**Databases**: Redis (rate limiting)

### Why It Exists

The API Gateway is the single entry point for all client traffic. Without it, the Android client would need to know the address of every backend service and handle authentication independently per request. The gateway centralizes three critical cross-cutting concerns:

1. **Authentication**: every request (except `/auth/*`) is validated by calling `auth-service.ValidateToken` via gRPC. The validated `userId` is injected into the `X-User-ID` header before proxying.
2. **Rate limiting**: a Redis-backed token-bucket algorithm prevents abuse. Each IP gets a configurable request budget.
3. **Routing**: a single URL namespace (`/api/v1/...`) is mapped to the correct downstream service using reverse proxying.

### Request Flow

```
Client Request
    │
    ▼
CORS middleware
    │
    ▼
Rate Limiter (Redis)
    │   ┌──── Exceeded? → 429 Too Many Requests
    ▼
Auth Middleware
    │   ┌──── /auth/* paths? → Skip validation (public)
    │   └──── gRPC → auth-service.ValidateToken
    │          ├── Invalid → 401 Unauthorized
    │          └── Valid → inject X-User-ID header
    ▼
Route Matching
    │
    ▼
Reverse Proxy → downstream service
```

### Route Table

| Gateway Path | Downstream Service | Downstream Path |
|--------------|--------------------|-----------------|
| `/api/v1/auth/*` | auth-service:8081 | `/api/v1/auth/*` |
| `/api/v1/users/*` | user-service:8082 | `/api/v1/users/*` |
| `/api/v1/chats/*` | chat-service:8083 | `/api/v1/chats/*` |
| `/api/v1/chats/:chatId/messages/*` | message-service:8084 | `/api/v1/messages/*` |
| `/api/v1/messages/*` | message-service:8084 | `/api/v1/messages/*` |
| `/api/v1/media/*` | media-service:8086 | `/api/v1/media/*` |
| `/api/v1/notifications/devices/*` | user-service:8082 | `/api/v1/users/devices/*` |
| `/ws` | websocket-service:8087 | `/ws` |
| `/health` | self | Health check |
| `/metrics` | self | Prometheus metrics |

### Additional Features

- **CORS**: configurable allowed origins, methods, and headers
- **Request ID propagation**: generates or forwards `X-Request-ID` for distributed tracing
- **Body size limits**: prevents oversized payloads from reaching downstream services
- **OpenTelemetry**: trace context is propagated to all downstream calls

---

## 2. Auth Service

**Ports**: 8081 (HTTP), 9081 (gRPC)
**Framework**: Gin + gRPC
**Databases**: PostgreSQL (users, refresh tokens), Redis (OTP storage)

### Why It Exists

Authentication is isolated into its own service because:
- **Security boundary**: token signing keys and OTP logic are contained; other services never touch credentials directly.
- **Independent scaling**: auth verification (via gRPC `ValidateToken`) is called on every single API request — it can be scaled independently.
- **Single source of truth**: JWT validation logic lives in one place, preventing divergence across services.

### How Authentication Works

```
1. User enters phone number
   └─► POST /auth/request-otp { phone: "+1234567890" }
   └─► Service generates 6-digit OTP
   └─► Stores in Redis with 5-minute TTL: key=otp:<phone>, value=<code>
   └─► (In production: sends via SMS; in dev: logged to console)

2. User enters OTP
   └─► POST /auth/verify-otp { phone, code }
   └─► Compares against Redis
   └─► If valid:
       ├─► Creates user in PostgreSQL (if first login)
       ├─► Generates JWT access token (24h expiry)
       ├─► Generates refresh token (stored as hash in PostgreSQL)
       └─► Returns { accessToken, refreshToken, user }

3. Token refresh
   └─► POST /auth/refresh { refreshToken }
   └─► Validates hash against PostgreSQL
   └─► Issues new access token
   └─► Optionally rotates refresh token

4. Every subsequent API call
   └─► API Gateway calls gRPC ValidateToken(accessToken)
   └─► Auth service verifies JWT signature and expiry
   └─► Returns { userId, phone }
```

### Data Models

**PostgreSQL — `users` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| phone | VARCHAR(20) | Unique, indexed |
| created_at | TIMESTAMP | Account creation time |

**PostgreSQL — `refresh_tokens` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| token_hash | VARCHAR(255) | SHA-256 hash of the token |
| user_id | UUID | FK → users |
| expires_at | TIMESTAMP | Expiration time |
| created_at | TIMESTAMP | Creation time |

**Redis — OTP storage:**
| Key | Value | TTL |
|-----|-------|-----|
| `otp:<phone>` | 6-digit code | 5 minutes |

### gRPC Interface

```protobuf
service AuthService {
  rpc ValidateToken(ValidateTokenRequest) returns (ValidateTokenResponse);
}

message ValidateTokenRequest {
  string token = 1;
}

message ValidateTokenResponse {
  string user_id = 1;
  string phone = 2;
}
```

---

## 3. User Service

**Ports**: 8082 (HTTP), 9082 (gRPC)
**Framework**: Gin + gRPC
**Databases**: PostgreSQL (profiles, contacts, privacy, devices), Redis (presence)

### Why It Exists

The user service owns everything about a user's identity and social graph. It's separate from auth because authentication (proving who you are) is a different concern from user management (your profile, contacts, settings). Separating them allows:
- Profile updates without touching auth logic
- Contact sync to scale independently (heavy phone-number matching queries)
- Presence tracking to be served from Redis at high throughput

### Key Responsibilities

#### Profile Management
Users can set their display name, status text, and avatar. The avatar URL points to MinIO (uploaded via media-service).

#### Contact Sync
The client sends a list of phone numbers from the device's address book. The service matches them against registered users and returns the intersection — showing the user which contacts are on the platform.

```
Client sends: ["+1111", "+2222", "+3333", "+4444"]
                                │
                                ▼
    PostgreSQL query: SELECT * FROM users WHERE phone IN (...)
                                │
                                ▼
    Matches found: ["+1111" → userId1, "+3333" → userId3]
                                │
                                ▼
    Response: [{ userId1, "+1111", "Alice" }, { userId3, "+3333", "Bob" }]
    + Upserts into contacts table for the requesting user
```

#### Presence Tracking
Online status is ephemeral — stored in Redis with a 5-minute TTL:

```
Key: presence:<userId>
Value: { "online": true, "last_seen": "2026-02-18T12:00:00Z" }
TTL: 300 seconds (refreshed by WebSocket heartbeats)
```

When the TTL expires (user disconnects), the presence naturally disappears, and the user appears "offline."

#### Privacy Settings
Per-user controls for who can see their last-seen, profile photo, about text, and whether read receipts are sent.

#### Device Tokens
Stores FCM device tokens for push notifications. Each user can have multiple devices registered.

### Data Models

**PostgreSQL — `users` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| phone | VARCHAR(20) | Unique phone number |
| display_name | VARCHAR(100) | Profile name |
| avatar_url | TEXT | URL to avatar in MinIO |
| status_text | VARCHAR(500) | "About" text |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |

**PostgreSQL — `contacts` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| user_id | UUID | The user who owns this contact |
| contact_user_id | UUID | The contact's user ID |
| is_blocked | BOOLEAN | Block status |

**PostgreSQL — `privacy_settings` table:**
| Column | Type | Description |
|--------|------|-------------|
| user_id | UUID | FK → users |
| last_seen | ENUM | everyone / contacts / nobody |
| profile_photo | ENUM | everyone / contacts / nobody |
| about | ENUM | everyone / contacts / nobody |
| read_receipts | BOOLEAN | Enable/disable |

**PostgreSQL — `device_tokens` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| user_id | UUID | FK → users |
| token | TEXT | FCM registration token |
| platform | VARCHAR(20) | "android" / "ios" / "web" |

---

## 4. Chat Service

**Ports**: 8083 (HTTP), 9083 (gRPC)
**Framework**: Gin + gRPC
**Databases**: PostgreSQL (chats, participants, groups), NATS (event publishing)

### Why It Exists

Chat management is one of the most permission-sensitive domains. The chat service answers questions like "does this user belong to this chat?", "is this user an admin?", and "is this a direct or group chat?" These checks are called by multiple other services (message-service, websocket-service) via gRPC before allowing any operation.

Keeping chat management separate from message storage allows:
- Permission checks to be cached and optimized independently
- Group management logic (roles, invitations, settings) to evolve without touching message storage
- Chat-level settings (mute, pin, disappearing timer) to be distinct from message-level data

### Chat Types

**Direct Chat (1:1):**
- Exactly 2 participants
- Created when either user initiates
- Deduplicated — creating a chat with an existing partner returns the existing chat

**Group Chat:**
- 2–256 participants
- Has a `groups` table row with name, description, avatar, creator
- Roles: `admin` (can manage members, edit group info) and `member`
- Optional admin-only messaging mode

### NATS Events Published

These events trigger downstream actions in the websocket-service and notification-service:

| Event | Subject | Trigger | Payload |
|-------|---------|---------|---------|
| Chat created | `chat.created` | New direct or group chat | chatId, type, participants |
| Chat updated | `chat.updated` | Name/description/avatar change | chatId, updated fields |
| Member added | `group.member.added` | Admin adds member | chatId, userId, addedBy |
| Member removed | `group.member.removed` | Admin removes member or member leaves | chatId, userId |

### Key gRPC Methods

```protobuf
service ChatService {
  rpc GetChatParticipants(GetParticipantsRequest) returns (GetParticipantsResponse);
  rpc IsMember(IsMemberRequest) returns (IsMemberResponse);
  rpc CheckChatPermission(CheckPermissionRequest) returns (CheckPermissionResponse);
}
```

`CheckChatPermission` is called by `message-service` before storing every message — it verifies the sender is a member and, for admin-only groups, that they are an admin.

### Data Models

**PostgreSQL — `chats` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| type | ENUM | direct / group |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |

**PostgreSQL — `chat_participants` table:**
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| chat_id | UUID | FK → chats |
| user_id | UUID | FK → users |
| role | ENUM | admin / member |
| is_muted | BOOLEAN | Mute status |
| mute_until | TIMESTAMP | Mute expiry (nullable) |
| is_pinned | BOOLEAN | Pin status |
| auto_delete_timer | INTEGER | Disappearing message timer (seconds) |
| joined_at | TIMESTAMP | When the user joined |

**PostgreSQL — `groups` table:**
| Column | Type | Description |
|--------|------|-------------|
| chat_id | UUID | FK → chats (1:1) |
| name | VARCHAR(100) | Group name |
| description | TEXT | Group description |
| avatar_url | TEXT | Group avatar URL |
| created_by | UUID | Creator's user ID |
| is_admin_only | BOOLEAN | Only admins can send messages |

---

## 5. Message Service

**Ports**: 8084 (HTTP), 9084 (gRPC)
**Framework**: Gin + gRPC
**Databases**: MongoDB (messages), NATS (event publishing)

### Why It Exists

Messages are the highest-volume data in a messaging app. Choosing MongoDB gives:
- **Schema flexibility**: different message types (text, image, location) have different payload shapes
- **High write throughput**: append-heavy workload suits MongoDB's write path
- **Efficient pagination**: cursor-based pagination on `created_at` timestamps
- **Document-level status tracking**: each message embeds a `status` map with per-recipient delivery states

Isolating messages from chats means the message store can be sharded by `chatId` without affecting relational chat/participant data in PostgreSQL.

### Message Lifecycle

```
1. Client sends message
   └─► POST /messages { chatId, type: "text", content: "Hello" }

2. Message Service
   ├─► gRPC → chat-service.CheckChatPermission
   │   └─► Verifies sender is a chat member
   ├─► Generates unique messageId
   ├─► Stores in MongoDB with status: { <recipientId>: "sent" } for each participant
   ├─► Publishes NATS: msg.new
   └─► Returns messageId

3. Recipient's device comes online or receives via WebSocket
   └─► Auto-sends delivery acknowledgment
   └─► PUT /messages/status { messageId, status: "delivered" }
   └─► Publishes NATS: msg.status.updated

4. Recipient reads the message
   └─► POST /messages/read { messageIds: [...] }
   └─► Updates status to "read" for that recipient
   └─► Publishes NATS: msg.status.updated
```

### Message Types

| Type | Payload Fields |
|------|---------------|
| `text` | content (string) |
| `image` | mediaId, mediaUrl, thumbnailUrl, width, height |
| `video` | mediaId, mediaUrl, thumbnailUrl, width, height, durationMs |
| `audio` | mediaId, mediaUrl, durationMs |
| `document` | mediaId, mediaUrl, fileName, fileSize |
| `location` | latitude, longitude, name, address |

### Features

- **Reply**: messages can reference a `replyToId`
- **Forward**: copies message content to another chat
- **Delete**: "delete for me" (hides) vs. "delete for everyone" (marks `isDeleted`)
- **Reactions**: emoji reactions stored as a map `{ userId: emoji }`
- **Star**: per-user bookmarking via `isStarredBy` array
- **Search**: text search within a chat or globally across user's chats (MongoDB text index)
- **Disappearing messages**: a background job runs every 6 hours, deleting messages older than the chat's `auto_delete_timer`

### Data Model

**MongoDB — `messages` collection:**
```json
{
  "_id": "ObjectId",
  "message_id": "uuid",
  "chat_id": "uuid",
  "sender_id": "uuid",
  "type": "text | image | video | audio | document | location",
  "payload": {
    "content": "Hello world",
    "media_id": "uuid (if media)",
    "media_url": "https://... (if media)"
  },
  "reply_to_id": "uuid | null",
  "forwarded_from": "uuid | null",
  "status": {
    "<recipientUserId>": {
      "status": "sent | delivered | read",
      "updated_at": "timestamp"
    }
  },
  "reactions": {
    "<userId>": "👍"
  },
  "is_deleted": false,
  "is_starred_by": ["userId1", "userId2"],
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```

---

## 6. Media Service

**Ports**: 8086 (HTTP), 9086 (gRPC)
**Framework**: Gin + gRPC
**Databases**: MongoDB (metadata), MinIO (file storage)

### Why It Exists

Media handling is compute-intensive (thumbnail generation, video transcoding) and storage-intensive (binary files). Isolating it into its own service means:
- Media processing doesn't block message delivery
- Storage can scale independently (MinIO distributed mode)
- Presigned URLs keep binary traffic off the application servers — clients download directly from MinIO

### Upload Flow

```
1. Client uploads file
   └─► POST /media/upload (multipart/form-data)
       ├─► file: binary data
       ├─► type: "image" | "video" | "audio" | "document"
       └─► (optional) chat_id for context

2. Media Service processes
   ├─► Validates file type and size
   ├─► Uploads to MinIO bucket: whatsapp-media/<mediaId>/<filename>
   ├─► For images: generates thumbnail (resized)
   ├─► For videos: generates thumbnail (FFmpeg, first frame)
   ├─► Stores metadata in MongoDB
   └─► Returns { mediaId, url, thumbnailUrl, ... }

3. Client uses mediaId in message
   └─► POST /messages { type: "image", payload: { mediaId: "..." } }
```

### Presigned URLs

Files are never served directly through the service. Instead, `GET /media/:mediaId` returns metadata with presigned URLs:

```json
{
  "media_id": "abc-123",
  "type": "image",
  "mime_type": "image/jpeg",
  "size_bytes": 245000,
  "url": "https://minio:9000/whatsapp-media/abc-123/photo.jpg?X-Amz-Signature=...",
  "thumbnail_url": "https://minio:9000/whatsapp-media/abc-123/thumb.jpg?X-Amz-Signature=...",
  "width": 1920,
  "height": 1080,
  "created_at": "2026-02-18T12:00:00Z"
}
```

The presigned URLs have a configurable TTL and grant time-limited read access without authentication.

### Orphan Cleanup

A background job periodically scans for media files in MinIO that are not referenced by any message. These orphans (from abandoned uploads) are deleted to reclaim storage.

---

## 7. Notification Service

**Port**: 8085 (HTTP — health check only)
**Framework**: Gin (minimal)
**Databases**: PostgreSQL (device tokens, participants — read only), Redis (presence — read only), NATS (consumer)

### Why It Exists

Push notifications are a fundamentally different concern from message delivery. The notification service is a pure **event consumer** — it has no API that clients call directly. It listens to NATS events and decides whether to send an FCM push. This separation means:
- Message delivery latency is unaffected by notification processing
- Notification logic (batching, presence checks, mute handling) can evolve independently
- FCM failures don't cascade to the messaging pipeline

### Decision Flow

```
NATS event: msg.new arrives
       │
       ▼
Get chat participants from PostgreSQL
       │
       ▼
For each recipient (excluding sender):
       │
       ├─► Check Redis: is user online?
       │   └─► Yes → Skip (they'll get it via WebSocket)
       │
       ├─► Check PostgreSQL: is chat muted for this user?
       │   └─► Yes → Skip
       │
       └─► Queue for FCM push
              │
              ▼
       Batch window (3 seconds)
       ── collect multiple notifications ──
              │
              ▼
       Send batched FCM request
       │
       ├─► Success → done
       └─► Token invalid → remove stale token from PostgreSQL
```

### Batching

Multiple messages in rapid succession (common in active group chats) are collapsed into a single notification within a 3-second window. Instead of "Alice: Hi" + "Alice: How are you?" + "Alice: Want to grab lunch?" arriving as three separate pushes, the user sees one notification.

### NATS Consumers

| Subject | Action |
|---------|--------|
| `msg.new` | Send notification for new message |
| `group.member.added` | Send "You were added to X" notification |

---

## 8. WebSocket Service

**Port**: 8087 (HTTP + WebSocket upgrade)
**Framework**: Gin + gorilla/websocket (or nhooyr)
**Databases**: Redis (pub-sub, presence), NATS (consumer)

### Why It Exists

The WebSocket service is the **real-time backbone** of the entire platform. It maintains persistent, bidirectional connections with every active client. Separating it from the REST services allows:
- Independent horizontal scaling (WebSocket connections are memory-intensive)
- Redis pub-sub handles cross-instance message routing
- Stateful connection management doesn't complicate stateless HTTP services

### Connection Lifecycle

```
1. Client connects
   └─► GET /ws?token=<JWT>
   └─► Service validates JWT via gRPC → auth-service
   └─► If valid: upgrade to WebSocket
   └─► Register connection in Hub (in-memory map: userId → []*Connection)
   └─► Subscribe to Redis channel: user:channel:<userId>
   └─► Update presence in Redis: online=true

2. Heartbeat loop
   └─► Server pings every 25 seconds
   └─► Client must respond within 60 seconds
   └─► Missed pong → connection closed

3. Client disconnects
   └─► Unregister from Hub
   └─► Unsubscribe from Redis channel
   └─► Update presence: online=false, last_seen=now
   └─► Presence TTL expires after 5 minutes
```

### The Hub Pattern

Each WebSocket service instance maintains an in-memory `Hub`:

```go
type Hub struct {
    connections map[string][]*Connection  // userId → active connections
    register   chan *Connection
    unregister chan *Connection
    broadcast  chan Event
}
```

A single user can have multiple connections (e.g., phone + tablet). The Hub delivers events to all of them.

### Cross-Instance Routing via Redis

When the system runs multiple WebSocket instances, a user might be connected to instance A while a message for them arrives at instance B (from NATS). Redis pub-sub solves this:

```
Instance B receives NATS event for userId=X
    │
    └─► Redis PUBLISH user:channel:X { event payload }
    
Instance A (where userId=X is connected)
    │
    └─► Redis SUBSCRIBE user:channel:X
    └─► Receives event → delivers to WebSocket connection
```

### Event Processing Pipeline

```
NATS JetStream events
    │
    ├─► msg.new           → Look up chat participants → deliver message.new to each
    ├─► msg.status.updated → Deliver status update to sender
    ├─► msg.deleted        → Deliver deletion notice to participants
    ├─► chat.created       → Deliver to all participants
    ├─► chat.updated       → Deliver to all participants
    ├─► group.member.added → Deliver to all participants (including new member)
    └─► group.member.removed → Deliver to all participants (including removed member)
```

### Typing Indicators

Typing indicators are pure real-time signals — they are never persisted:

```
User A starts typing in chatId=123
    └─► WS event: { type: "typing.start", chatId: "123" }
    └─► Service publishes to Redis: typing:123:<userA> (TTL: 5s)
    └─► Broadcasts to other participants in chat 123
    
User A stops typing (or TTL expires)
    └─► WS event: { type: "typing.stop", chatId: "123" }
    └─► Broadcasts stop signal
```

### Presence Broadcasting

When users subscribe to another user's presence (`presence.subscribe`), the WebSocket service watches the Redis presence key and sends updates:

```
{ type: "presence.updated", userId: "abc", online: true }
{ type: "presence.updated", userId: "abc", online: false, lastSeen: "..." }
```

---

## Shared Packages (`backend/pkg/`)

All services import from a common `pkg/` directory to avoid code duplication:

| Package | Purpose |
|---------|---------|
| `logger` | Structured logging via Zerolog |
| `metrics` | Prometheus metric registration and exposure |
| `tracing` | OpenTelemetry tracer initialization |
| `middleware` | CORS, recovery, request ID injection, logging middleware |
| `grpcclient` | gRPC client factory with connection pooling and retry |
| `jwt` | JWT generation and validation utilities |
| `health` | Standard health check HTTP handler |
| `errors` | Application-specific error types |
| `response` | HTTP response helpers (success, error, paginated) |
| `validator` | Request body validation |
| `config` | Environment-based configuration loading |

---

## Inter-Service Dependency Graph

```
api-gateway ──gRPC──► auth-service
api-gateway ──HTTP──► [all services]

message-service ──gRPC──► chat-service
message-service ──gRPC──► user-service

chat-service ──gRPC──► message-service

websocket-service ──gRPC──► auth-service
websocket-service ──gRPC──► message-service
websocket-service ──gRPC──► chat-service

notification-service ──reads──► PostgreSQL (device_tokens, participants)
notification-service ──reads──► Redis (presence)

chat-service ──publishes──► NATS
message-service ──publishes──► NATS
websocket-service ──consumes──► NATS
notification-service ──consumes──► NATS
```

---

## Background Jobs

| Service | Job | Schedule | Purpose |
|---------|-----|----------|---------|
| message-service | Disappearing message cleanup | Every 6 hours | Deletes messages past their auto-delete timer |
| media-service | Orphan file cleanup | Periodic | Removes MinIO files with no message reference |
| notification-service | Stale token cleanup | On FCM error | Removes invalid FCM tokens |
