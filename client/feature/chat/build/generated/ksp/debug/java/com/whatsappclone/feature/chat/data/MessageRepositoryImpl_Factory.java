package com.whatsappclone.feature.chat.data;

import android.content.SharedPreferences;
import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.core.database.dao.MessageDao;
import com.whatsappclone.core.network.api.MessageApi;
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
public final class MessageRepositoryImpl_Factory implements Factory<MessageRepositoryImpl> {
  private final Provider<MessageApi> messageApiProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public MessageRepositoryImpl_Factory(Provider<MessageApi> messageApiProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<WebSocketManager> webSocketManagerProvider, Provider<Json> jsonProvider,
      Provider<SharedPreferences> encryptedPrefsProvider) {
    this.messageApiProvider = messageApiProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.jsonProvider = jsonProvider;
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public MessageRepositoryImpl get() {
    return newInstance(messageApiProvider.get(), messageDaoProvider.get(), chatDaoProvider.get(), webSocketManagerProvider.get(), jsonProvider.get(), encryptedPrefsProvider.get());
  }

  public static MessageRepositoryImpl_Factory create(Provider<MessageApi> messageApiProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<WebSocketManager> webSocketManagerProvider, Provider<Json> jsonProvider,
      Provider<SharedPreferences> encryptedPrefsProvider) {
    return new MessageRepositoryImpl_Factory(messageApiProvider, messageDaoProvider, chatDaoProvider, webSocketManagerProvider, jsonProvider, encryptedPrefsProvider);
  }

  public static MessageRepositoryImpl newInstance(MessageApi messageApi, MessageDao messageDao,
      ChatDao chatDao, WebSocketManager webSocketManager, Json json,
      SharedPreferences encryptedPrefs) {
    return new MessageRepositoryImpl(messageApi, messageDao, chatDao, webSocketManager, json, encryptedPrefs);
  }
}
