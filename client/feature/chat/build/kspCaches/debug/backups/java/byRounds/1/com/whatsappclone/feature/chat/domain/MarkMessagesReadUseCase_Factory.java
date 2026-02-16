package com.whatsappclone.feature.chat.domain;

import com.whatsappclone.core.database.dao.MessageDao;
import com.whatsappclone.core.network.websocket.WebSocketManager;
import com.whatsappclone.feature.chat.data.ChatRepository;
import com.whatsappclone.feature.chat.data.MessageRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class MarkMessagesReadUseCase_Factory implements Factory<MarkMessagesReadUseCase> {
  private final Provider<MessageRepository> messageRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  public MarkMessagesReadUseCase_Factory(Provider<MessageRepository> messageRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider, Provider<MessageDao> messageDaoProvider,
      Provider<WebSocketManager> webSocketManagerProvider) {
    this.messageRepositoryProvider = messageRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
  }

  @Override
  public MarkMessagesReadUseCase get() {
    return newInstance(messageRepositoryProvider.get(), chatRepositoryProvider.get(), messageDaoProvider.get(), webSocketManagerProvider.get());
  }

  public static MarkMessagesReadUseCase_Factory create(
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider, Provider<MessageDao> messageDaoProvider,
      Provider<WebSocketManager> webSocketManagerProvider) {
    return new MarkMessagesReadUseCase_Factory(messageRepositoryProvider, chatRepositoryProvider, messageDaoProvider, webSocketManagerProvider);
  }

  public static MarkMessagesReadUseCase newInstance(MessageRepository messageRepository,
      ChatRepository chatRepository, MessageDao messageDao, WebSocketManager webSocketManager) {
    return new MarkMessagesReadUseCase(messageRepository, chatRepository, messageDao, webSocketManager);
  }
}
