# Android Client — Architecture & Deep Dive

The WhatsApp Clone Android client is a modern Kotlin application built with Jetpack Compose, following MVVM + Clean Architecture with a multi-module structure. It provides a local-first, offline-capable messaging experience with real-time sync via WebSocket.

---

## Module Structure

```
client/
├── app/                         # Application module (entry point, navigation, DI)
│   ├── WhatsAppApplication.kt  # @HiltAndroidApp
│   ├── MainActivity.kt         # @AndroidEntryPoint, single Activity
│   ├── navigation/             # AppNavGraph, route definitions
│   └── data/websocket/         # WsEventRouter, SyncOnReconnectManager, WsLifecycleManager
│
├── core/
│   ├── common/                 # Result types, constants, extensions, utilities
│   ├── database/               # Room database, entities, DAOs, type converters
│   ├── network/                # Retrofit APIs, WebSocket, token management, interceptors
│   └── ui/                     # Shared composables, theme, colors, typography
│
└── feature/
    ├── auth/                   # Login, OTP verification, profile setup
    ├── chat/                   # Chat list, chat detail, message composer
    ├── contacts/               # Contact picker, contact info
    ├── group/                  # Group creation, group info, participant management
    ├── media/                  # Media viewer (images, videos)
    ├── profile/                # Profile editing
    └── settings/               # App settings, privacy, notifications, server URL
```

---

## Architecture Layers

```
┌──────────────────────────────────────────────────────────────────┐
│                        UI Layer                                   │
│  Jetpack Compose Screens ← observe → ViewModels (StateFlow)      │
└──────────────────────────────┬───────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│                      Domain Layer                                 │
│  Use Cases (e.g., SendMessageUseCase, GetChatsUseCase)           │
└──────────────────────────────┬───────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│                       Data Layer                                  │
│  Repositories → Room DAOs (local) + Retrofit APIs (remote)        │
│              → WebSocketManager (real-time)                        │
└──────────────────────────────────────────────────────────────────┘
```

**Data flows down, events flow up:**
- ViewModels expose `StateFlow<UiState>` and `SharedFlow<UiEvent>`
- Screens collect flows and render UI
- User actions call ViewModel methods, which delegate to use cases/repositories
- Repositories coordinate between local (Room) and remote (API/WebSocket) sources

---

## Navigation

The app uses Jetpack Navigation Compose with a sealed class route system.

### Route Definitions

```kotlin
sealed class AppRoute {
    object Splash : AppRoute()
    object Login : AppRoute()
    object Otp : AppRoute()
    object ProfileSetup : AppRoute()
    object ChatList : AppRoute()         // Main screen
    data class ChatDetail(val chatId: String) : AppRoute()
    object ContactPicker : AppRoute()
    object ContactInfo : AppRoute()
    object NewGroup : AppRoute()
    object GroupSetup : AppRoute()
    data class GroupInfo(val chatId: String) : AppRoute()
    object AddParticipants : AppRoute()
    data class MediaViewer(val mediaId: String) : AppRoute()
    object Settings : AppRoute()
    object ProfileEdit : AppRoute()
    object PrivacySettings : AppRoute()
    object NotificationSettings : AppRoute()
    object ServerUrl : AppRoute()       // Debug only
}
```

### Navigation Flow

```
App Launch
    │
    ▼
SplashScreen
    │
    ├── Has valid token? ──► ChatListScreen (main)
    └── No token? ──► LoginScreen
                          │
                          ▼
                     OtpScreen
                          │
                          ▼
                     ProfileSetupScreen (first-time only)
                          │
                          ▼
                     ChatListScreen
```

### WebSocket Lifecycle Integration

Navigation events control the WebSocket connection:
- **Entering main flow** (after auth): WebSocket connects
- **Session expired**: WebSocket disconnects, navigates to login
- **App backgrounded**: WebSocket is managed by `WsLifecycleManager`

---

## Data Layer: Room Database

### Database Configuration

```kotlin
@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        ChatParticipantEntity::class,
        ContactEntity::class,
        GroupEntity::class,
        MediaEntity::class,
        MessageFts::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun chatParticipantDao(): ChatParticipantDao
    abstract fun contactDao(): ContactDao
    abstract fun groupDao(): GroupDao
    abstract fun mediaDao(): MediaDao
}
```

### Key Entities

**ChatEntity** — represents a conversation:
```kotlin
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val chatType: String,              // "direct" or "group"
    val name: String?,                 // group name or contact name
    val description: String?,
    val avatarUrl: String?,
    val lastMessageId: String?,
    val lastMessagePreview: String?,
    val lastMessageTimestamp: Long?,
    val unreadCount: Int,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

**MessageEntity** — a single message:
```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val clientMsgId: String?,          // client-generated ID for optimistic sends
    val chatId: String,
    val senderId: String,
    val messageType: String,           // text, image, video, audio, document, location
    val content: String?,
    val mediaId: String?,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
    val replyToId: String?,
    val status: String,                // pending, sent, delivered, read
    val isDeleted: Boolean,
    val isStarred: Boolean,
    val timestamp: Long
)
```

### DAO Patterns

DAOs return `Flow<T>` for reactive queries — when Room data changes, the UI automatically updates:

```kotlin
@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun observeChatsWithLastMessage(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    fun observeChat(chatId: String): Flow<ChatEntity?>

    @Upsert
    suspend fun upsertChat(chat: ChatEntity)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE chatId = :chatId")
    suspend fun incrementUnreadCount(chatId: String)
}
```

**Message pagination** uses Room's `PagingSource`:
```kotlin
@Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC")
fun getMessagesPaged(chatId: String): PagingSource<Int, MessageEntity>
```

### Full-Text Search

Messages support FTS via a dedicated FTS entity:
```kotlin
@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = "messages_fts")
data class MessageFts(
    val content: String?
)
```

---

## Network Layer

### Retrofit API Services

Each backend service has a corresponding Retrofit interface:

```kotlin
interface ChatApi {
    @GET("api/v1/chats")
    suspend fun getChats(@Query("page") page: Int): Response<ChatsResponse>

    @POST("api/v1/chats")
    suspend fun createChat(@Body request: CreateChatRequest): Response<ChatResponse>

    @PUT("api/v1/chats/{chatId}/mute")
    suspend fun muteChat(@Path("chatId") chatId: String, @Body request: MuteRequest): Response<Unit>
}

interface MessageApi {
    @GET("api/v1/messages")
    suspend fun getMessages(
        @Query("chat_id") chatId: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<MessagesResponse>

    @POST("api/v1/messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<MessageResponse>

    @POST("api/v1/messages/read")
    suspend fun markRead(@Body request: MarkReadRequest): Response<Unit>
}
```

### Token Management

```
┌─────────────────────────────────────────────────────────────────┐
│                    TokenManager                                  │
│                                                                  │
│  ┌──────────────────────┐    ┌────────────────────────┐         │
│  │ EncryptedSharedPrefs │    │    AuthInterceptor      │         │
│  │                      │    │                          │         │
│  │ accessToken          │◄──►│ Adds Authorization       │         │
│  │ refreshToken         │    │ header to every request  │         │
│  │ userId               │    │                          │         │
│  └──────────────────────┘    │ On 401:                  │         │
│                               │  1. Refresh token       │         │
│                               │  2. Retry request       │         │
│                               │  3. If refresh fails:   │         │
│                               │     → clear tokens      │         │
│                               │     → emit sessionExpiry│         │
│                               └────────────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

The `AuthInterceptor` (OkHttp interceptor):
1. Attaches `Authorization: Bearer <accessToken>` to every request
2. On HTTP 401, attempts a token refresh
3. Retries the original request with the new token
4. If refresh fails, emits a session expiry event that triggers navigation to login

### Base URL Configuration

The server URL is stored in DataStore and can be changed at runtime via a debug settings screen — useful for switching between local development and deployed backends.

---

## Real-Time Layer: WebSocket

### WebSocketManager

The central component managing the WebSocket connection:

```kotlin
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val baseUrlProvider: BaseUrlProvider
) {
    val connectionState: StateFlow<ConnectionState>  // CONNECTING, CONNECTED, DISCONNECTED
    val events: SharedFlow<WsEvent>                   // incoming events

    fun connect()
    fun disconnect()
    fun send(event: WsEvent)
}
```

**Connection state machine:**
```
DISCONNECTED ──connect()──► CONNECTING ──success──► CONNECTED
      ▲                         │                       │
      │                    failure/timeout          disconnect/error
      │                         │                       │
      └─────────────────────────┴───────────────────────┘
                                │
                        Exponential backoff
                        (1s, 2s, 4s, 8s... max 30s)
```

**Heartbeat**: the server pings every 25 seconds; the client responds with pong. If no ping arrives within 60 seconds, the client reconnects.

### WsEventRouter

Lives in the `app` module and bridges WebSocket events to the Room database:

```
WebSocket event arrives
    │
    ├─► message.new
    │   └─► Insert MessageEntity into Room
    │   └─► Update ChatEntity (lastMessage, unreadCount)
    │
    ├─► message.sent (confirmation)
    │   └─► Update MessageEntity: status "pending" → "sent", set server messageId
    │
    ├─► message.status.updated
    │   └─► Update MessageEntity status (delivered/read)
    │
    ├─► message.deleted
    │   └─► Mark MessageEntity as deleted
    │
    ├─► chat.created
    │   └─► Insert ChatEntity into Room
    │
    ├─► chat.updated
    │   └─► Update ChatEntity fields
    │
    ├─► group.member.added / removed
    │   └─► Update ChatParticipantEntity
    │
    ├─► typing
    │   └─► Update TypingStateHolder (in-memory, not persisted)
    │
    └─► presence.updated
        └─► Update UserEntity online status
```

### SyncOnReconnectManager

Handles data synchronization after a reconnection (e.g., phone was offline for hours):

```
WebSocket reconnects
    │
    ▼
1. Sync chats
   └─► GET /api/v1/chats (paginated)
   └─► Upsert all chats into Room
   └─► Update unread counts

2. Flush pending messages
   └─► Query Room: messages WHERE status = "pending"
   └─► Re-send each via WebSocket
   └─► Update status on success

3. Update last sync timestamp
   └─► Store in DataStore for next reconnection window
```

### WsLifecycleManager

Ties the WebSocket lifecycle to the app's process lifecycle:

```kotlin
class WsLifecycleManager @Inject constructor(
    private val wsManager: WebSocketManager,
    private val tokenManager: TokenManager
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        if (tokenManager.hasValidToken()) {
            wsManager.connect()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // Optionally keep alive for a grace period
    }
}
```

---

## Offline-First Strategy

The app follows a local-first approach. The Room database is the source of truth for the UI — network calls update Room, and the UI observes Room.

### Write Path (Sending a Message)

```
1. User taps Send
    │
    ▼
2. ViewModel calls repository.sendMessage(chatId, content)
    │
    ▼
3. Repository:
   a. Generate clientMsgId (UUID)
   b. Insert into Room: MessageEntity(status = "pending")
   c. Update ChatEntity: lastMessage preview
   d. UI shows message immediately (optimistic update)
    │
    ▼
4. Send via WebSocket: { type: "message.send", clientMsgId, chatId, content }
    │
    ├─► Success: server confirms with messageId
    │   └─► Update Room: messageId = server's ID, status = "sent"
    │
    └─► Failure: message stays "pending"
        └─► SyncOnReconnectManager retries on next connection
```

### Read Path (Receiving Messages)

```
1. WebSocket receives message.new event
    │
    ▼
2. WsEventRouter:
   a. Insert MessageEntity into Room
   b. Update ChatEntity (lastMessage, increment unreadCount)
    │
    ▼
3. UI (observing Room Flows):
   a. ChatListScreen sees updated chat with new last message
   b. ChatDetailScreen (if open) sees new message appear
   c. No explicit refresh needed — Room Flow triggers recomposition
```

### Sync Path (Reconnection)

```
1. App was offline for 2 hours
2. WebSocket reconnects
3. SyncOnReconnectManager:
   a. Fetches chats from API → upserts into Room
   b. Retries pending messages → updates status in Room
4. UI automatically reflects synced state
```

---

## Key UI Screens

### ChatListScreen

The main screen showing all conversations:

```
┌────────────────────────────────────────┐
│ WhatsApp Clone               🔍  ⋮    │
├────────────────────────────────────────┤
│ 📌 Alice                      2:30 PM │
│    See you tomorrow!              ✓✓  │
├────────────────────────────────────────┤
│ 📌 Project Group               1:15 PM│
│    Bob: Let's meet at 3          (3)  │
├────────────────────────────────────────┤
│ Charlie                       11:00 AM │
│    Thanks!                        ✓✓  │
├────────────────────────────────────────┤
│                                        │
│                                        │
│                              💬 FAB   │
└────────────────────────────────────────┘
```

**Features**: pinned chats at top, unread badges, last message preview, delivery status ticks, search, pull-to-refresh, FAB to start new chat.

### ChatDetailScreen

The message view with composer:

```
┌────────────────────────────────────────┐
│ ← Alice          Online     📞  ⋮     │
├────────────────────────────────────────┤
│                                        │
│              Hey! How are you?         │
│                          2:28 PM ✓✓   │
│                                        │
│  I'm doing great, thanks!             │
│  2:29 PM                               │
│                                        │
│              See you tomorrow!         │
│                          2:30 PM ✓✓   │
│                                        │
├────────────────────────────────────────┤
│ 📎  Type a message...         🎤  ▶  │
└────────────────────────────────────────┘
```

**Features**: paged message loading (Paging 3), typing indicators, reply preview, message long-press actions (reply, forward, delete, star, react), scroll-to-bottom FAB, read receipt ticks.

---

## Dependency Injection (Hilt)

### Module Organization

```
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideCoroutineScope(): CoroutineScope
    
    @Provides @Singleton
    fun provideDispatchers(): AppDispatchers
}

@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(app: Application): AppDatabase
    
    @Provides fun provideChatDao(db: AppDatabase): ChatDao
    @Provides fun provideMessageDao(db: AppDatabase): MessageDao
    // ... other DAOs
}

@Module @InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient
    
    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, baseUrlProvider: BaseUrlProvider): Retrofit
    
    @Provides @Singleton
    fun provideWebSocketManager(...): WebSocketManager
    
    @Provides fun provideChatApi(retrofit: Retrofit): ChatApi
    // ... other APIs
}
```

### Key Singletons

| Component | Scope | Purpose |
|-----------|-------|---------|
| `AppDatabase` | Singleton | Room database instance |
| `WebSocketManager` | Singleton | WebSocket connection |
| `WsEventRouter` | Singleton | Event-to-Room bridge |
| `SyncOnReconnectManager` | Singleton | Reconnection sync |
| `TokenManager` | Singleton | Token storage/refresh |
| Repositories | Singleton | Data access layer |
| ViewModels | ViewModelComponent | Per-screen state |

---

## Error Handling

The app uses a sealed result type across all layers:

```kotlin
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val code: Int? = null) : AppResult<Nothing>()
    object Loading : AppResult<Nothing>()
}
```

ViewModels map these to UI states:
```kotlin
data class ChatListUiState(
    val chats: List<ChatItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)
```

Network errors, timeouts, and server errors are caught in the repository layer and surfaced as `AppResult.Error`, preventing crashes and allowing graceful UI degradation.

---

## Performance Optimizations

1. **Paging 3**: messages are loaded in pages of 50, fetching more as the user scrolls
2. **Room Flow**: reactive queries avoid manual refresh; only changed rows trigger recomposition
3. **LazyColumn**: Compose's lazy layout for chat list and message list
4. **Image caching**: Coil with disk and memory cache for avatars and media thumbnails
5. **Debounced search**: search input is debounced to avoid excessive queries
6. **Optimistic UI**: messages appear instantly without waiting for server confirmation
7. **Batched Room operations**: bulk upserts during sync to minimize database transactions
