# API Reference

Complete reference for all REST and WebSocket APIs exposed by the WhatsApp Clone backend. All requests go through the API Gateway at `http://<host>:30080`.

---

## Authentication

All endpoints except `/api/v1/auth/*` require a valid JWT in the `Authorization` header:

```
Authorization: Bearer <access_token>
```

The gateway validates the token and injects `X-User-ID` into downstream requests.

---

## Auth Service — `/api/v1/auth`

| Method | Path | Description | Auth |
|--------|------|-------------|:----:|
| POST | `/api/v1/auth/request-otp` | Request OTP for phone number | No |
| POST | `/api/v1/auth/verify-otp` | Verify OTP and get tokens | No |
| POST | `/api/v1/auth/refresh` | Refresh access token | No |
| POST | `/api/v1/auth/logout` | Invalidate refresh token | Yes |

### POST `/api/v1/auth/request-otp`

Request an OTP code sent to the given phone number.

**Request:**
```json
{
  "phone": "+1234567890"
}
```

**Response (200):**
```json
{
  "message": "OTP sent successfully",
  "expires_in": 300
}
```

### POST `/api/v1/auth/verify-otp`

Verify the OTP and receive authentication tokens.

**Request:**
```json
{
  "phone": "+1234567890",
  "code": "123456"
}
```

**Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "phone": "+1234567890",
    "display_name": "",
    "avatar_url": "",
    "created_at": "2026-02-18T12:00:00Z"
  }
}
```

### POST `/api/v1/auth/refresh`

Refresh an expired access token.

**Request:**
```json
{
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2..."
}
```

**Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "bmV3IHJlZnJlc2ggdG9rZW4..."
}
```

### POST `/api/v1/auth/logout`

Invalidate the current refresh token.

**Request:**
```json
{
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2..."
}
```

**Response (200):**
```json
{
  "message": "Logged out successfully"
}
```

---

## User Service — `/api/v1/users`

| Method | Path | Description | Auth |
|--------|------|-------------|:----:|
| GET | `/api/v1/users/me` | Get current user profile | Yes |
| PUT | `/api/v1/users/me` | Update profile | Yes |
| PUT | `/api/v1/users/me/avatar` | Upload avatar | Yes |
| GET | `/api/v1/users/:id` | Get user by ID | Yes |
| GET | `/api/v1/users/:id/presence` | Get user presence | Yes |
| POST | `/api/v1/users/contacts/sync` | Sync contacts | Yes |
| GET | `/api/v1/users/contacts` | List contacts | Yes |
| POST | `/api/v1/users/contacts/:id/block` | Block user | Yes |
| DELETE | `/api/v1/users/contacts/:id/block` | Unblock user | Yes |
| GET | `/api/v1/users/privacy` | Get privacy settings | Yes |
| PUT | `/api/v1/users/privacy` | Update privacy settings | Yes |
| POST | `/api/v1/users/devices` | Register device token | Yes |
| DELETE | `/api/v1/users/devices/:token` | Remove device token | Yes |

### GET `/api/v1/users/me`

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "phone": "+1234567890",
  "display_name": "Alice",
  "avatar_url": "https://minio:9000/whatsapp-media/avatars/alice.jpg",
  "status_text": "Hey there! I'm using WhatsApp Clone",
  "created_at": "2026-02-18T12:00:00Z"
}
```

### PUT `/api/v1/users/me`

**Request:**
```json
{
  "display_name": "Alice Smith",
  "status_text": "Available"
}
```

### POST `/api/v1/users/contacts/sync`

Sync the device's address book to find registered users.

**Request:**
```json
{
  "phone_numbers": ["+1111111111", "+2222222222", "+3333333333"]
}
```

**Response (200):**
```json
{
  "contacts": [
    {
      "user_id": "user-1",
      "phone": "+1111111111",
      "display_name": "Bob",
      "avatar_url": "..."
    },
    {
      "user_id": "user-3",
      "phone": "+3333333333",
      "display_name": "Charlie",
      "avatar_url": "..."
    }
  ]
}
```

### GET `/api/v1/users/:id/presence`

**Response (200):**
```json
{
  "user_id": "user-1",
  "online": true,
  "last_seen": "2026-02-18T12:00:00Z"
}
```

### PUT `/api/v1/users/privacy`

**Request:**
```json
{
  "last_seen": "contacts",
  "profile_photo": "everyone",
  "about": "nobody",
  "read_receipts": true
}
```

### POST `/api/v1/users/devices`

Register an FCM device token for push notifications.

**Request:**
```json
{
  "token": "fcm-device-token-here",
  "platform": "android"
}
```

---

## Chat Service — `/api/v1/chats`

| Method | Path | Description | Auth |
|--------|------|-------------|:----:|
| POST | `/api/v1/chats` | Create chat | Yes |
| GET | `/api/v1/chats` | List user's chats | Yes |
| GET | `/api/v1/chats/:id` | Get chat details | Yes |
| PATCH | `/api/v1/chats/:id` | Update group info | Yes |
| POST | `/api/v1/chats/:id/participants` | Add member | Yes |
| DELETE | `/api/v1/chats/:id/participants/:userId` | Remove member | Yes |
| PATCH | `/api/v1/chats/:id/participants/:userId/role` | Change role | Yes |
| PUT | `/api/v1/chats/:id/mute` | Mute/unmute | Yes |
| PUT | `/api/v1/chats/:id/pin` | Pin/unpin | Yes |
| PUT | `/api/v1/chats/:id/avatar` | Upload group avatar | Yes |
| PUT | `/api/v1/chats/:id/disappearing` | Set disappearing timer | Yes |

### POST `/api/v1/chats`

Create a direct or group chat.

**Direct chat:**
```json
{
  "type": "direct",
  "participant_id": "user-2"
}
```

**Group chat:**
```json
{
  "type": "group",
  "name": "Project Team",
  "description": "Discussion about the project",
  "participant_ids": ["user-2", "user-3", "user-4"]
}
```

**Response (201):**
```json
{
  "chat": {
    "id": "chat-1",
    "type": "group",
    "created_at": "2026-02-18T12:00:00Z"
  },
  "group": {
    "name": "Project Team",
    "description": "Discussion about the project",
    "created_by": "user-1"
  },
  "participants": [
    { "user_id": "user-1", "role": "admin" },
    { "user_id": "user-2", "role": "member" },
    { "user_id": "user-3", "role": "member" },
    { "user_id": "user-4", "role": "member" }
  ]
}
```

### GET `/api/v1/chats`

List all chats for the authenticated user, with last message preview.

**Query params:**
- `page` (int, default 1)
- `limit` (int, default 20)

**Response (200):**
```json
{
  "chats": [
    {
      "id": "chat-1",
      "type": "group",
      "name": "Project Team",
      "avatar_url": "...",
      "last_message": {
        "message_id": "msg-99",
        "sender_id": "user-2",
        "sender_name": "Bob",
        "content": "See you at 3!",
        "type": "text",
        "timestamp": "2026-02-18T11:55:00Z"
      },
      "unread_count": 3,
      "is_muted": false,
      "is_pinned": true,
      "updated_at": "2026-02-18T11:55:00Z"
    }
  ],
  "total": 15,
  "page": 1,
  "limit": 20
}
```

### POST `/api/v1/chats/:id/participants`

Add a member to a group chat (admin only).

**Request:**
```json
{
  "user_id": "user-5"
}
```

### PATCH `/api/v1/chats/:id/participants/:userId/role`

**Request:**
```json
{
  "role": "admin"
}
```

### PUT `/api/v1/chats/:id/mute`

**Request:**
```json
{
  "muted": true,
  "mute_until": "2026-02-19T12:00:00Z"
}
```

### PUT `/api/v1/chats/:id/disappearing`

**Request:**
```json
{
  "timer": 86400
}
```
Timer values: `0` (off), `86400` (24h), `604800` (7d), `7776000` (90d)

---

## Message Service — `/api/v1/messages`

| Method | Path | Description | Auth |
|--------|------|-------------|:----:|
| POST | `/api/v1/messages` | Send message | Yes |
| GET | `/api/v1/messages?chat_id=...` | List messages (paginated) | Yes |
| POST | `/api/v1/messages/read` | Mark messages as read | Yes |
| GET | `/api/v1/messages/search?chat_id=...&q=...` | Search in chat | Yes |
| GET | `/api/v1/messages/search-global?q=...` | Global search | Yes |
| DELETE | `/api/v1/messages/:messageId?for=me\|everyone` | Delete message | Yes |
| POST | `/api/v1/messages/:messageId/forward` | Forward message | Yes |
| POST | `/api/v1/messages/:messageId/star` | Star message | Yes |
| DELETE | `/api/v1/messages/:messageId/star` | Unstar message | Yes |
| POST | `/api/v1/messages/:messageId/react` | Add reaction | Yes |
| DELETE | `/api/v1/messages/:messageId/react` | Remove reaction | Yes |
| GET | `/api/v1/messages/:messageId/receipts` | Get delivery receipts | Yes |

### POST `/api/v1/messages`

Send a new message.

**Text message:**
```json
{
  "chat_id": "chat-1",
  "type": "text",
  "content": "Hello, everyone!",
  "client_msg_id": "client-uuid-123"
}
```

**Image message:**
```json
{
  "chat_id": "chat-1",
  "type": "image",
  "media_id": "media-456",
  "content": "Check out this photo"
}
```

**Reply:**
```json
{
  "chat_id": "chat-1",
  "type": "text",
  "content": "I agree!",
  "reply_to_id": "msg-55",
  "client_msg_id": "client-uuid-124"
}
```

**Response (201):**
```json
{
  "message_id": "msg-100",
  "client_msg_id": "client-uuid-123",
  "chat_id": "chat-1",
  "sender_id": "user-1",
  "type": "text",
  "content": "Hello, everyone!",
  "status": "sent",
  "created_at": "2026-02-18T12:00:00Z"
}
```

### GET `/api/v1/messages?chat_id=...`

Retrieve messages with cursor-based pagination.

**Query params:**
- `chat_id` (required)
- `before` (message ID — fetch messages before this one)
- `after` (message ID — fetch messages after this one)
- `limit` (int, default 50)

**Response (200):**
```json
{
  "messages": [
    {
      "message_id": "msg-100",
      "chat_id": "chat-1",
      "sender_id": "user-1",
      "type": "text",
      "content": "Hello, everyone!",
      "reply_to_id": null,
      "status": {
        "user-2": { "status": "read", "updated_at": "2026-02-18T12:01:00Z" },
        "user-3": { "status": "delivered", "updated_at": "2026-02-18T12:00:30Z" }
      },
      "reactions": {
        "user-2": "👍"
      },
      "is_deleted": false,
      "created_at": "2026-02-18T12:00:00Z"
    }
  ],
  "has_more": true
}
```

### POST `/api/v1/messages/read`

Mark messages as read.

**Request:**
```json
{
  "chat_id": "chat-1",
  "message_ids": ["msg-98", "msg-99", "msg-100"]
}
```

### DELETE `/api/v1/messages/:messageId`

**Query params:**
- `for` — `me` (hides for caller) or `everyone` (marks as deleted for all)

### POST `/api/v1/messages/:messageId/forward`

**Request:**
```json
{
  "target_chat_ids": ["chat-2", "chat-3"]
}
```

### POST `/api/v1/messages/:messageId/react`

**Request:**
```json
{
  "emoji": "❤️"
}
```

### GET `/api/v1/messages/:messageId/receipts`

**Response (200):**
```json
{
  "receipts": [
    { "user_id": "user-2", "status": "read", "updated_at": "2026-02-18T12:01:00Z" },
    { "user_id": "user-3", "status": "delivered", "updated_at": "2026-02-18T12:00:30Z" },
    { "user_id": "user-4", "status": "sent", "updated_at": "2026-02-18T12:00:05Z" }
  ]
}
```

---

## Media Service — `/api/v1/media`

| Method | Path | Description | Auth |
|--------|------|-------------|:----:|
| POST | `/api/v1/media/upload` | Upload file | Yes |
| GET | `/api/v1/media/:mediaId` | Get media metadata | Yes |
| GET | `/api/v1/media/:mediaId/download` | Download (redirect) | Yes |

### POST `/api/v1/media/upload`

Upload a file using multipart form data.

**Request:**
```
Content-Type: multipart/form-data

file: <binary>
type: "image"    (image | video | audio | document)
```

**Response (201):**
```json
{
  "media_id": "media-456",
  "type": "image",
  "mime_type": "image/jpeg",
  "size_bytes": 245000,
  "url": "https://minio:9000/whatsapp-media/media-456/photo.jpg?X-Amz-Signature=...",
  "thumbnail_url": "https://minio:9000/whatsapp-media/media-456/thumb.jpg?X-Amz-Signature=...",
  "width": 1920,
  "height": 1080,
  "created_at": "2026-02-18T12:00:00Z"
}
```

### GET `/api/v1/media/:mediaId`

Returns metadata with fresh presigned URLs.

**Response (200):**
```json
{
  "media_id": "media-456",
  "type": "image",
  "mime_type": "image/jpeg",
  "size_bytes": 245000,
  "url": "https://minio:9000/...?X-Amz-Signature=...",
  "thumbnail_url": "https://minio:9000/...?X-Amz-Signature=...",
  "width": 1920,
  "height": 1080,
  "duration_ms": null,
  "created_at": "2026-02-18T12:00:00Z"
}
```

### GET `/api/v1/media/:mediaId/download`

Redirects (302) to the presigned download URL.

---

## WebSocket API — `/ws`

### Connection

```
GET /ws?token=<JWT_access_token>

→ 101 Switching Protocols (on success)
→ 401 Unauthorized (on invalid token)
```

### Event Format

All WebSocket messages use a JSON envelope:

```json
{
  "type": "<event_type>",
  "payload": { ... },
  "timestamp": "2026-02-18T12:00:00Z"
}
```

### Client → Server Events

#### `message.send`

Send a new message via WebSocket.

```json
{
  "type": "message.send",
  "payload": {
    "chat_id": "chat-1",
    "type": "text",
    "content": "Hello!",
    "client_msg_id": "client-uuid-123",
    "reply_to_id": null
  }
}
```

#### `message.delete`

```json
{
  "type": "message.delete",
  "payload": {
    "message_id": "msg-100",
    "for": "everyone"
  }
}
```

#### `typing.start`

```json
{
  "type": "typing.start",
  "payload": {
    "chat_id": "chat-1"
  }
}
```

#### `typing.stop`

```json
{
  "type": "typing.stop",
  "payload": {
    "chat_id": "chat-1"
  }
}
```

#### `presence.subscribe`

Subscribe to real-time presence updates for a user.

```json
{
  "type": "presence.subscribe",
  "payload": {
    "user_id": "user-2"
  }
}
```

### Server → Client Events

#### `message.new`

New message received.

```json
{
  "type": "message.new",
  "payload": {
    "message_id": "msg-101",
    "chat_id": "chat-1",
    "sender_id": "user-2",
    "type": "text",
    "content": "Hey there!",
    "created_at": "2026-02-18T12:01:00Z"
  }
}
```

#### `message.sent`

Confirmation that a message was stored by the server.

```json
{
  "type": "message.sent",
  "payload": {
    "client_msg_id": "client-uuid-123",
    "message_id": "msg-100",
    "created_at": "2026-02-18T12:00:00Z"
  }
}
```

#### `message.status.updated`

Delivery or read status changed.

```json
{
  "type": "message.status.updated",
  "payload": {
    "message_id": "msg-100",
    "chat_id": "chat-1",
    "user_id": "user-2",
    "status": "read",
    "updated_at": "2026-02-18T12:01:00Z"
  }
}
```

#### `message.deleted`

```json
{
  "type": "message.deleted",
  "payload": {
    "message_id": "msg-100",
    "chat_id": "chat-1",
    "deleted_by": "user-1",
    "for": "everyone"
  }
}
```

#### `chat.created`

```json
{
  "type": "chat.created",
  "payload": {
    "chat_id": "chat-2",
    "type": "group",
    "name": "New Group",
    "participants": ["user-1", "user-2", "user-3"],
    "created_by": "user-1"
  }
}
```

#### `chat.updated`

```json
{
  "type": "chat.updated",
  "payload": {
    "chat_id": "chat-2",
    "name": "Renamed Group",
    "avatar_url": "..."
  }
}
```

#### `group.member.added`

```json
{
  "type": "group.member.added",
  "payload": {
    "chat_id": "chat-2",
    "user_id": "user-5",
    "added_by": "user-1"
  }
}
```

#### `group.member.removed`

```json
{
  "type": "group.member.removed",
  "payload": {
    "chat_id": "chat-2",
    "user_id": "user-5",
    "removed_by": "user-1"
  }
}
```

#### `presence.updated`

```json
{
  "type": "presence.updated",
  "payload": {
    "user_id": "user-2",
    "online": false,
    "last_seen": "2026-02-18T12:05:00Z"
  }
}
```

#### `typing`

```json
{
  "type": "typing",
  "payload": {
    "chat_id": "chat-1",
    "user_id": "user-2",
    "is_typing": true
  }
}
```

---

## Error Responses

All services return errors in a consistent format:

```json
{
  "error": {
    "code": "CHAT_NOT_FOUND",
    "message": "Chat with ID chat-999 not found"
  }
}
```

### Common HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (invalid input) |
| 401 | Unauthorized (missing or invalid token) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 409 | Conflict (duplicate resource) |
| 429 | Too Many Requests (rate limited) |
| 500 | Internal Server Error |

---

## Health & Metrics

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check (all services) |
| GET | `/metrics` | Prometheus metrics (all services) |

### Health Response

```json
{
  "status": "ok",
  "service": "message-service",
  "uptime": "2h30m15s"
}
```
