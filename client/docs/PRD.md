# WhatsApp Clone - Android Client App PRD

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Goals and Objectives](#2-goals-and-objectives)
3. [User Personas & Stories](#3-user-personas--stories)
4. [Feature Specification](#4-feature-specification)
5. [UI/UX Screen Specifications](#5-uiux-screen-specifications)
6. [Technical Architecture](#6-technical-architecture)
7. [Data Models](#7-data-models)
8. [API Contract Summary](#8-api-contract-summary)
9. [WebSocket Protocol (Client Side)](#9-websocket-protocol-client-side)
10. [Offline Support & Local Caching Strategy](#10-offline-support--local-caching-strategy)
11. [Push Notifications](#11-push-notifications)
12. [Non-Functional Requirements](#12-non-functional-requirements)
13. [Appendix](#13-appendix)

---

## 1. Product Overview

### 1.1 Vision

A lightweight, real-time messaging Android application that replicates the core WhatsApp experience -- 1-to-1 chats, group chats, media sharing, real-time delivery, and push notifications -- built entirely with modern Android (Kotlin + Jetpack Compose) and connecting to a self-hosted backend exposed via ngrok.

### 1.2 Scope

| In Scope | Out of Scope (Stretch / Future) |
|---|---|
| Phone + OTP authentication | End-to-end encryption (E2EE) |
| 1-to-1 text messaging | Voice / Video calls (WebRTC) |
| Group messaging | Stories / Status updates |
| Media sharing (images, videos, docs) | Payment integration |
| Voice notes | Desktop / Web companion app |
| Real-time delivery via WebSocket | Message reactions |
| Push notifications (FCM) | Disappearing messages |
| Message status (sent/delivered/read) | Broadcast lists |
| Typing indicators & online/last seen | |
| User profile management | |
| Contact sync & discovery | |

### 1.3 Target Platform

- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 34 (Android 14)
- **Language**: Kotlin 1.9+
- **UI Toolkit**: Jetpack Compose (Material 3)

---

## 2. Goals and Objectives

| # | Goal | Success Metric |
|---|---|---|
| G1 | Deliver messages in real time | Message delivery latency < 500ms on stable network |
| G2 | Reliable offline experience | Messages queued offline are delivered within 5s of reconnection |
| G3 | Smooth, responsive UI | 60 fps scrolling on chat lists with 1000+ conversations |
| G4 | Battery efficiency | < 2% battery drain per hour in idle with WebSocket connected |
| G5 | Fast app startup | Cold start to chat list < 2 seconds |

---

## 3. User Personas & Stories

### 3.1 Personas

| Persona | Description |
|---|---|
| **Casual User (Priya)** | College student, uses the app for daily texting with friends and family. Expects instant delivery, easy media sharing, and group chats for class projects. |
| **Power User (Rahul)** | Small business owner who manages 20+ group chats for coordinating with vendors and staff. Needs reliable notifications, fast search, and admin controls. |
| **Privacy-Conscious User (Amit)** | Developer who self-hosts the backend. Cares about data ownership, wants to see "last seen" controls and control over read receipts. |

### 3.2 User Stories

#### Authentication
| ID | Story | Priority |
|---|---|---|
| US-A1 | As a new user, I want to register with my phone number so I can start messaging. | Must |
| US-A2 | As a returning user, I want to log in via OTP so I don't need to remember a password. | Must |
| US-A3 | As a user, I want my session to persist across app restarts so I don't re-authenticate every time. | Must |
| US-A4 | As a user, I want to log out from my account. | Must |

#### Messaging
| ID | Story | Priority |
|---|---|---|
| US-M1 | As a user, I want to send and receive text messages in real time. | Must |
| US-M2 | As a user, I want to see message status (single tick = sent, double tick = delivered, blue tick = read). | Must |
| US-M3 | As a user, I want to send images, videos, and documents. | Must |
| US-M4 | As a user, I want to record and send voice notes. | Should |
| US-M5 | As a user, I want to see a typing indicator when the other person is typing. | Should |
| US-M6 | As a user, I want to reply to a specific message (quoted reply). | Should |
| US-M7 | As a user, I want to forward a message to another chat. | Could |
| US-M8 | As a user, I want to delete a message for myself or for everyone. | Could |
| US-M9 | As a user, I want to search messages within a chat. | Could |

#### Group Chat
| ID | Story | Priority |
|---|---|---|
| US-G1 | As a user, I want to create a group and add members from my contacts. | Must |
| US-G2 | As a group admin, I want to add/remove members. | Must |
| US-G3 | As a group admin, I want to change group name and icon. | Should |
| US-G4 | As a group member, I want to leave a group. | Must |
| US-G5 | As a group admin, I want to promote/demote other admins. | Should |
| US-G6 | As a group admin, I want to control who can send messages (admin-only mode). | Could |

#### Profile & Contacts
| ID | Story | Priority |
|---|---|---|
| US-P1 | As a user, I want to set my display name and profile picture. | Must |
| US-P2 | As a user, I want to set a text status/about. | Should |
| US-P3 | As a user, I want to see my contacts who are also on the app. | Must |
| US-P4 | As a user, I want to control my "last seen" and "online" visibility. | Should |
| US-P5 | As a user, I want to block/unblock contacts. | Should |

#### Notifications
| ID | Story | Priority |
|---|---|---|
| US-N1 | As a user, I want to receive push notifications for new messages when the app is in the background. | Must |
| US-N2 | As a user, I want to mute notifications for specific chats. | Should |
| US-N3 | As a user, I want notification badges showing unread message counts. | Must |

---

## 4. Feature Specification

### 4.1 Authentication (Must Have)

#### 4.1.1 Phone Number Registration & OTP Login

- User enters phone number with country code picker (composed into E.164 format, e.g. `+1234567890`).
- Backend sends a 6-digit OTP via SMS (simulated/mocked in local dev; Twilio in prod).
- User enters OTP; backend validates and returns JWT access token + opaque refresh token.
- Tokens stored securely in Android `EncryptedSharedPreferences`.
- Access token sent as `Authorization: Bearer <token>` on all API requests.
- Refresh token used to silently renew expired access tokens.
- Auto-logout if refresh token expires (configurable TTL, default 30 days).

#### 4.1.2 Session Management

- On app launch, check for valid access token in local storage.
- If expired, attempt silent refresh using refresh token.
- If refresh fails, redirect to login screen.
- FCM device token registered with backend on every login and token refresh.

### 4.2 1-to-1 Messaging (Must Have)

#### 4.2.1 Text Messages

- Compose bar at the bottom of the chat screen with a text input and send button.
- Messages sent via WebSocket for real-time delivery.
- If WebSocket is disconnected, messages are queued locally (Room DB) and sent on reconnection.
- Messages displayed in a scrollable list, grouped by date.
- Own messages aligned right (green bubble), received messages aligned left (white bubble).

#### 4.2.2 Message Status

```
Status Flow:
  PENDING -> SENT -> DELIVERED -> READ

  PENDING   : Message saved locally, not yet acknowledged by server (clock icon)
  SENT      : Server acknowledged receipt (single grey tick)
  DELIVERED : Recipient device received the message (double grey ticks)
  READ      : Recipient opened the chat and viewed the message (double blue ticks)
```

- Status updates received via WebSocket events.
- Batch "read" acknowledgement: when user opens a chat, send a single `mark_read` event for all unread messages in that chat.

#### 4.2.3 Media Messages

- Tap attachment icon to open media picker (camera, gallery, document, voice note).
- Media files uploaded to backend via multipart HTTP POST (`/api/v1/media/upload`).
- Backend returns a `media_id` and `media_url`.
- Message payload contains `media_id`, `media_url`, `media_type`, `thumbnail_url`, and optional `caption`.
- Images: compress to max 1600px wide, JPEG quality 80 before upload.
- Videos: compress to 720p, max 16MB.
- Documents: pass-through, max 100MB.
- Show upload progress bar in the chat bubble.
- Media downloaded lazily; thumbnails loaded immediately.

#### 4.2.4 Voice Notes

- Long-press mic button to record.
- Slide left to cancel.
- Recording uses `MediaRecorder` with AAC codec.
- Uploaded same as media; displayed with waveform visualization and play/pause button.
- Duration displayed on the bubble.

### 4.3 Group Messaging (Must Have)

#### 4.3.1 Group Creation

- User taps "New Group" from chat list.
- Contact picker screen: multi-select contacts.
- Set group name and optional group icon.
- Backend creates the group and returns `group_id`.
- System message: "You created group '<name>'" shown in chat.

#### 4.3.2 Group Admin Controls

- Creator is the first admin.
- Admins can: add/remove members, promote/demote admins, edit group info, toggle "admin-only messaging".
- Non-admins can: send messages (unless restricted), view group info, leave group.
- System messages for all membership changes ("Alice added Bob", "Charlie left").

#### 4.3.3 Group Messaging Behavior

- Same message composing UX as 1-to-1.
- Each message shows sender name (color-coded) above the bubble.
- Message status: sent = server received; delivered/read = aggregated across all members (show double tick when all members received; blue when all read).

### 4.4 Real-Time Communication (Must Have)

#### 4.4.1 WebSocket Connection

- Established on app launch after successful authentication.
- URL: `wss://<ngrok-host>/ws?token=<jwt>`
- Automatic reconnection with exponential backoff (1s, 2s, 4s, 8s, max 30s).
- Heartbeat `ping` every **25 seconds**; server responds with `pong`.
- If no `pong` received within **10 seconds**, consider connection dead and reconnect.
- Server closes the connection if no `ping` is received within **60 seconds**.

#### 4.4.2 WebSocket Event Types (Client Receives / Server → Client)

| Event | Payload | Description |
|---|---|---|
| `message.new` | `{ message_id, chat_id, sender_id, type, payload, timestamp }` | New incoming message |
| `message.sent` | `{ message_id, client_msg_id, timestamp }` | Ack for a message the client sent |
| `message.status` | `{ message_id, user_id, status, timestamp }` | Status change (delivered/read) |
| `message.deleted` | `{ message_id, chat_id }` | A message was deleted |
| `typing` | `{ chat_id, user_id, is_typing }` | Typing indicator from another user |
| `presence` | `{ user_id, online, last_seen }` | User online/offline status change |
| `chat.created` | `{ chat_id, type, name, participants }` | Added to a new chat/group |
| `chat.updated` | `{ chat_id, changes }` | Chat metadata updated |
| `group.member.added` | `{ chat_id, user_id, display_name }` | Member joined group |
| `group.member.removed` | `{ chat_id, user_id }` | Member left/removed from group |
| `pong` | `{}` | Server heartbeat response |
| `error` | `{ code, message }` | Error response |

#### 4.4.3 WebSocket Event Types (Client Sends / Client → Server)

| Event | Payload | Description |
|---|---|---|
| `message.send` | `{ chat_id, client_msg_id, type, payload }` | Send a new message |
| `message.delivered` | `{ message_ids: [...] }` | Acknowledge delivery of messages |
| `message.read` | `{ chat_id, up_to_message_id }` | Mark messages as read up to a given ID |
| `typing.start` | `{ chat_id }` | Start typing indicator |
| `typing.stop` | `{ chat_id }` | Stop typing indicator |
| `presence.subscribe` | `{ user_ids: [...] }` | Subscribe to presence updates for specific users |
| `ping` | `{}` | Client heartbeat |

### 4.5 User Profile (Must Have)

- Profile screen accessible from Settings.
- Editable fields: display name (max 64 chars), about/status (max 140 chars), profile picture.
- Profile picture: crop to square, compress, upload via media service.
- Privacy settings toggleable:
  - Last seen: Everyone / My Contacts / Nobody
  - Profile photo: Everyone / My Contacts / Nobody
  - Read receipts: On / Off

### 4.6 Contacts & Discovery (Must Have)

- On first launch (after auth), request contacts permission.
- Upload phone numbers to backend via `POST /api/v1/users/contacts/sync` for matching.
- Backend returns list of registered users matching the contact list.
- Contacts cached locally in Room DB; periodic sync (every 24h or on-demand pull-to-refresh).
- New contact discovery: when a new user registers, backend sends push notification to users who have that number in their contacts.

### 4.7 Typing Indicators & Presence (Should Have)

- When user starts typing, send `typing.start: { chat_id }` via WebSocket.
- Debounce: send `typing.stop: { chat_id }` after 3 seconds of inactivity.
- Display "typing..." under contact name in chat header.
- In group chats: "Alice is typing..." or "Alice, Bob are typing...".
- Online/last seen shown below contact name on chat screen and profile view.

### 4.8 Quoted Replies (Should Have)

- Long-press or swipe-right on a message to reply.
- Quoted message preview shown above compose bar while composing.
- In the chat, the replied message is shown as a small card above the reply bubble.
- Tapping the quoted card scrolls to and highlights the original message.

### 4.9 Message Deletion (Could Have)

- Long-press a message -> "Delete" option.
- Options: "Delete for me" (local only) or "Delete for everyone" (within 1 hour of sending).
- "Delete for everyone" sends a `delete_message` WebSocket event; recipients replace the bubble with "This message was deleted".

### 4.10 Search (Could Have)

- Global search in chat list: search by contact name or message content.
- In-chat search: search within a specific conversation.
- Implemented using local Room DB FTS (Full-Text Search) for instant results.
- Results highlight matched terms and allow jumping to the message in context.

---

## 5. UI/UX Screen Specifications

### 5.1 Screen Map

```
Splash Screen
  |
  v
Login Screen (Phone Entry) --> OTP Verification Screen
  |
  v
Main Screen (Bottom Navigation)
  |--- Chats Tab (Chat List)
  |       |--- Chat Detail Screen
  |       |       |--- Media Viewer (fullscreen image/video)
  |       |       |--- Contact/Group Info Screen
  |       |       |--- Forward Message Picker
  |       |--- New Chat (Contact Picker)
  |       |--- New Group
  |               |--- Group Setup (name, icon)
  |
  |--- Calls Tab (Stretch - placeholder)
  |
  |--- Settings Tab
          |--- Profile Edit Screen
          |--- Privacy Settings
          |--- Notifications Settings
          |--- About / Logout
```

### 5.2 Screen Details

#### 5.2.1 Splash Screen
- App logo centered.
- Auto-navigate: if valid token exists -> Main Screen; otherwise -> Login Screen.
- Max display time: 2 seconds.

#### 5.2.2 Login Screen
- Country code dropdown (default: +91 India).
- Phone number input field (numeric keyboard).
- "Continue" button -> triggers OTP send.
- Loading indicator while OTP is being sent.
- Error handling: invalid number format, rate limiting.

#### 5.2.3 OTP Verification Screen
- 6-digit OTP input (auto-focus, auto-advance).
- "Resend OTP" link with countdown timer (60s).
- Auto-submit on entering 6th digit.
- Error: "Invalid OTP" with shake animation.

#### 5.2.4 Chat List Screen (Home)
- Top App Bar: "WhatsApp" title, search icon, overflow menu (New Group, Settings).
- Floating Action Button: new chat (opens contact picker).
- Scrollable list of conversations, each item shows:
  - Contact/group avatar (circular).
  - Contact/group name (bold if unread).
  - Last message preview (truncated, with sender name for groups).
  - Timestamp of last message (today: time, this week: day name, older: date).
  - Unread message count badge (green circle).
  - Mute icon if muted.
- Pull-to-refresh to sync latest chats.
- Sorted by last message timestamp (newest first).

#### 5.2.5 Chat Detail Screen
- Top App Bar:
  - Back arrow.
  - Avatar + Name (tappable -> opens Contact/Group Info).
  - Subtitle: "online", "last seen today at 3:42 PM", or "typing...".
  - Overflow menu: mute, search, media, clear chat.
- Message list:
  - Lazy-loaded, paginated (load older messages on scroll up).
  - Date separator headers ("Today", "Yesterday", "Feb 14, 2026").
  - Message bubbles with status icons (for sent messages).
  - Scroll-to-bottom FAB appears when scrolled up.
  - Unread message divider: "3 UNREAD MESSAGES".
- Compose bar:
  - Emoji icon (opens emoji picker).
  - Text input (multi-line, max 65,536 chars).
  - Attachment icon (opens bottom sheet: Camera, Gallery, Document, Audio).
  - Send button (appears when text is non-empty, replaces mic icon).
  - Mic button (appears when text is empty, long-press to record voice note).

#### 5.2.6 Contact Picker Screen
- Search bar at top.
- List of registered contacts (alphabetically sorted with letter dividers).
- Each item: avatar, name, about/status.
- Tap to open 1-to-1 chat (create if not exists).

#### 5.2.7 New Group Screen
- Step 1: Multi-select contact picker with selected contacts shown as horizontal chips at the top. "Next" button.
- Step 2: Group name input, optional group icon (camera icon overlay). "Create" button.

#### 5.2.8 Contact/Group Info Screen
- Large avatar at top (tappable to view fullscreen).
- Name, phone number (for contacts) or member count (for groups).
- About/status section.
- Media, Links, Docs tabs (grid/list of shared media).
- For groups: scrollable member list with role badges (Admin).
- Actions: Mute, Block (contacts), Leave Group (groups), Report.

#### 5.2.9 Profile Edit Screen
- Large avatar with camera overlay to change.
- Editable name field.
- Editable about/status field.
- Phone number displayed (non-editable).

#### 5.2.10 Settings Screen
- Profile card (avatar, name, about -- tappable to edit).
- List items: Account, Privacy, Notifications, Storage, Help, Logout.

#### 5.2.11 Media Viewer
- Fullscreen image/video with pinch-to-zoom.
- Top bar: sender name, timestamp, share/forward button.
- Swipe down to dismiss.

### 5.3 Design System

| Element | Specification |
|---|---|
| Primary Color | `#075E54` (WhatsApp teal) |
| Secondary Color | `#25D366` (WhatsApp green) |
| Accent Color | `#34B7F1` (WhatsApp light blue) |
| Chat Background | `#ECE5DD` (WhatsApp doodle bg) |
| Sent Bubble | `#DCF8C6` (light green) |
| Received Bubble | `#FFFFFF` (white) |
| Typography | System default (Roboto), 16sp body, 14sp caption |
| Icons | Material Icons (Filled) |
| Shape | Rounded corners (12dp for cards, 18dp for bubbles) |
| Dark Mode | Support via Material 3 dynamic color (phase 2) |

---

## 6. Technical Architecture

### 6.1 Architecture Pattern

**MVVM + Clean Architecture** with the following layers:

```
┌─────────────────────────────────────────────┐
│                  UI Layer                    │
│  (Jetpack Compose Screens + ViewModels)     │
├─────────────────────────────────────────────┤
│               Domain Layer                   │
│  (Use Cases / Interactors + Domain Models)  │
├─────────────────────────────────────────────┤
│                Data Layer                    │
│  (Repositories + Data Sources)              │
│  ┌──────────────┐  ┌─────────────────────┐  │
│  │ Remote Source │  │   Local Source       │  │
│  │ (Retrofit +  │  │   (Room DB +         │  │
│  │  WebSocket)  │  │   DataStore)         │  │
│  └──────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────┘
```

### 6.2 Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Kotlin | 1.9+ |
| UI Framework | Jetpack Compose + Material 3 | BOM 2024.x |
| Navigation | Compose Navigation | 2.7+ |
| DI | Hilt (Dagger) | 2.50+ |
| Networking (REST) | Retrofit 2 + OkHttp 4 | Latest |
| Networking (WebSocket) | OkHttp WebSocket | 4.12+ |
| JSON Serialization | Kotlinx Serialization | 1.6+ |
| Local Database | Room | 2.6+ |
| Preferences | DataStore (Preferences) | 1.0+ |
| Secure Storage | EncryptedSharedPreferences | 1.1+ |
| Image Loading | Coil (Compose) | 2.5+ |
| Push Notifications | Firebase Cloud Messaging | Latest |
| Media Recorder | Android MediaRecorder API | -- |
| Camera | CameraX | 1.3+ |
| Async | Kotlin Coroutines + Flow | 1.7+ |
| Paging | Paging 3 (Compose) | 3.2+ |
| Build System | Gradle (Kotlin DSL) | 8.x |
| Min SDK | 26 (Android 8) | -- |
| Target SDK | 34 (Android 14) | -- |

### 6.3 Module Structure

```
client/
├── app/                          # Application module (entry point)
│   ├── src/main/
│   │   ├── java/com/whatsapp/clone/
│   │   │   ├── WhatsAppApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   └── navigation/
│   │   │       └── AppNavGraph.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── core/                         # Shared core modules
│   ├── network/                  # Retrofit, OkHttp, WebSocket client
│   │   ├── src/main/java/.../
│   │   │   ├── api/              # Retrofit service interfaces
│   │   │   ├── websocket/        # WebSocket manager, event models
│   │   │   ├── interceptors/     # Auth interceptor, logging
│   │   │   └── di/               # Hilt network module
│   │   └── build.gradle.kts
│   │
│   ├── database/                 # Room DB, DAOs, entities
│   │   ├── src/main/java/.../
│   │   │   ├── entities/
│   │   │   ├── dao/
│   │   │   ├── AppDatabase.kt
│   │   │   └── di/
│   │   └── build.gradle.kts
│   │
│   ├── common/                   # Shared utilities, extensions, constants
│   │   └── build.gradle.kts
│   │
│   └── ui/                       # Shared Compose components, theme
│       ├── src/main/java/.../
│       │   ├── theme/            # Colors, Typography, Shapes
│       │   └── components/       # Reusable composables (Avatar, Badge, etc.)
│       └── build.gradle.kts
│
├── feature/                      # Feature modules
│   ├── auth/                     # Login, OTP verification
│   │   ├── src/main/java/.../
│   │   │   ├── ui/              # Compose screens + ViewModels
│   │   │   ├── domain/          # Use cases
│   │   │   └── data/            # Repository + data sources
│   │   └── build.gradle.kts
│   │
│   ├── chat/                     # Chat list + chat detail
│   │   ├── src/main/java/.../
│   │   │   ├── ui/
│   │   │   │   ├── chatlist/
│   │   │   │   └── chatdetail/
│   │   │   ├── domain/
│   │   │   └── data/
│   │   └── build.gradle.kts
│   │
│   ├── contacts/                 # Contact list + picker
│   ├── group/                    # Group creation + info
│   ├── profile/                  # Profile view + edit
│   ├── media/                    # Media viewer + picker
│   └── settings/                 # Settings screens
│
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Module includes
└── gradle.properties
```

### 6.4 Dependency Injection Graph (Hilt)

```
@HiltAndroidApp WhatsAppApplication
│
├── @Singleton NetworkModule
│   ├── OkHttpClient (with AuthInterceptor)
│   ├── Retrofit instance (base URL from BuildConfig)
│   ├── WebSocketManager
│   ├── AuthApi, UserApi, ChatApi, MessageApi, MediaApi, NotificationApi
│   └── Kotlinx Json instance
│
├── @Singleton DatabaseModule
│   ├── AppDatabase (Room)
│   ├── UserDao, ChatDao, MessageDao, ContactDao
│   └── DataStore<Preferences>
│
├── @Singleton RepositoryModule
│   ├── AuthRepository
│   ├── UserRepository
│   ├── ChatRepository
│   ├── MessageRepository
│   ├── ContactRepository
│   └── MediaRepository
│
└── @Singleton NotificationModule
    └── FCMTokenManager
```

### 6.5 Networking Layer

#### 6.5.1 REST API Client (Retrofit)

```kotlin
// Base URL configured per build variant
// Debug: https://<ngrok-subdomain>.ngrok.io/api/v1/
// The ngrok URL is configurable at runtime via Settings screen.

interface AuthApi {
    @POST("auth/otp/send")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<SendOtpResponse>

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthTokenResponse>

    @POST("auth/token/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<AuthTokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
```

#### 6.5.2 Auth Interceptor

```kotlin
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = tokenManager.getAccessToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            // Attempt token refresh
            val newToken = runBlocking { tokenManager.refreshToken() }
            if (newToken != null) {
                val retryRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(retryRequest)
            }
        }
        return response
    }
}
```

#### 6.5.3 WebSocket Manager

```kotlin
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val json: Json
) {
    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<WebSocketEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    private var reconnectAttempt = 0
    private val maxReconnectDelay = 30_000L

    fun connect() { /* ... */ }
    fun disconnect() { /* ... */ }
    fun send(event: ClientWebSocketEvent) { /* ... */ }
    private fun scheduleReconnect() { /* exponential backoff */ }
}
```

### 6.6 Data Flow

```
User Action (Compose UI)
    |
    v
ViewModel (StateFlow / MutableStateFlow)
    |
    v
Use Case (Domain Layer)
    |
    v
Repository (Data Layer)
    |
    ├──> Remote Data Source (Retrofit / WebSocket)
    |         |
    |         v
    |    Backend API (via ngrok)
    |
    └──> Local Data Source (Room DB)
              |
              v
         SQLite (on-device)
```

**Reactive data flow**: Repositories expose `Flow<T>` from Room DB. ViewModels collect these flows. When new data arrives from the network/WebSocket, the repository writes to Room DB, which automatically triggers UI updates via the Flow.

---

## 7. Data Models

### 7.1 Room Database Entities

#### User
```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,                 // UUID from backend
    val phone: String,              // E.164 format
    val displayName: String,
    val statusText: String?,        // "Hey there! I am using WhatsApp."
    val avatarUrl: String?,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,     // epoch millis
    val isBlocked: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Contact
```kotlin
@Entity(
    tableName = "contacts",
    indices = [Index(value = ["phone"], unique = true)]
)
data class ContactEntity(
    @PrimaryKey
    val contactId: String,
    val phone: String,              // E.164 format
    val deviceName: String,         // Name from phone contacts
    val registeredUserId: String?,  // Null if not on the app
    val isSynced: Boolean = false,
    val syncedAt: Long? = null
)
```

#### Chat
```kotlin
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val chatId: String,             // UUID
    val chatType: String,           // "direct" | "group"
    val lastMessageId: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val muteUntil: Long? = null,    // Null = muted indefinitely
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### ChatParticipant
```kotlin
@Entity(
    tableName = "chat_participants",
    primaryKeys = ["chatId", "userId"]
)
data class ChatParticipantEntity(
    val chatId: String,
    val userId: String,
    val role: String = "member",    // "admin" | "member"
    val joinedAt: Long
)
```

#### Message
```kotlin
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatId", "timestamp"]),
        Index(value = ["chatId", "status"]),
        Index(value = ["clientMsgId"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey
    val messageId: String,          // UUID from server (or client-generated for pending)
    val clientMsgId: String,        // Client-generated UUID (idempotency key)
    val chatId: String,
    val senderId: String,
    val messageType: String,        // "text" | "image" | "video" | "audio" | "document" | "system"
    val content: String?,           // Text content or caption
    val mediaId: String? = null,
    val mediaUrl: String? = null,
    val mediaThumbnailUrl: String? = null,
    val mediaMimeType: String? = null,
    val mediaSize: Long? = null,    // bytes
    val mediaDuration: Int? = null,  // milliseconds (audio/video)
    val replyToMessageId: String? = null,
    val status: String = "pending", // "pending" | "sent" | "delivered" | "read"
    val isDeleted: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val isStarred: Boolean = false,
    val timestamp: Long,            // Server timestamp (or local for pending)
    val createdAt: Long             // Local creation time
)
```

#### Group
```kotlin
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val chatId: String,             // References chats.chatId (1:1 relationship)
    val name: String,
    val description: String?,
    val avatarUrl: String?,
    val createdBy: String,          // userId
    val isAdminOnly: Boolean = false,
    val memberCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
```

> **Note**: Group members are stored in the `chat_participants` table (see ChatParticipant entity above). The backend uses a unified `chat_participants` model for both direct and group chats. The `role` field distinguishes admins from members.

#### Media
```kotlin
@Entity(
    tableName = "media",
    indices = [Index(value = ["mediaId"], unique = true)]
)
data class MediaEntity(
    @PrimaryKey
    val mediaId: String,            // UUID from backend
    val uploaderId: String,
    val fileType: String,           // "image" | "video" | "audio" | "document"
    val mimeType: String,
    val originalFilename: String?,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Int? = null,
    val storageUrl: String?,        // Presigned download URL (time-limited)
    val thumbnailUrl: String?,
    val localPath: String? = null,  // Local file path after download
    val localThumbnailPath: String? = null,
    val createdAt: Long
)
```

### 7.2 Network DTOs (API Request/Response Models)

```kotlin
// Auth — aligned with backend endpoints
data class SendOtpRequest(val phone: String)               // E.164 format, e.g. "+1234567890"
data class SendOtpResponse(val message: String, val expiresInSeconds: Int)
data class VerifyOtpRequest(val phone: String, val otp: String)
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,                                    // seconds
    val user: UserDto
)
data class RefreshRequest(val refreshToken: String)

// User
data class UserDto(
    val id: String,                                         // UUID
    val phone: String,
    val displayName: String,
    val avatarUrl: String?,
    val statusText: String?,
    val createdAt: String?                                  // ISO8601
)

data class PresenceDto(
    val userId: String,
    val online: Boolean,
    val lastSeen: String?                                   // ISO8601
)

data class ContactSyncRequest(val phoneNumbers: List<String>)
data class ContactSyncResponse(val registeredUsers: List<UserDto>)

// Message
data class MessageDto(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val clientMsgId: String?,                               // Idempotency key
    val type: String,                                       // text, image, video, audio, document, system
    val payload: MessagePayloadDto,
    val status: String,                                     // sent, delivered, read
    val isDeleted: Boolean,
    val isStarred: Boolean,
    val createdAt: String                                   // ISO8601
)
data class MessagePayloadDto(
    val body: String? = null,                               // For text/system messages
    val mediaId: String? = null,                            // For media types
    val caption: String? = null,
    val filename: String? = null,
    val durationMs: Int? = null                             // For audio/video
)
data class SendMessageRequest(
    val clientMsgId: String,
    val type: String,
    val payload: MessagePayloadDto
)
data class MarkReadRequest(val upToMessageId: String)

// Chat
data class ChatDto(
    val chatId: String,
    val type: String,                                       // "direct" | "group"
    val participant: UserDto?,                               // For direct chats
    val name: String?,                                      // For group chats
    val participants: List<ChatParticipantDto>?,             // For group chats
    val lastMessage: ChatLastMessageDto?,
    val unreadCount: Int,
    val isMuted: Boolean,
    val updatedAt: String                                   // ISO8601
)
data class ChatLastMessageDto(
    val messageId: String,
    val senderId: String,
    val type: String,
    val preview: String,
    val timestamp: String                                   // ISO8601
)
data class ChatParticipantDto(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String                                        // "admin" | "member"
)
data class CreateDirectChatRequest(val type: String = "direct", val participantId: String)
data class CreateGroupChatRequest(
    val type: String = "group",
    val name: String,
    val participantIds: List<String>
)

// Media
data class MediaUploadResponse(
    val mediaId: String,
    val type: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val url: String,                                        // Presigned download URL
    val thumbnailUrl: String?                               // Presigned thumbnail URL
)

// Notification
data class DeviceTokenRequest(val token: String, val platform: String = "android")
```

### 7.3 WebSocket Event Models

```kotlin
// Generic frame wrapper — all WS communication uses this envelope
@Serializable
data class WsFrame(
    val event: String,
    val data: JsonObject,
    val reqId: String? = null,
    val timestamp: String? = null
)

// Server → Client events (deserialized from WsFrame.data based on event name)
@Serializable
sealed class ServerWsEvent {
    @Serializable @SerialName("message.new")
    data class MessageNew(
        val messageId: String,
        val chatId: String,
        val senderId: String,
        val type: String,
        val payload: MessagePayloadDto,
        val timestamp: String
    ) : ServerWsEvent()

    @Serializable @SerialName("message.sent")
    data class MessageSent(
        val messageId: String,
        val clientMsgId: String,
        val timestamp: String
    ) : ServerWsEvent()

    @Serializable @SerialName("message.status")
    data class MessageStatus(
        val messageId: String,
        val userId: String,
        val status: String,               // "delivered" | "read"
        val timestamp: String
    ) : ServerWsEvent()

    @Serializable @SerialName("message.deleted")
    data class MessageDeleted(
        val messageId: String,
        val chatId: String
    ) : ServerWsEvent()

    @Serializable @SerialName("typing")
    data class Typing(
        val chatId: String,
        val userId: String,
        val isTyping: Boolean
    ) : ServerWsEvent()

    @Serializable @SerialName("presence")
    data class Presence(
        val userId: String,
        val online: Boolean,
        val lastSeen: String?
    ) : ServerWsEvent()

    @Serializable @SerialName("chat.created")
    data class ChatCreated(
        val chatId: String,
        val type: String,
        val name: String?,
        val participants: List<ChatParticipantDto>
    ) : ServerWsEvent()

    @Serializable @SerialName("chat.updated")
    data class ChatUpdated(
        val chatId: String,
        val changes: JsonObject
    ) : ServerWsEvent()

    @Serializable @SerialName("group.member.added")
    data class GroupMemberAdded(
        val chatId: String,
        val userId: String,
        val displayName: String
    ) : ServerWsEvent()

    @Serializable @SerialName("group.member.removed")
    data class GroupMemberRemoved(
        val chatId: String,
        val userId: String
    ) : ServerWsEvent()

    @Serializable @SerialName("pong")
    data object Pong : ServerWsEvent()

    @Serializable @SerialName("error")
    data class Error(val code: Int, val message: String) : ServerWsEvent()
}

// Client → Server events (serialized into WsFrame)
@Serializable
sealed class ClientWsEvent {
    @Serializable @SerialName("message.send")
    data class MessageSend(
        val chatId: String,
        val clientMsgId: String,
        val type: String,
        val payload: MessagePayloadDto
    ) : ClientWsEvent()

    @Serializable @SerialName("message.delivered")
    data class MessageDelivered(val messageIds: List<String>) : ClientWsEvent()

    @Serializable @SerialName("message.read")
    data class MessageRead(val chatId: String, val upToMessageId: String) : ClientWsEvent()

    @Serializable @SerialName("typing.start")
    data class TypingStart(val chatId: String) : ClientWsEvent()

    @Serializable @SerialName("typing.stop")
    data class TypingStop(val chatId: String) : ClientWsEvent()

    @Serializable @SerialName("presence.subscribe")
    data class PresenceSubscribe(val userIds: List<String>) : ClientWsEvent()

    @Serializable @SerialName("ping")
    data object Ping : ClientWsEvent()
}
```

---

## 8. API Contract Summary

All REST endpoints are prefixed with `/api/v1/` and routed through the API gateway.

### 8.1 Authentication

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/auth/otp/send` | Send OTP to phone number | No |
| POST | `/auth/otp/verify` | Verify OTP and get tokens | No |
| POST | `/auth/token/refresh` | Refresh access token | No (uses refresh token) |
| POST | `/auth/logout` | Invalidate tokens + unregister FCM | Yes |

### 8.2 User

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/users/me` | Get current user profile | Yes |
| PATCH | `/users/me` | Update profile (name, about, avatar_url) | Yes |
| GET | `/users/{userId}` | Get user profile by ID | Yes |
| GET | `/users/{userId}/presence` | Get user online status and last seen | Yes |
| POST | `/users/contacts/sync` | Sync phone contacts, get registered users | Yes |
| POST | `/users/{userId}/block` | Block a user | Yes |
| DELETE | `/users/{userId}/block` | Unblock a user | Yes |

> **Note**: Privacy settings (last seen visibility, read receipts) are managed client-side in DataStore and enforced by selectively subscribing to presence events. FCM device tokens are registered via the notification service (see 8.6).

### 8.3 Chats & Groups

The backend uses a unified `/chats` resource for both 1-to-1 (direct) and group conversations.

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/chats` | List all chats with last message preview and unread count (cursor-paginated) | Yes |
| POST | `/chats` | Create a direct or group chat | Yes |
| GET | `/chats/{chatId}` | Get chat details (metadata + participants) | Yes |
| PATCH | `/chats/{chatId}` | Update group chat metadata (name, description, avatar). Admin-only | Yes |
| PATCH | `/chats/{chatId}/mute` | Mute/unmute a chat | Yes |
| POST | `/chats/{chatId}/participants` | Add members to a group. Admin-only | Yes (admin) |
| DELETE | `/chats/{chatId}/participants/{userId}` | Remove a member or self-leave | Yes (admin or self) |
| PATCH | `/chats/{chatId}/participants/{userId}/role` | Promote/demote member. Admin-only | Yes (admin) |

**Create Chat Request (direct)**:
```json
{ "type": "direct", "participant_id": "uuid" }
```

**Create Chat Request (group)**:
```json
{ "type": "group", "name": "Family Group", "participant_ids": ["uuid1", "uuid2"] }
```

### 8.4 Messages

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/messages/{chatId}` | Get paginated message history (cursor-based, newest-first) | Yes |
| POST | `/messages/{chatId}` | Send a message (REST fallback; primary path is WebSocket) | Yes |
| DELETE | `/messages/{messageId}` | Soft-delete a message | Yes |
| POST | `/messages/{messageId}/star` | Star a message | Yes |
| DELETE | `/messages/{messageId}/star` | Unstar a message | Yes |
| POST | `/messages/{chatId}/read` | Mark messages as read up to a given message ID | Yes |

### 8.5 Media

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/media/upload` | Upload media file (multipart/form-data) | Yes |
| GET | `/media/{mediaId}` | Get media metadata and presigned download URL | Yes |
| GET | `/media/{mediaId}/download` | Redirect to presigned MinIO download URL | Yes |

### 8.6 Notifications

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/notifications/device` | Register or update FCM device token | Yes |
| DELETE | `/notifications/device` | Unregister device token (on logout) | Yes |

### 8.7 Common Response Envelope

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

For paginated endpoints, `data` includes a `next_cursor` field:

```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "next_cursor": "cursor-value",
    "has_more": true
  },
  "meta": { ... }
}
```

### 8.8 Error Response

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_OTP",
    "message": "The OTP you entered is invalid or expired.",
    "details": [ ... ]
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "ISO8601"
  }
}
```

### 8.9 Standard Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| `INVALID_REQUEST` | 400 | Malformed request body |
| `UNAUTHORIZED` | 401 | Missing or invalid token |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `CONFLICT` | 409 | Duplicate resource |
| `RATE_LIMITED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Server error |

---

## 9. WebSocket Protocol (Client Side)

### 9.1 Connection Lifecycle

```
App Launch
    |
    v
[Check Auth Token] --invalid--> Login Flow
    |
    valid
    |
    v
[Connect WebSocket]
    wss://<host>/ws?token=<jwt>
    |
    v
[Connected] <-------> [Server]
    |                      |
    |  <--- new_message    |
    |  <--- typing         |
    |  <--- presence       |
    |  ---> send_message   |
    |  ---> mark_read      |
    |  ---> ping (25s)---> |
    |  <--- pong           |
    |                      |
    v
[Disconnected]
    |
    v
[Exponential Backoff Reconnect]
    1s -> 2s -> 4s -> 8s -> 16s -> 30s (max)
    |
    v
[Reconnected]
    |
    v
[Sync missed messages via REST]
    GET /chats/{chatId}/messages?after={lastMessageTimestamp}
```

### 9.2 Message Frame Format

All WebSocket frames are JSON text frames.

**Client → Server**:

```json
{
  "event": "message.send",
  "data": {
    "chat_id": "chat-uuid",
    "client_msg_id": "client-generated-uuid",
    "type": "text",
    "payload": { "body": "Hello!" }
  },
  "req_id": "req-uuid"
}
```

**Server → Client** (ack for the sent message):

```json
{
  "event": "message.sent",
  "data": {
    "message_id": "server-assigned-uuid",
    "client_msg_id": "client-generated-uuid",
    "timestamp": "2026-02-16T10:00:00.123Z"
  },
  "req_id": "req-uuid",
  "timestamp": "2026-02-16T10:00:00.123Z"
}
```

**Server → Client** (new incoming message):

```json
{
  "event": "message.new",
  "data": {
    "message_id": "uuid",
    "chat_id": "chat-uuid",
    "sender_id": "sender-uuid",
    "type": "text",
    "payload": { "body": "Hey there!" },
    "timestamp": "2026-02-16T10:00:01.456Z"
  },
  "timestamp": "2026-02-16T10:00:01.456Z"
}
```

---

## 10. Offline Support & Local Caching Strategy

### 10.1 Offline-First Architecture

The app follows an **offline-first** approach using Room DB as the single source of truth for the UI layer.

```
[Network Available]
    |
    API/WebSocket --> Repository --> Room DB --> Flow --> UI
    
[Network Unavailable]
    |
    Room DB (cached data) --> Flow --> UI
    |
    Outgoing messages --> Pending Queue (Room DB, status="pending")
    
[Network Restored]
    |
    Pending Queue --> Send via WebSocket/REST --> Update status in Room DB
    |
    Sync missed data --> GET /chats?updated_after=<last_sync>
```

### 10.2 Caching Strategy by Data Type

| Data Type | Cache Strategy | TTL | Sync Trigger |
|---|---|---|---|
| User Profiles | Cache in Room, refresh on view | 1 hour | Profile screen open |
| Chat List | Always cached, real-time updates via WS | -- | WebSocket events |
| Messages | Paginated cache, append-only | -- | Chat opened, WS events |
| Contacts | Full cache, periodic sync | 24 hours | Pull-to-refresh, app launch |
| Media Thumbnails | Disk cache (Coil) | 7 days | Lazy load on scroll |
| Media Files | Download on demand, LRU disk cache | 30 days | User taps to view |
| Group Info | Cache in Room | 1 hour | Group screen open |

### 10.3 Pending Message Queue

- When sending a message without network:
  1. Save message to Room DB with `status = "pending"`.
  2. Show in chat UI immediately (optimistic UI).
  3. Enqueue in a `PendingMessageWorker` (WorkManager).
  4. On network restore, WorkManager sends all pending messages sequentially.
  5. On server acknowledgement, update status to "sent".
  6. If send fails after 3 retries, mark as "failed" and show retry button.

### 10.4 Conflict Resolution

- Messages are identified by client-generated UUIDs (idempotency key).
- Server deduplicates by `messageId` -- sending the same message twice is safe.
- For profile/group updates, server timestamp wins (last-write-wins).

---

## 11. Push Notifications

### 11.1 FCM Integration

- App registers FCM token on login and on every token refresh.
- Token sent to backend via `POST /api/v1/notifications/device`.
- Backend sends push notifications via FCM HTTP v1 API when user is offline (no active WebSocket).

### 11.2 Notification Payload

```json
{
  "message": {
    "token": "<fcm_device_token>",
    "data": {
      "type": "new_message",
      "chatId": "chat-uuid",
      "chatType": "direct",
      "senderName": "Alice",
      "senderAvatar": "https://...",
      "messagePreview": "Hey, are you free tonight?",
      "messageType": "text",
      "timestamp": "1708099200000"
    },
    "android": {
      "priority": "high"
    }
  }
}
```

### 11.3 Client-Side Notification Handling

- **Foreground**: If chat is open for the same `chatId`, suppress notification (message already visible via WebSocket). Otherwise, show in-app banner.
- **Background**: Show system notification with sender name, message preview, avatar.
- **Tap action**: Deep-link to the specific chat screen.
- **Notification grouping**: Group notifications by chat (Android notification group/summary).
- **Muted chats**: Suppress notification sound/vibration but still show in notification shade.

### 11.4 Notification Channels (Android 8+)

| Channel ID | Name | Importance | Sound | Vibration |
|---|---|---|---|---|
| `messages` | Messages | High | Default | Yes |
| `groups` | Group Messages | High | Default | Yes |
| `calls` | Calls (future) | Max | Ringtone | Yes |
| `general` | General | Default | None | No |

---

## 12. Non-Functional Requirements

### 12.1 Performance

| Metric | Target |
|---|---|
| Cold start time | < 2 seconds |
| Chat list render (1000 items) | 60 fps scrolling |
| Message send latency (network available) | < 500ms to "sent" status |
| Image load (thumbnail) | < 200ms from cache, < 1s from network |
| WebSocket reconnect | < 5 seconds (95th percentile) |
| APK size | < 30 MB |
| Room DB query (messages page) | < 50ms |

### 12.2 Battery & Resource Efficiency

- WebSocket keep-alive uses minimal battery (OkHttp manages TCP efficiently).
- Background sync limited to WorkManager constraints (network available, not low battery).
- Image compression before upload reduces bandwidth.
- Coil disk cache prevents redundant downloads.
- No polling -- all real-time updates via WebSocket push.

### 12.3 Network Resilience

- Graceful degradation: app is fully usable from cache when offline.
- Message queue survives app restarts (persisted in Room DB).
- Exponential backoff on WebSocket reconnect prevents server overload.
- REST fallback for message sending if WebSocket is unavailable.
- Configurable server URL (ngrok URL changes on restart).

### 12.4 Security

- Tokens stored in `EncryptedSharedPreferences` (AES-256).
- All network traffic over HTTPS (TLS via ngrok).
- JWT access tokens short-lived (15 minutes); refresh tokens long-lived (30 days).
- No sensitive data in logs (ProGuard/R8 strips log statements in release builds).
- Room DB not encrypted by default (can add SQLCipher for phase 2).
- Certificate pinning not required for ngrok (dynamic URLs); rely on TLS.

### 12.5 Accessibility

- All images have content descriptions.
- Minimum touch target size: 48dp.
- Support for TalkBack screen reader.
- Sufficient color contrast ratios (WCAG AA).
- Scalable text (respect system font size settings).

### 12.6 Testing Strategy

| Test Type | Tool | Coverage Target |
|---|---|---|
| Unit Tests | JUnit 5 + MockK | ViewModels, Use Cases, Repositories: 80%+ |
| UI Tests | Compose Testing | Critical flows: login, send message, create group |
| Integration Tests | Hilt Test + Room in-memory | Data layer: 70%+ |
| E2E Tests | Maestro (or Espresso) | Happy paths: login -> send message -> receive |
| Screenshot Tests | Paparazzi | Key screens for regression |

---

## 13. Appendix

### 13.1 Glossary

| Term | Definition |
|---|---|
| Chat | A conversation thread between two users (direct) or a group |
| Direct Chat | A private 1-to-1 conversation between exactly two users |
| Message | A single unit of communication (text, media, system event) |
| Client Message ID | A UUID generated by the client for idempotent message sends |
| Presence | Online/offline status and last seen timestamp |
| OTP | One-Time Password sent via SMS for authentication |
| FCM | Firebase Cloud Messaging -- Google's push notification service |
| JWT | JSON Web Token -- used for stateless authentication |
| Presigned URL | A time-limited URL granting temporary access to a MinIO-stored file |
| Deep Link | A URL that navigates directly to a specific screen in the app |
| E.164 | International phone number format (e.g., `+1234567890`) |

### 13.2 Phased Delivery Roadmap

| Phase | Features | Timeline |
|---|---|---|
| **Phase 1 (MVP)** | Auth, 1-to-1 text messaging, chat list, WebSocket, basic profile | Week 1-3 |
| **Phase 2** | Group messaging, media sharing (images), message status, push notifications | Week 4-6 |
| **Phase 3** | Voice notes, typing indicators, presence, contact sync, search | Week 7-8 |
| **Phase 4** | Quoted replies, message deletion, group admin controls, settings | Week 9-10 |
| **Phase 5 (Stretch)** | E2EE, voice/video calls, dark mode, stories | Week 11+ |

### 13.3 Runtime Configuration

The ngrok URL changes each time the tunnel restarts (unless using a paid plan with reserved domains). The app must support changing the backend URL without reinstalling:

- **Settings screen** includes a "Server URL" field (debug builds only).
- URL stored in DataStore; Retrofit base URL reads from this dynamically.
- Default URL set via `BuildConfig.BASE_URL` at compile time.
- QR code scanner option: scan a QR code generated by the backend setup script to auto-configure the URL.

### 13.4 Build Variants

| Variant | Base URL | Logging | Debuggable |
|---|---|---|---|
| `debug` | `http://10.0.2.2:8080/api/v1/` (emulator) or configurable | Verbose | Yes |
| `staging` | ngrok URL (configurable) | Info | Yes |
| `release` | Production URL | None | No |

### 13.5 Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />    <!-- API 33+ -->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />     <!-- API 33+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />   <!-- API 33+ -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.VIBRATE" />
```
