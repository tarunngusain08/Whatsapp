package com.whatsappclone.feature.chat.ui.chatdetail;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import com.whatsappclone.core.database.dao.ChatParticipantDao;
import com.whatsappclone.core.database.dao.MessageDao;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.websocket.TypingStateHolder;
import com.whatsappclone.core.network.websocket.WebSocketManager;
import com.whatsappclone.feature.chat.data.ChatRepository;
import com.whatsappclone.feature.chat.data.MessageRepository;
import com.whatsappclone.feature.chat.data.UserRepository;
import com.whatsappclone.feature.chat.domain.MarkMessagesReadUseCase;
import com.whatsappclone.feature.chat.domain.SendMessageUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ChatDetailViewModel_Factory implements Factory<ChatDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<MessageRepository> messageRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<SendMessageUseCase> sendMessageUseCaseProvider;

  private final Provider<MarkMessagesReadUseCase> markMessagesReadUseCaseProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<TypingStateHolder> typingStateHolderProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<ChatParticipantDao> chatParticipantDaoProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<Context> appContextProvider;

  public ChatDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider,
      Provider<MarkMessagesReadUseCase> markMessagesReadUseCaseProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<TypingStateHolder> typingStateHolderProvider,
      Provider<MessageDao> messageDaoProvider,
      Provider<ChatParticipantDao> chatParticipantDaoProvider, Provider<UserDao> userDaoProvider,
      Provider<Context> appContextProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.messageRepositoryProvider = messageRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.sendMessageUseCaseProvider = sendMessageUseCaseProvider;
    this.markMessagesReadUseCaseProvider = markMessagesReadUseCaseProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.typingStateHolderProvider = typingStateHolderProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.chatParticipantDaoProvider = chatParticipantDaoProvider;
    this.userDaoProvider = userDaoProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public ChatDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), messageRepositoryProvider.get(), chatRepositoryProvider.get(), userRepositoryProvider.get(), sendMessageUseCaseProvider.get(), markMessagesReadUseCaseProvider.get(), webSocketManagerProvider.get(), typingStateHolderProvider.get(), messageDaoProvider.get(), chatParticipantDaoProvider.get(), userDaoProvider.get(), appContextProvider.get());
  }

  public static ChatDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider,
      Provider<MarkMessagesReadUseCase> markMessagesReadUseCaseProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<TypingStateHolder> typingStateHolderProvider,
      Provider<MessageDao> messageDaoProvider,
      Provider<ChatParticipantDao> chatParticipantDaoProvider, Provider<UserDao> userDaoProvider,
      Provider<Context> appContextProvider) {
    return new ChatDetailViewModel_Factory(savedStateHandleProvider, messageRepositoryProvider, chatRepositoryProvider, userRepositoryProvider, sendMessageUseCaseProvider, markMessagesReadUseCaseProvider, webSocketManagerProvider, typingStateHolderProvider, messageDaoProvider, chatParticipantDaoProvider, userDaoProvider, appContextProvider);
  }

  public static ChatDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      MessageRepository messageRepository, ChatRepository chatRepository,
      UserRepository userRepository, SendMessageUseCase sendMessageUseCase,
      MarkMessagesReadUseCase markMessagesReadUseCase, WebSocketManager webSocketManager,
      TypingStateHolder typingStateHolder, MessageDao messageDao,
      ChatParticipantDao chatParticipantDao, UserDao userDao, Context appContext) {
    return new ChatDetailViewModel(savedStateHandle, messageRepository, chatRepository, userRepository, sendMessageUseCase, markMessagesReadUseCase, webSocketManager, typingStateHolder, messageDao, chatParticipantDao, userDao, appContext);
  }
}
