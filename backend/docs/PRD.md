# Backend Microservices — Product Requirements Document

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [System Architecture](#2-system-architecture)
3. [Microservice Breakdown](#3-microservice-breakdown)
4. [Inter-Service Communication](#4-inter-service-communication)
5. [Database Design](#5-database-design)
6. [API Specifications](#6-api-specifications)
7. [WebSocket Protocol](#7-websocket-protocol)
8. [Push Notification Flow](#8-push-notification-flow)
9. [Infrastructure](#9-infrastructure)
10. [Non-Functional Requirements](#10-non-functional-requirements)
11. [Security](#11-security)
12. [Phased Delivery Roadmap](#12-phased-delivery-roadmap)
13. [Cross-PRD Alignment Notes](#13-cross-prd-alignment-notes)
14. [Glossary](#14-glossary)

---

## 1. Product Overview

### 1.1 Purpose

This document specifies the backend system for a WhatsApp-clone messaging platform. The backend is a collection of loosely-coupled Golang microservices that together provide real-time messaging, group chat, media sharing, push notifications, and user management.

### 1.2 Goals

| Goal | Description |
|------|-------------|
| **Real-time messaging** | Sub-second message delivery via persistent WebSocket connections |
| **Scalability** | Horizontally scalable microservices behind a single API gateway |
| **Reliability** | At-least-once message delivery with status tracking (sent → delivered → read) |
| **Local-first development** | Everything runs on a single macOS machine using Docker / Kind |
| **Extensibility** | Clean service boundaries to add features (calls, stories) later |

### 1.3 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Go 1.22+ |
| HTTP Framework | Gin |
| gRPC | google.golang.org/grpc |
| SQL Database | PostgreSQL 16 |
| Document Store | MongoDB 7 |
| Cache / Pub-Sub | Redis 7 |
| Message Queue | NATS JetStream |
| Object Storage | MinIO (S3-compatible) |
| Containerization | Docker |
| Orchestration | Kind (local K8s) + Helm 3 |
| Tunnel | ngrok (exposes API gateway) |
| Auth | JWT (access + refresh tokens) |
| Push Notifications | Firebase Cloud Messaging (FCM) |

### 1.4 Directory Layout

```
backend/
├── api-gateway/
├── auth-service/
├── user-service/
├── chat-service/
├── message-service/
├── notification-service/
├── media-service/
├── websocket-service/
├── pkg/                  # Shared libraries (JWT, logger, errors, proto)
├── proto/                # Protobuf definitions for gRPC
├── migrations/           # SQL & Mongo migration scripts
├── helm/                 # Helm charts (one sub-chart per service)
├── docker-compose.yml    # Local dev alternative to K8s
└── Makefile
```

Each service follows an identical internal layout:

```
<service-name>/
├── cmd/
│   └── main.go           # Entrypoint
├── internal/
│   ├── handler/          # HTTP / gRPC handlers
│   ├── service/          # Business logic
│   ├── repository/       # Data access layer
│   └── model/            # Domain models
├── config/
│   └── config.go         # Env-based configuration
├── Dockerfile
└── README.md
```

---

## 2. System Architecture

### 2.1 High-Level Diagram

```mermaid
graph TB
    subgraph Client
        A[Android App]
    end

    subgraph Tunnel
        NG[ngrok]
    end

    subgraph APILayer [API Layer]
        GW["API Gateway :8080"]
    end

    subgraph Services
        AUTH["Auth Service :8081"]
        USER["User Service :8082"]
        CHAT["Chat Service :8083"]
        MSG["Message Service :8084"]
        NOTIF["Notification Service :8085"]
        MEDIA["Media Service :8086"]
        WS["WebSocket Service :8087"]
    end

    subgraph Data Stores
        PG[(PostgreSQL)]
        MONGO[(MongoDB)]
        REDIS[(Redis)]
        MINIO[(MinIO)]
    end

    subgraph External
        FCM[Firebase Cloud Messaging]
    end

    subgraph Messaging
        NATS[NATS JetStream]
    end

    A -->|HTTPS| NG
    NG --> GW
    A -->|WSS| NG

    GW -->|REST / gRPC| AUTH
    GW -->|REST / gRPC| USER
    GW -->|REST / gRPC| CHAT
    GW -->|REST / gRPC| MSG
    GW -->|REST / gRPC| MEDIA
    GW -->|WS Upgrade| WS

    AUTH --> PG
    AUTH --> REDIS
    USER --> PG
    USER --> REDIS
    CHAT --> PG
    MSG --> MONGO
    MEDIA --> MINIO
    MEDIA --> MONGO
    NOTIF --> FCM

    WS --> REDIS
    WS --> NATS
    MSG --> NATS
    NOTIF --> NATS
    CHAT --> NATS
```

### 2.2 Request Flow (Typical Message Send)

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant WS as WebSocket Service
    participant MSG as Message Service
    participant NATS as NATS JetStream
    participant NOTIF as Notification Service
    participant R as Redis Pub-Sub

    C->>WS: WebSocket frame (new message)
    WS->>MSG: gRPC SendMessage()
    MSG->>MSG: Persist to MongoDB
    MSG->>NATS: Publish "msg.new" event
    MSG-->>WS: Ack with message ID + timestamp

    NATS->>WS: Consume "msg.new"
    WS->>R: Publish to recipient channel
    R->>WS: Subscriber receives
    WS->>C: WebSocket frame (message delivered)

    NATS->>NOTIF: Consume "msg.new"
    NOTIF->>NOTIF: Check recipient online status
    alt Recipient offline
        NOTIF->>FCM: Send push notification
    end
```

---

## 3. Microservice Breakdown

### 3.1 API Gateway (`api-gateway/`)

**Purpose**: Single entry point for all client traffic. Routes requests, enforces authentication, and applies rate limiting.

| Aspect | Detail |
|--------|--------|
| Framework | Gin |
| Port | 8080 |
| Exposed via | ngrok |

**Responsibilities**:

- Route incoming HTTP requests to downstream services (reverse proxy)
- Upgrade WebSocket connections and proxy to `websocket-service`
- Validate JWT access tokens on protected routes (middleware)
- Apply per-IP and per-user rate limiting (token bucket via Redis)
- CORS handling
- Request/response logging
- Health check aggregation (`/health`)

**Route Table**:

| Prefix | Target Service | Notes |
|--------|---------------|-------|
| `/api/v1/auth/*` | auth-service | Public (no JWT required for OTP endpoints) |
| `/api/v1/users/*` | user-service | Includes privacy settings, FCM token |
| `/api/v1/chats/*` | chat-service | Chat CRUD, group management, participants |
| `/api/v1/chats/:id/messages/*` | message-service | Message history, send (REST fallback) |
| `/api/v1/messages/*` | message-service | Delete, forward, star (by message ID) |
| `/api/v1/media/*` | media-service | Upload, download, thumbnails |
| `/ws` | websocket-service | WebSocket upgrade |

---

### 3.2 Auth Service (`auth-service/`)

**Purpose**: Handle phone-number-based OTP authentication, JWT token issuance, and token refresh/revocation.

| Aspect | Detail |
|--------|--------|
| Framework | Gin + gRPC |
| Port | 8081 (HTTP), 9081 (gRPC) |
| Databases | PostgreSQL, Redis |

**Responsibilities**:

- Send OTP to phone number (mock SMS provider for local dev)
- Verify OTP and issue JWT access token + refresh token pair
- Refresh expired access tokens using refresh tokens
- Revoke tokens (logout)
- Expose gRPC `ValidateToken` RPC for other services

**Token Strategy**:

| Token | TTL | Storage |
|-------|-----|---------|
| Access Token (JWT) | 15 minutes | Stateless (signed) |
| Refresh Token (opaque) | 30 days | Redis (revocable) |

**JWT Claims**:

```json
{
  "sub": "user-uuid",
  "phone": "+1234567890",
  "iat": 1700000000,
  "exp": 1700000900
}
```

---

### 3.3 User Service (`user-service/`)

**Purpose**: Manage user profiles, contacts, and online/last-seen presence.

| Aspect | Detail |
|--------|--------|
| Framework | Gin + gRPC |
| Port | 8082 (HTTP), 9082 (gRPC) |
| Databases | PostgreSQL, Redis |

**Responsibilities**:

- Create user profile on first login (upsert)
- Update profile (display name, avatar URL, status text)
- Fetch user profile by ID or phone number
- Contact sync: client uploads phone contacts → service matches registered users
- Presence tracking:
  - `online` / `offline` status stored in Redis with TTL
  - `last_seen` timestamp persisted to PostgreSQL on disconnect
- Block / unblock users
- Expose gRPC `GetUser`, `GetUsers`, `CheckPresence` RPCs

---

### 3.4 Chat Service (`chat-service/`)

**Purpose**: Manage chat metadata for 1-to-1 and group conversations.

| Aspect | Detail |
|--------|--------|
| Framework | Gin + gRPC |
| Port | 8083 (HTTP), 9083 (gRPC) |
| Databases | PostgreSQL |

**Responsibilities**:

- Create 1-to-1 chat (idempotent — same pair always returns same chat)
- Create group chat (creator becomes admin)
- Add / remove group members (admin-only)
- Promote / demote group admins
- Update group metadata (name, avatar, description)
- List chats for a user (with last message preview, unread count)
- Mute / unmute chat
- Publish `chat.created`, `chat.updated`, `group.member.added`, `group.member.removed` events to NATS

**Group Constraints**:

| Constraint | Value |
|------------|-------|
| Max group size | 256 members |
| Min admins | 1 (creator cannot leave without transferring admin) |
| Group name max length | 100 characters |
| Group description max length | 512 characters |

---

### 3.5 Message Service (`message-service/`)

**Purpose**: Message CRUD, status tracking, and paginated history retrieval.

| Aspect | Detail |
|--------|--------|
| Framework | Gin + gRPC |
| Port | 8084 (HTTP), 9084 (gRPC) |
| Databases | MongoDB, Redis |

**Responsibilities**:

- Persist messages (text, media references, voice notes)
- Track message status per recipient: `sent` → `delivered` → `read`
- Provide paginated message history (cursor-based, newest-first)
- Delete message (soft delete — replace content with "This message was deleted")
- Star / unstar messages
- Search messages within a chat (text index on MongoDB)
- Publish `msg.new`, `msg.status.updated`, `msg.deleted` events to NATS

**Message Types**:

| Type | Payload |
|------|---------|
| `text` | `{ "body": "Hello!" }` |
| `image` | `{ "media_id": "uuid", "caption": "..." }` |
| `video` | `{ "media_id": "uuid", "caption": "..." }` |
| `audio` | `{ "media_id": "uuid", "duration_ms": 5000 }` |
| `document` | `{ "media_id": "uuid", "filename": "report.pdf" }` |
| `system` | `{ "body": "Alice added Bob" }` |

**Pagination**:

- Cursor-based using `(chat_id, created_at, message_id)` composite key
- Default page size: 50, max: 100

---

### 3.6 Notification Service (`notification-service/`)

**Purpose**: Deliver push notifications to offline users via FCM and manage notification preferences.

| Aspect | Detail |
|--------|--------|
| Framework | Gin + gRPC |
| Port | 8085 (HTTP), 9085 (gRPC) |
| Databases | PostgreSQL, Redis |

**Responsibilities**:

- Listen to NATS events (`msg.new`, `group.member.added`, etc.)
- Check if recipient is currently online (via Redis presence)
- If offline → send FCM push notification
- Register / update FCM device tokens per user
- Manage per-chat notification preferences (mute/unmute)
- Batch notifications for groups (debounce rapid messages, e.g., 3 second window)

**FCM Payload Structure** (FCM HTTP v1 API):

```json
{
  "message": {
    "token": "<fcm_device_token>",
    "data": {
      "type": "new_message",
      "chat_id": "uuid",
      "chat_type": "direct",
      "message_id": "uuid",
      "sender_id": "uuid",
      "sender_name": "John Doe",
      "sender_avatar": "https://...",
      "message_preview": "Hey, are you free tonight?",
      "message_type": "text",
      "timestamp": "1708099200000"
    },
    "android": {
      "priority": "high",
      "ttl": "86400s"
    }
  }
}
```

> **Note**: We use data-only messages (no `notification` block) so the client `FirebaseMessagingService` always handles display, enabling custom grouping, mute checks, and foreground suppression.

---

### 3.7 Media Service (`media-service/`)

**Purpose**: Handle file uploads, downloads, thumbnail generation, and media metadata.

| Aspect | Detail |
|--------|--------|
| Framework | Gin + gRPC |
| Port | 8086 (HTTP), 9086 (gRPC) |
| Databases | MongoDB, MinIO |

**Responsibilities**:

- Accept multipart file uploads (images, videos, audio, documents)
- Validate file type and size limits
- Generate thumbnails for images and video (using FFmpeg)
- Store original + thumbnail in MinIO
- Persist media metadata in MongoDB (file type, size, dimensions, duration, checksum)
- Serve presigned download URLs (time-limited)
- Cleanup expired / orphaned media (background job)

**File Limits**:

| Type | Max Size |
|------|----------|
| Image | 16 MB |
| Video | 64 MB |
| Audio | 16 MB |
| Document | 100 MB |

**Supported Formats**:

| Type | Formats |
|------|---------|
| Image | JPEG, PNG, WebP, GIF |
| Video | MP4, 3GP, MKV |
| Audio | AAC, OGG, MP3, OPUS |
| Document | PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, ZIP |

---

### 3.8 WebSocket Service (`websocket-service/`)

**Purpose**: Maintain persistent bidirectional connections with clients for real-time message delivery, typing indicators, and presence.

| Aspect | Detail |
|--------|--------|
| Framework | Gorilla WebSocket + Gin (upgrade endpoint) |
| Port | 8087 (HTTP/WS) |
| Databases | Redis |

**Responsibilities**:

- Accept WebSocket upgrade from API Gateway
- Authenticate connection via JWT (passed as query param or first frame)
- Maintain in-memory connection registry (user_id → []*websocket.Conn)
- Subscribe to Redis Pub-Sub channels per user for message fan-out
- Consume NATS events and push to connected clients
- Handle typing indicators (publish/subscribe via Redis)
- Broadcast presence changes (online/offline)
- Heartbeat / ping-pong for connection health (30s interval)
- Graceful connection cleanup on disconnect

**Connection Lifecycle**:

```
1. Client opens WSS connection to /ws?token=<jwt>
2. Server validates JWT
3. Server registers connection in memory + publishes "user.online" to Redis
4. Server subscribes to Redis channel "user:<user_id>"
5. Bidirectional communication begins
6. On disconnect → unregister, publish "user.offline", update last_seen
```

---

## 4. Inter-Service Communication

### 4.1 Synchronous — gRPC

Used for request-response patterns where the caller needs an immediate result.

| Caller | Callee | RPC | Purpose |
|--------|--------|-----|---------|
| api-gateway | auth-service | `ValidateToken` | Authenticate incoming requests |
| websocket-service | message-service | `SendMessage` | Persist new message |
| websocket-service | message-service | `UpdateMessageStatus` | Mark delivered/read |
| chat-service | user-service | `GetUsers` | Resolve member profiles |
| notification-service | user-service | `CheckPresence` | Decide push vs skip |
| message-service | media-service | `GetMediaMetadata` | Enrich message response |

**Proto Definitions** (in `proto/` directory):

```protobuf
// proto/auth/auth.proto
syntax = "proto3";
package auth;

service AuthService {
  rpc ValidateToken(ValidateTokenRequest) returns (ValidateTokenResponse);
}

message ValidateTokenRequest {
  string token = 1;
}

message ValidateTokenResponse {
  bool valid = 1;
  string user_id = 2;
  string phone = 3;
}
```

```protobuf
// proto/message/message.proto
syntax = "proto3";
package message;

service MessageService {
  rpc SendMessage(SendMessageRequest) returns (SendMessageResponse);
  rpc UpdateMessageStatus(UpdateStatusRequest) returns (UpdateStatusResponse);
}

message SendMessageRequest {
  string chat_id = 1;
  string sender_id = 2;
  string type = 3;         // text, image, video, audio, document
  bytes payload = 4;       // JSON-encoded type-specific payload
  string client_msg_id = 5; // Client-generated idempotency key
}

message SendMessageResponse {
  string message_id = 1;
  int64 timestamp = 2;
}

message UpdateStatusRequest {
  string message_id = 1;
  string user_id = 2;
  string status = 3; // delivered, read
}

message UpdateStatusResponse {
  bool success = 1;
}
```

```protobuf
// proto/user/user.proto
syntax = "proto3";
package user;

service UserService {
  rpc GetUser(GetUserRequest) returns (UserResponse);
  rpc GetUsers(GetUsersRequest) returns (GetUsersResponse);
  rpc CheckPresence(CheckPresenceRequest) returns (CheckPresenceResponse);
}

message GetUserRequest {
  string user_id = 1;
}

message GetUsersRequest {
  repeated string user_ids = 1;
}

message UserResponse {
  string user_id = 1;
  string phone = 2;
  string display_name = 3;
  string avatar_url = 4;
  string status_text = 5;
}

message GetUsersResponse {
  repeated UserResponse users = 1;
}

message CheckPresenceRequest {
  string user_id = 1;
}

message CheckPresenceResponse {
  bool online = 1;
  int64 last_seen = 2;
}
```

```protobuf
// proto/media/media.proto
syntax = "proto3";
package media;

service MediaService {
  rpc GetMediaMetadata(GetMediaRequest) returns (MediaMetadataResponse);
}

message GetMediaRequest {
  string media_id = 1;
}

message MediaMetadataResponse {
  string media_id = 1;
  string file_type = 2;
  string mime_type = 3;
  int64 size_bytes = 4;
  string url = 5;
  string thumbnail_url = 6;
  int32 width = 7;
  int32 height = 8;
  int32 duration_ms = 9;
}
```

### 4.2 Asynchronous — NATS JetStream

Used for event-driven communication where the publisher does not need an immediate response.

**Stream Configuration**:

| Stream | Subjects | Retention | Max Age |
|--------|----------|-----------|---------|
| MESSAGES | `msg.>` | WorkQueue | 7 days |
| CHATS | `chat.>` | WorkQueue | 7 days |
| PRESENCE | `presence.>` | Interest | 1 hour |
| NOTIFICATIONS | `notif.>` | WorkQueue | 3 days |

**Key Events**:

| Subject | Publisher | Consumers | Payload Summary |
|---------|-----------|-----------|-----------------|
| `msg.new` | message-service | websocket-service, notification-service | `{ message_id, chat_id, sender_id, type, payload, timestamp }` |
| `msg.status.updated` | message-service | websocket-service | `{ message_id, user_id, status, timestamp }` |
| `msg.deleted` | message-service | websocket-service | `{ message_id, chat_id, timestamp }` |
| `chat.created` | chat-service | websocket-service | `{ chat_id, type, participants }` |
| `chat.updated` | chat-service | websocket-service | `{ chat_id, changes }` |
| `group.member.added` | chat-service | websocket-service, notification-service | `{ chat_id, user_id, added_by }` |
| `group.member.removed` | chat-service | websocket-service, notification-service | `{ chat_id, user_id, removed_by }` |
| `presence.changed` | websocket-service | websocket-service (fan-out) | `{ user_id, status, last_seen }` |

---

## 5. Database Design

### 5.1 PostgreSQL Schema

#### `users` table

```sql
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone       VARCHAR(20) UNIQUE NOT NULL,
    display_name VARCHAR(64) NOT NULL DEFAULT '',
    avatar_url  TEXT NOT NULL DEFAULT '',
    status_text VARCHAR(140) NOT NULL DEFAULT 'Hey there! I am using WhatsApp.',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users(phone);
```

#### `contacts` table

```sql
CREATE TABLE contacts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nickname    VARCHAR(64) NOT NULL DEFAULT '',
    is_blocked  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(user_id, contact_id)
);

CREATE INDEX idx_contacts_user ON contacts(user_id);
```

#### `chats` table

```sql
CREATE TABLE chats (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type        VARCHAR(10) NOT NULL CHECK (type IN ('direct', 'group')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

#### `chat_participants` table

```sql
CREATE TABLE chat_participants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id     UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(10) NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    is_muted    BOOLEAN NOT NULL DEFAULT FALSE,
    mute_until  TIMESTAMPTZ,            -- NULL = muted indefinitely (when is_muted=true)
    is_pinned   BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(chat_id, user_id)
);

CREATE INDEX idx_chat_participants_user ON chat_participants(user_id);
CREATE INDEX idx_chat_participants_chat ON chat_participants(chat_id);
```

#### `groups` table

```sql
CREATE TABLE groups (
    chat_id       UUID PRIMARY KEY REFERENCES chats(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(512) NOT NULL DEFAULT '',
    avatar_url    TEXT NOT NULL DEFAULT '',
    created_by    UUID NOT NULL REFERENCES users(id),
    is_admin_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

#### `device_tokens` table

```sql
CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT NOT NULL UNIQUE,
    platform    VARCHAR(10) NOT NULL DEFAULT 'android',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_tokens_user ON device_tokens(user_id);
```

#### `refresh_tokens` table

```sql
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

#### `privacy_settings` table

```sql
CREATE TABLE privacy_settings (
    user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    last_seen       VARCHAR(20) NOT NULL DEFAULT 'everyone'
                    CHECK (last_seen IN ('everyone', 'contacts', 'nobody')),
    profile_photo   VARCHAR(20) NOT NULL DEFAULT 'everyone'
                    CHECK (profile_photo IN ('everyone', 'contacts', 'nobody')),
    about           VARCHAR(20) NOT NULL DEFAULT 'everyone'
                    CHECK (about IN ('everyone', 'contacts', 'nobody')),
    read_receipts   BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 5.2 MongoDB Collections

#### `messages` collection

```json
{
  "_id": "ObjectId",
  "message_id": "UUID (string)",
  "chat_id": "UUID (string)",
  "sender_id": "UUID (string)",
  "client_msg_id": "string (idempotency key)",
  "type": "text | image | video | audio | document | system",
  "reply_to_message_id": "UUID (string, nullable — for quoted replies)",
  "forwarded_from": {
    "chat_id": "UUID (string, nullable)",
    "message_id": "UUID (string, nullable)"
  },
  "payload": {
    "body": "string (for text/system)",
    "media_id": "UUID (for media types)",
    "caption": "string",
    "filename": "string",
    "duration_ms": 0
  },
  "status": {
    "<recipient_user_id>": {
      "status": "sent | delivered | read",
      "updated_at": "ISODate"
    }
  },
  "is_deleted": false,
  "is_starred_by": ["user_id_1", "user_id_2"],
  "created_at": "ISODate",
  "updated_at": "ISODate"
}
```

**Indexes**:

```javascript
db.messages.createIndex({ "chat_id": 1, "created_at": -1 });
db.messages.createIndex({ "client_msg_id": 1 }, { unique: true });
db.messages.createIndex({ "chat_id": 1, "payload.body": "text" });
```

#### `media` collection

```json
{
  "_id": "ObjectId",
  "media_id": "UUID (string)",
  "uploader_id": "UUID (string)",
  "file_type": "image | video | audio | document",
  "mime_type": "string",
  "original_filename": "string",
  "size_bytes": 0,
  "width": 0,
  "height": 0,
  "duration_ms": 0,
  "checksum_sha256": "string",
  "storage_key": "string (MinIO object key)",
  "thumbnail_key": "string (MinIO object key, nullable)",
  "created_at": "ISODate"
}
```

**Indexes**:

```javascript
db.media.createIndex({ "media_id": 1 }, { unique: true });
db.media.createIndex({ "uploader_id": 1 });
```

### 5.3 Redis Data Structures

| Key Pattern | Type | TTL | Purpose |
|-------------|------|-----|---------|
| `session:<user_id>` | STRING (refresh token hash) | 30 days | Session lookup |
| `presence:<user_id>` | STRING ("online") | 60s (refreshed by heartbeat) | Online status |
| `rate:<ip>:<endpoint>` | STRING (counter) | 1 min | Rate limiting |
| `typing:<chat_id>:<user_id>` | STRING ("1") | 5s | Typing indicator |
| `user:channel:<user_id>` | Pub-Sub channel | — | Real-time message fan-out |
| `otp:<phone>` | STRING (hashed OTP) | 5 min | OTP verification |

---

## 6. API Specifications

All endpoints are prefixed with `/api/v1`. Responses follow a standard envelope:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "request_id": "uuid",
    "timestamp": "ISO8601"
  }
}
```

Error responses:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Phone number is required",
    "details": [...]
  },
  "meta": { ... }
}
```

### 6.1 Auth Service Endpoints

#### `POST /api/v1/auth/request-otp`

Send OTP to a phone number.

**Request**:

```json
{
  "phone": "+1234567890",
  "country_code": "+1"
}
```

**Response** (200):

```json
{
  "success": true,
  "data": {
    "request_id": "uuid",
    "message": "OTP sent successfully",
    "expires_in_seconds": 300
  }
}
```

#### `POST /api/v1/auth/verify-otp`

Verify OTP and receive tokens. Creates user if first login.

**Request**:

```json
{
  "phone": "+1234567890",
  "otp": "123456"
}
```

**Response** (200):

```json
{
  "success": true,
  "data": {
    "access_token": "eyJ...",
    "refresh_token": "opaque-token-string",
    "expires_in": 900,
    "user": {
      "id": "uuid",
      "phone": "+1234567890",
      "display_name": "",
      "is_new_user": true
    }
  }
}
```

#### `POST /api/v1/auth/refresh`

Refresh an expired access token.

**Request**:

```json
{
  "refresh_token": "opaque-token-string"
}
```

**Response** (200):

```json
{
  "success": true,
  "data": {
    "access_token": "eyJ...",
    "refresh_token": "new-opaque-token",
    "expires_in": 900
  }
}
```

#### `POST /api/v1/auth/logout`

Revoke refresh token and unregister FCM device token. Requires `Authorization: Bearer <access_token>`.

**Response** (200):

```json
{
  "success": true,
  "data": {
    "message": "Logged out successfully"
  }
}
```

---

### 6.2 User Service Endpoints

All endpoints require `Authorization: Bearer <access_token>`.

#### `GET /api/v1/users/me`

Get current user profile.

**Response** (200):

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "phone": "+1234567890",
    "display_name": "John Doe",
    "avatar_url": "https://...",
    "status_text": "Hey there!",
    "created_at": "ISO8601"
  }
}
```

#### `PATCH /api/v1/users/me`

Update current user profile.

**Request**:

```json
{
  "display_name": "John Doe",
  "status_text": "Available"
}
```

#### `GET /api/v1/users/:id`

Get user profile by ID.

#### `POST /api/v1/users/contacts/sync`

Sync phone contacts to discover registered users.

**Request**:

```json
{
  "phone_numbers": ["+1234567890", "+0987654321"]
}
```

**Response** (200):

```json
{
  "success": true,
  "data": {
    "registered_users": [
      {
        "phone": "+1234567890",
        "user_id": "uuid",
        "display_name": "Jane",
        "avatar_url": "..."
      }
    ]
  }
}
```

#### `POST /api/v1/users/:id/block`

Block a user.

#### `DELETE /api/v1/users/:id/block`

Unblock a user.

#### `GET /api/v1/users/:id/presence`

Get user online status and last seen (respects target user's privacy settings).

**Response** (200):

```json
{
  "success": true,
  "data": {
    "user_id": "uuid",
    "online": false,
    "last_seen": "ISO8601"
  }
}
```

#### `GET /api/v1/users/me/privacy`

Get current user's privacy settings.

**Response** (200):

```json
{
  "success": true,
  "data": {
    "last_seen": "everyone",
    "profile_photo": "contacts",
    "about": "everyone",
    "read_receipts": true
  }
}
```

#### `PUT /api/v1/users/me/privacy`

Update privacy settings.

**Request**:

```json
{
  "last_seen": "contacts",
  "read_receipts": false
}
```

#### `PUT /api/v1/users/me/fcm-token`

Register or update FCM device token for push notifications.

**Request**:

```json
{
  "token": "fcm-device-token",
  "platform": "android"
}
```

#### `DELETE /api/v1/users/me/fcm-token`

Unregister device token (called on logout alongside `/auth/logout`).

---

### 6.3 Chat Service Endpoints

All endpoints require `Authorization: Bearer <access_token>`.

#### `POST /api/v1/chats`

Create a new chat (1-to-1 or group).

**Request (direct)**:

```json
{
  "type": "direct",
  "participant_id": "uuid"
}
```

**Request (group)**:

```json
{
  "type": "group",
  "name": "Family Group",
  "participant_ids": ["uuid1", "uuid2", "uuid3"]
}
```

**Response** (201):

```json
{
  "success": true,
  "data": {
    "chat_id": "uuid",
    "type": "group",
    "name": "Family Group",
    "participants": [...],
    "created_at": "ISO8601"
  }
}
```

#### `GET /api/v1/chats`

List all chats for the authenticated user (with last message preview and unread count).

**Query Params**: `?cursor=<timestamp>&limit=20`

**Response** (200):

```json
{
  "success": true,
  "data": {
    "chats": [
      {
        "chat_id": "uuid",
        "type": "direct",
        "participant": { "id": "uuid", "display_name": "Jane", "avatar_url": "..." },
        "last_message": {
          "message_id": "uuid",
          "sender_id": "uuid",
          "type": "text",
          "preview": "Hey, how are you?",
          "timestamp": "ISO8601"
        },
        "unread_count": 3,
        "is_muted": false,
        "updated_at": "ISO8601"
      }
    ],
    "next_cursor": "ISO8601"
  }
}
```

#### `GET /api/v1/chats/:id`

Get chat details (metadata + participants).

#### `PATCH /api/v1/chats/:id`

Update group chat metadata (name, description, avatar). Admin-only.

#### `POST /api/v1/chats/:id/participants`

Add members to a group chat. Admin-only.

**Request**:

```json
{
  "user_ids": ["uuid1", "uuid2"]
}
```

#### `DELETE /api/v1/chats/:id/participants/:userId`

Remove a member from a group chat. Admin-only (or self-removal to leave).

#### `PATCH /api/v1/chats/:id/participants/:userId/role`

Promote / demote a member. Admin-only.

**Request**:

```json
{
  "role": "admin"
}
```

#### `PATCH /api/v1/chats/:id/mute`

Mute / unmute a chat.

**Request**:

```json
{
  "is_muted": true
}
```

---

### 6.4 Message Service Endpoints

All endpoints require `Authorization: Bearer <access_token>`.

#### `GET /api/v1/chats/:chatId/messages`

Get paginated message history for a chat.

**Query Params**: `?cursor=<message_id>&limit=50`

**Response** (200):

```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "message_id": "uuid",
        "chat_id": "uuid",
        "sender_id": "uuid",
        "type": "text",
        "payload": { "body": "Hello!" },
        "reply_to_message_id": null,
        "forwarded": false,
        "status": "delivered",
        "is_deleted": false,
        "created_at": "ISO8601"
      }
    ],
    "next_cursor": "uuid",
    "has_more": true
  }
}
```

#### `POST /api/v1/chats/:chatId/messages`

Send a message (REST fallback -- primary path is via WebSocket).

**Request**:

```json
{
  "client_msg_id": "client-generated-uuid",
  "type": "text",
  "payload": { "body": "Hello!" },
  "reply_to_message_id": null
}
```

#### `DELETE /api/v1/messages/:messageId`

Soft-delete a message (replace content with deletion marker).

**Query Params**: `?for=everyone` (optional, default `me`)

#### `POST /api/v1/messages/:messageId/forward`

Forward a message to one or more chats.

**Request**:

```json
{
  "target_chat_ids": ["uuid1", "uuid2"]
}
```

**Response** (201):

```json
{
  "success": true,
  "data": {
    "forwarded_messages": [
      { "chat_id": "uuid1", "message_id": "new-uuid1" },
      { "chat_id": "uuid2", "message_id": "new-uuid2" }
    ]
  }
}
```

#### `POST /api/v1/messages/:messageId/star`

Star a message.

#### `DELETE /api/v1/messages/:messageId/star`

Unstar a message.

#### `POST /api/v1/chats/:chatId/messages/read`

Mark all messages up to a given message as read.

**Request**:

```json
{
  "up_to_message_id": "uuid"
}
```

---

### 6.5 Media Service Endpoints

All endpoints require `Authorization: Bearer <access_token>`.

#### `POST /api/v1/media/upload`

Upload a file. Uses `multipart/form-data`.

**Form Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | The file to upload |
| `type` | String | Yes | `image`, `video`, `audio`, `document` |

**Response** (201):

```json
{
  "success": true,
  "data": {
    "media_id": "uuid",
    "type": "image",
    "mime_type": "image/jpeg",
    "size_bytes": 245000,
    "width": 1920,
    "height": 1080,
    "url": "https://.../presigned-url",
    "thumbnail_url": "https://.../presigned-thumb-url"
  }
}
```

#### `GET /api/v1/media/:mediaId`

Get media metadata and a fresh presigned download URL.

#### `GET /api/v1/media/:mediaId/download`

Redirect to presigned MinIO download URL.

---

### 6.6 Notification Service

The notification service is event-driven and has no client-facing REST endpoints. It consumes NATS events internally. FCM device token registration is handled by the user-service (`PUT /api/v1/users/me/fcm-token`), and token records are stored in the `device_tokens` table which the notification service reads.

---

## 7. WebSocket Protocol

### 7.1 Connection

```
WSS wss://<ngrok-url>/ws?token=<jwt_access_token>
```

The server validates the JWT on upgrade. Invalid/expired tokens result in a `4001` close code.

### 7.2 Frame Format

All WebSocket frames are JSON-encoded text frames.

**Client → Server**:

```json
{
  "event": "string",
  "data": { ... },
  "req_id": "client-generated-uuid"
}
```

**Server → Client**:

```json
{
  "event": "string",
  "data": { ... },
  "req_id": "echo-back-if-applicable",
  "timestamp": "ISO8601"
}
```

### 7.3 Client Events (Client → Server)

| Event | Data | Description |
|-------|------|-------------|
| `message.send` | `{ chat_id, client_msg_id, type, payload, reply_to_message_id? }` | Send a new message |
| `message.delivered` | `{ message_ids: [...] }` | Acknowledge delivery |
| `message.read` | `{ chat_id, up_to_message_id }` | Mark messages as read |
| `message.delete` | `{ message_id, for: "me" or "everyone" }` | Delete a message |
| `typing.start` | `{ chat_id }` | Start typing indicator |
| `typing.stop` | `{ chat_id }` | Stop typing indicator |
| `presence.subscribe` | `{ user_ids: [...] }` | Subscribe to presence updates |
| `ping` | `{}` | Client heartbeat |

> **Naming convention**: All WebSocket events use dot-notation (`message.send`, `typing.start`). The client app should map its internal event models to these wire names.

### 7.4 Server Events (Server → Client)

| Event | Data | Description |
|-------|------|-------------|
| `message.new` | `{ message_id, chat_id, sender_id, type, payload, timestamp }` | New incoming message |
| `message.sent` | `{ message_id, client_msg_id, timestamp }` | Ack for sent message |
| `message.status` | `{ message_id, user_id, status, timestamp }` | Status change (delivered/read) |
| `message.deleted` | `{ message_id, chat_id }` | Message was deleted |
| `typing` | `{ chat_id, user_id, is_typing }` | Typing indicator |
| `presence` | `{ user_id, online, last_seen }` | Presence update |
| `chat.created` | `{ chat_id, type, name, participants }` | Added to a new chat |
| `chat.updated` | `{ chat_id, changes }` | Chat metadata updated |
| `group.member.added` | `{ chat_id, user_id, display_name }` | Member joined group |
| `group.member.removed` | `{ chat_id, user_id }` | Member left/removed |
| `pong` | `{}` | Server heartbeat response |
| `error` | `{ code, message }` | Error response |

### 7.5 Heartbeat

- Client sends `ping` every **25 seconds**
- Server responds with `pong`
- Server closes connection if no `ping` received within **60 seconds**
- Client reconnects with exponential backoff: 1s, 2s, 4s, 8s, 16s, max 30s

### 7.6 Error Codes

| Code | Meaning |
|------|---------|
| `4001` | Authentication failed / token expired |
| `4003` | Forbidden (not a member of chat) |
| `4004` | Resource not found |
| `4029` | Rate limited |
| `5000` | Internal server error |

---

## 8. Push Notification Flow

```mermaid
flowchart TD
    A[NATS Event: msg.new] --> B{Notification Service}
    B --> C{Is recipient online?}
    C -->|Yes| D[Skip push - delivered via WS]
    C -->|No| E{Is chat muted?}
    E -->|Yes| F[Skip push]
    E -->|No| G{Batch window open?}
    G -->|Yes| H[Add to batch buffer]
    G -->|No| I[Start 3s batch timer]
    I --> H
    H --> J{Timer expired?}
    J -->|Yes| K[Build FCM payload]
    K --> L{Has device token?}
    L -->|Yes| M[Send FCM push]
    L -->|No| N[Log warning - no token]
    M --> O{FCM response}
    O -->|Success| P[Done]
    O -->|Token invalid| Q[Remove stale token]
    O -->|Retry-able error| R[Retry with backoff]
```

**Notification Types**:

| Trigger Event | Title | Body |
|---------------|-------|------|
| `msg.new` (text) | Sender name | Message body (truncated 100 chars) |
| `msg.new` (image) | Sender name | "📷 Photo" |
| `msg.new` (video) | Sender name | "📹 Video" |
| `msg.new` (audio) | Sender name | "🎵 Audio" |
| `msg.new` (document) | Sender name | "📄 Document" |
| `group.member.added` | Group name | "You were added by {admin}" |

---

## 9. Infrastructure

### 9.1 Docker

Each service has its own multi-stage `Dockerfile`:

```dockerfile
# Build stage
FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /service ./cmd/main.go

# Run stage
FROM alpine:3.19
RUN apk --no-cache add ca-certificates tzdata
COPY --from=builder /service /service
EXPOSE 8080
ENTRYPOINT ["/service"]
```

### 9.2 Docker Compose (Local Dev)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: whatsapp
      POSTGRES_PASSWORD: whatsapp
      POSTGRES_DB: whatsapp
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongodata:/data/db

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  nats:
    image: nats:2-alpine
    command: ["--jetstream", "--store_dir=/data"]
    ports:
      - "4222:4222"
      - "8222:8222"
    volumes:
      - natsdata:/data

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - miniodata:/data

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - redis
    environment:
      - AUTH_SERVICE_URL=http://auth-service:8081
      - USER_SERVICE_URL=http://user-service:8082
      - CHAT_SERVICE_URL=http://chat-service:8083
      - MESSAGE_SERVICE_URL=http://message-service:8084
      - MEDIA_SERVICE_URL=http://media-service:8086
      - WEBSOCKET_SERVICE_URL=http://websocket-service:8087
      - REDIS_URL=redis://redis:6379

  auth-service:
    build: ./auth-service
    ports:
      - "8081:8081"
      - "9081:9081"
    depends_on:
      - postgres
      - redis
    environment:
      - DATABASE_URL=postgres://whatsapp:whatsapp@postgres:5432/whatsapp?sslmode=disable
      - REDIS_URL=redis://redis:6379
      - JWT_SECRET=local-dev-secret-change-in-prod

  user-service:
    build: ./user-service
    ports:
      - "8082:8082"
      - "9082:9082"
    depends_on:
      - postgres
      - redis
    environment:
      - DATABASE_URL=postgres://whatsapp:whatsapp@postgres:5432/whatsapp?sslmode=disable
      - REDIS_URL=redis://redis:6379

  chat-service:
    build: ./chat-service
    ports:
      - "8083:8083"
      - "9083:9083"
    depends_on:
      - postgres
      - nats
    environment:
      - DATABASE_URL=postgres://whatsapp:whatsapp@postgres:5432/whatsapp?sslmode=disable
      - NATS_URL=nats://nats:4222

  message-service:
    build: ./message-service
    ports:
      - "8084:8084"
      - "9084:9084"
    depends_on:
      - mongodb
      - redis
      - nats
    environment:
      - MONGO_URL=mongodb://mongodb:27017/whatsapp
      - REDIS_URL=redis://redis:6379
      - NATS_URL=nats://nats:4222
      - MEDIA_GRPC_ADDR=media-service:9086

  notification-service:
    build: ./notification-service
    ports:
      - "8085:8085"
    depends_on:
      - postgres
      - redis
      - nats
    environment:
      - DATABASE_URL=postgres://whatsapp:whatsapp@postgres:5432/whatsapp?sslmode=disable
      - REDIS_URL=redis://redis:6379
      - NATS_URL=nats://nats:4222
      - FCM_CREDENTIALS_PATH=/run/secrets/fcm.json

  media-service:
    build: ./media-service
    ports:
      - "8086:8086"
      - "9086:9086"
    depends_on:
      - mongodb
      - minio
    environment:
      - MONGO_URL=mongodb://mongodb:27017/whatsapp
      - MINIO_ENDPOINT=minio:9000
      - MINIO_ACCESS_KEY=minioadmin
      - MINIO_SECRET_KEY=minioadmin
      - MINIO_BUCKET=whatsapp-media

  websocket-service:
    build: ./websocket-service
    ports:
      - "8087:8087"
    depends_on:
      - redis
      - nats
    environment:
      - REDIS_URL=redis://redis:6379
      - NATS_URL=nats://nats:4222
      - AUTH_GRPC_ADDR=auth-service:9081
      - MESSAGE_GRPC_ADDR=message-service:9084

volumes:
  pgdata:
  mongodata:
  natsdata:
  miniodata:
```

### 9.3 Kubernetes (Kind Cluster)

**Cluster Setup**:

```bash
kind create cluster --name whatsapp --config kind-config.yaml
```

**kind-config.yaml**:

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30080
        hostPort: 8080
        protocol: TCP
  - role: worker
  - role: worker
```

### 9.4 Helm Charts

```
helm/
├── Chart.yaml
├── values.yaml
└── charts/
    ├── api-gateway/
    ├── auth-service/
    ├── user-service/
    ├── chat-service/
    ├── message-service/
    ├── notification-service/
    ├── media-service/
    ├── websocket-service/
    ├── postgresql/
    ├── mongodb/
    ├── redis/
    ├── nats/
    └── minio/
```

Each service sub-chart includes:

- `Deployment` (replicas, resource limits, health checks)
- `Service` (ClusterIP)
- `ConfigMap` (environment variables)
- `Secret` (credentials)
- `HorizontalPodAutoscaler` (optional)

### 9.5 ngrok

```bash
ngrok http 8080 --domain=<your-static-domain>.ngrok-free.app
```

The static ngrok domain is configured in the Android client as the base URL.

---

## 10. Non-Functional Requirements

### 10.1 Performance

| Metric | Target |
|--------|--------|
| Message delivery latency (sender → recipient) | < 500ms (online-to-online) |
| REST API p95 latency | < 200ms |
| WebSocket connection setup | < 1s |
| Message history page load | < 300ms |
| Media upload (16 MB image) | < 5s |
| Concurrent WebSocket connections per instance | 10,000 |

### 10.2 Scalability

| Service | Scaling Strategy |
|---------|-----------------|
| api-gateway | Horizontal (stateless) |
| auth-service | Horizontal (stateless after Redis session) |
| user-service | Horizontal (stateless) |
| chat-service | Horizontal (stateless) |
| message-service | Horizontal (stateless) |
| notification-service | Horizontal (consumer groups via NATS) |
| media-service | Horizontal (stateless, MinIO scales separately) |
| websocket-service | Horizontal (Redis Pub-Sub for cross-instance fan-out) |

### 10.3 Observability

| Pillar | Tool | Details |
|--------|------|---------|
| **Logging** | zerolog | Structured JSON logs, correlation via `request_id` |
| **Metrics** | Prometheus + Grafana | RED metrics (Rate, Errors, Duration) per endpoint |
| **Tracing** | OpenTelemetry + Jaeger | Distributed tracing across gRPC and NATS hops |
| **Health Checks** | `/health` (liveness), `/ready` (readiness) | Per-service, checked by K8s probes |

### 10.4 Reliability

- **Idempotency**: `client_msg_id` ensures duplicate messages are detected and deduplicated
- **At-least-once delivery**: NATS JetStream with manual ack ensures events are not lost
- **Graceful shutdown**: Services drain in-flight requests and close connections on SIGTERM
- **Circuit breaker**: gRPC calls wrapped with circuit breaker (e.g., `sony/gobreaker`)
- **Retry with backoff**: Failed NATS publishes and FCM calls use exponential backoff

---

## 11. Security

### 11.1 Authentication & Authorization

| Aspect | Implementation |
|--------|---------------|
| User authentication | Phone + OTP → JWT |
| Token validation | Gateway middleware validates JWT on every request |
| Token refresh | Opaque refresh token stored in Redis (revocable) |
| Inter-service auth | Trusted internal network (no auth) in K8s; mTLS as stretch goal |

### 11.2 Input Validation

- All request bodies validated with struct tags (`binding:"required"` in Gin)
- Phone numbers validated against E.164 format
- UUIDs validated before DB queries
- File uploads validated for MIME type (magic bytes, not just extension)
- SQL injection prevention via parameterized queries (pgx)
- NoSQL injection prevention via typed queries (MongoDB Go driver)

### 11.3 Rate Limiting

| Scope | Limit | Window |
|-------|-------|--------|
| OTP send per phone | 3 requests | 5 minutes |
| OTP verify per phone | 5 attempts | 5 minutes |
| API per user (general) | 100 requests | 1 minute |
| WebSocket messages per user | 30 messages | 10 seconds |
| Media upload per user | 10 uploads | 1 minute |

### 11.4 CORS

```go
cors.Config{
    AllowOrigins:     []string{"*"}, // Restricted in production
    AllowMethods:     []string{"GET", "POST", "PATCH", "DELETE", "OPTIONS"},
    AllowHeaders:     []string{"Authorization", "Content-Type"},
    ExposeHeaders:    []string{"X-Request-Id"},
    AllowCredentials: true,
    MaxAge:           12 * time.Hour,
}
```

### 11.5 Data Protection

- Passwords not applicable (OTP-based auth)
- OTP hashed before storage in Redis (bcrypt)
- Refresh tokens stored as SHA-256 hash
- Presigned URLs for media (time-limited, 1 hour expiry)
- No sensitive data in JWT payload beyond `user_id` and `phone`
- End-to-end encryption: **stretch goal** (Signal Protocol)

---

## 12. Phased Delivery Roadmap

| Phase | Services | Features | Timeline |
|-------|----------|----------|----------|
| **Phase 1 (MVP)** | api-gateway, auth-service, user-service, message-service, websocket-service | OTP auth, JWT tokens, user profiles, 1-to-1 text messaging via WebSocket, message persistence (MongoDB), message history REST endpoint, basic presence | Week 1-3 |
| **Phase 2** | chat-service, notification-service | Group chat creation, group admin controls, chat list with last message, FCM push notifications, message status (delivered/read) | Week 4-6 |
| **Phase 3** | media-service | Image/video/document upload, thumbnails (FFmpeg), MinIO storage, presigned download URLs, voice notes | Week 7-8 |
| **Phase 4** | All services (refinement) | Typing indicators, contact sync, quoted replies, message forwarding, message deletion, search (MongoDB text index), privacy settings | Week 9-10 |
| **Phase 5 (Stretch)** | New services | E2EE (Signal Protocol), WebRTC calls service, stories service, message reactions | Week 11+ |

### Infrastructure Milestones

| Milestone | Deliverable | Timeline |
|-----------|-------------|----------|
| **M1** | Docker Compose dev environment fully functional | Week 1 |
| **M2** | All services running with hot-reload (Air) | Week 2 |
| **M3** | Helm charts for all services | Week 6 |
| **M4** | Kind cluster deployment tested | Week 7 |
| **M5** | Observability stack (Prometheus + Grafana + Jaeger) | Week 8 |

---

## 13. Cross-PRD Alignment Notes

This section documents the canonical API contract between the backend and client. **The backend PRD is the authoritative source for all API endpoints, WebSocket events, and data formats.** The client PRD should be updated to match if discrepancies exist.

### 13.1 API Endpoint Mapping

| Resource | Backend Canonical Path | Notes |
|----------|----------------------|-------|
| Request OTP | `POST /api/v1/auth/request-otp` | |
| Verify OTP | `POST /api/v1/auth/verify-otp` | |
| Refresh Token | `POST /api/v1/auth/refresh` | |
| Logout | `POST /api/v1/auth/logout` | Also unregisters FCM token |
| Get Profile | `GET /api/v1/users/me` | |
| Update Profile | `PATCH /api/v1/users/me` | PATCH (partial update), not PUT |
| FCM Token | `PUT /api/v1/users/me/fcm-token` | Managed by user-service, not notification-service |
| Privacy Settings | `PUT /api/v1/users/me/privacy` | |
| Create Chat | `POST /api/v1/chats` | Handles both direct and group via `type` field |
| Message History | `GET /api/v1/chats/:chatId/messages` | Nested under chats |
| Send Message (REST) | `POST /api/v1/chats/:chatId/messages` | REST fallback; primary path is WebSocket |
| Forward Message | `POST /api/v1/messages/:messageId/forward` | |
| Group Operations | `PATCH /api/v1/chats/:id` | Groups are a type of chat; no separate `/groups` prefix |

### 13.2 WebSocket Event Name Mapping

| Wire Name (Backend) | Client Model Name | Direction |
|---------------------|-------------------|-----------|
| `message.send` | `SendMessage` | Client -> Server |
| `message.new` | `NewMessage` | Server -> Client |
| `message.sent` | `MessageSent` (ack) | Server -> Client |
| `message.status` | `MessageStatus` | Server -> Client |
| `message.delivered` | `MarkDelivered` | Client -> Server |
| `message.read` | `MarkRead` | Client -> Server |
| `message.deleted` | `MessageDeleted` | Server -> Client |
| `message.delete` | `DeleteMessage` | Client -> Server |
| `typing.start` / `typing.stop` | `Typing` | Client -> Server |
| `typing` | `Typing` | Server -> Client |
| `presence` | `Presence` | Server -> Client |
| `presence.subscribe` | `PresenceSubscribe` | Client -> Server |
| `chat.created` | `ChatCreated` | Server -> Client |
| `chat.updated` | `ChatUpdated` | Server -> Client |
| `group.member.added` | `GroupMemberAdded` | Server -> Client |
| `group.member.removed` | `GroupMemberRemoved` | Server -> Client |
| `ping` / `pong` | `Ping` / `Pong` | Bidirectional |

---

## 14. Glossary

| Term | Definition |
|------|------------|
| **Chat** | A conversation thread, either 1-to-1 (direct) or group |
| **Direct Chat** | A private conversation between exactly two users |
| **Group Chat** | A conversation with 3–256 members, managed by admins |
| **Presence** | A user's online/offline status and last-seen timestamp |
| **Typing Indicator** | Real-time signal that a user is composing a message |
| **Client Message ID** | A UUID generated by the client for idempotent message sends |
| **Presigned URL** | A time-limited URL granting temporary access to a MinIO object |
| **Fan-out** | Distributing a single event to multiple recipients via Pub-Sub |
| **Dead Letter** | A message that failed processing after max retries |
