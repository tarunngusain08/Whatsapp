package com.whatsappclone.app.data.websocket;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.core.database.dao.ChatParticipantDao;
import com.whatsappclone.core.database.dao.MessageDao;
import com.whatsappclone.core.network.api.ChatApi;
import com.whatsappclone.core.network.api.MessageApi;
import com.whatsappclone.core.network.websocket.WebSocketManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class SyncOnReconnectManager_Factory implements Factory<SyncOnReconnectManager> {
  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<ChatApi> chatApiProvider;

  private final Provider<MessageApi> messageApiProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<ChatParticipantDao> chatParticipantDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public SyncOnReconnectManager_Factory(Provider<WebSocketManager> webSocketManagerProvider,
      Provider<ChatApi> chatApiProvider, Provider<MessageApi> messageApiProvider,
      Provider<ChatDao> chatDaoProvider, Provider<ChatParticipantDao> chatParticipantDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<DataStore<Preferences>> dataStoreProvider) {
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.chatApiProvider = chatApiProvider;
    this.messageApiProvider = messageApiProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.chatParticipantDaoProvider = chatParticipantDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public SyncOnReconnectManager get() {
    return newInstance(webSocketManagerProvider.get(), chatApiProvider.get(), messageApiProvider.get(), chatDaoProvider.get(), chatParticipantDaoProvider.get(), messageDaoProvider.get(), dataStoreProvider.get());
  }

  public static SyncOnReconnectManager_Factory create(
      Provider<WebSocketManager> webSocketManagerProvider, Provider<ChatApi> chatApiProvider,
      Provider<MessageApi> messageApiProvider, Provider<ChatDao> chatDaoProvider,
      Provider<ChatParticipantDao> chatParticipantDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<DataStore<Preferences>> dataStoreProvider) {
    return new SyncOnReconnectManager_Factory(webSocketManagerProvider, chatApiProvider, messageApiProvider, chatDaoProvider, chatParticipantDaoProvider, messageDaoProvider, dataStoreProvider);
  }

  public static SyncOnReconnectManager newInstance(WebSocketManager webSocketManager,
      ChatApi chatApi, MessageApi messageApi, ChatDao chatDao,
      ChatParticipantDao chatParticipantDao, MessageDao messageDao,
      DataStore<Preferences> dataStore) {
    return new SyncOnReconnectManager(webSocketManager, chatApi, messageApi, chatDao, chatParticipantDao, messageDao, dataStore);
  }
}
