package com.whatsappclone.feature.chat.ui.chatlist;

import com.whatsappclone.core.network.websocket.TypingStateHolder;
import com.whatsappclone.feature.chat.data.ChatRepository;
import com.whatsappclone.feature.chat.domain.GetChatsUseCase;
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
public final class ChatListViewModel_Factory implements Factory<ChatListViewModel> {
  private final Provider<GetChatsUseCase> getChatsUseCaseProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<TypingStateHolder> typingStateHolderProvider;

  public ChatListViewModel_Factory(Provider<GetChatsUseCase> getChatsUseCaseProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<TypingStateHolder> typingStateHolderProvider) {
    this.getChatsUseCaseProvider = getChatsUseCaseProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.typingStateHolderProvider = typingStateHolderProvider;
  }

  @Override
  public ChatListViewModel get() {
    return newInstance(getChatsUseCaseProvider.get(), chatRepositoryProvider.get(), typingStateHolderProvider.get());
  }

  public static ChatListViewModel_Factory create(Provider<GetChatsUseCase> getChatsUseCaseProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<TypingStateHolder> typingStateHolderProvider) {
    return new ChatListViewModel_Factory(getChatsUseCaseProvider, chatRepositoryProvider, typingStateHolderProvider);
  }

  public static ChatListViewModel newInstance(GetChatsUseCase getChatsUseCase,
      ChatRepository chatRepository, TypingStateHolder typingStateHolder) {
    return new ChatListViewModel(getChatsUseCase, chatRepository, typingStateHolder);
  }
}
