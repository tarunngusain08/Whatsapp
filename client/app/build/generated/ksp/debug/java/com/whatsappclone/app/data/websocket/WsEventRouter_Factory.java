package com.whatsappclone.app.data.websocket;

import android.content.SharedPreferences;
import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.core.database.dao.ChatParticipantDao;
import com.whatsappclone.core.database.dao.MessageDao;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.websocket.TypingStateHolder;
import com.whatsappclone.core.network.websocket.WebSocketManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class WsEventRouter_Factory implements Factory<WsEventRouter> {
  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<ChatParticipantDao> chatParticipantDaoProvider;

  private final Provider<TypingStateHolder> typingStateHolderProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public WsEventRouter_Factory(Provider<WebSocketManager> webSocketManagerProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<UserDao> userDaoProvider, Provider<ChatParticipantDao> chatParticipantDaoProvider,
      Provider<TypingStateHolder> typingStateHolderProvider, Provider<Json> jsonProvider,
      Provider<SharedPreferences> encryptedPrefsProvider) {
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.userDaoProvider = userDaoProvider;
    this.chatParticipantDaoProvider = chatParticipantDaoProvider;
    this.typingStateHolderProvider = typingStateHolderProvider;
    this.jsonProvider = jsonProvider;
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public WsEventRouter get() {
    return newInstance(webSocketManagerProvider.get(), messageDaoProvider.get(), chatDaoProvider.get(), userDaoProvider.get(), chatParticipantDaoProvider.get(), typingStateHolderProvider.get(), jsonProvider.get(), encryptedPrefsProvider.get());
  }

  public static WsEventRouter_Factory create(Provider<WebSocketManager> webSocketManagerProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<UserDao> userDaoProvider, Provider<ChatParticipantDao> chatParticipantDaoProvider,
      Provider<TypingStateHolder> typingStateHolderProvider, Provider<Json> jsonProvider,
      Provider<SharedPreferences> encryptedPrefsProvider) {
    return new WsEventRouter_Factory(webSocketManagerProvider, messageDaoProvider, chatDaoProvider, userDaoProvider, chatParticipantDaoProvider, typingStateHolderProvider, jsonProvider, encryptedPrefsProvider);
  }

  public static WsEventRouter newInstance(WebSocketManager webSocketManager, MessageDao messageDao,
      ChatDao chatDao, UserDao userDao, ChatParticipantDao chatParticipantDao,
      TypingStateHolder typingStateHolder, Json json, SharedPreferences encryptedPrefs) {
    return new WsEventRouter(webSocketManager, messageDao, chatDao, userDao, chatParticipantDao, typingStateHolder, json, encryptedPrefs);
  }
}
