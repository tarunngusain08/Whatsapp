package com.whatsappclone.feature.chat.ui.forward;

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
public final class ForwardPickerViewModel_Factory implements Factory<ForwardPickerViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<MessageRepository> messageRepositoryProvider;

  public ForwardPickerViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<MessageRepository> messageRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.messageRepositoryProvider = messageRepositoryProvider;
  }

  @Override
  public ForwardPickerViewModel get() {
    return newInstance(chatRepositoryProvider.get(), messageRepositoryProvider.get());
  }

  public static ForwardPickerViewModel_Factory create(
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<MessageRepository> messageRepositoryProvider) {
    return new ForwardPickerViewModel_Factory(chatRepositoryProvider, messageRepositoryProvider);
  }

  public static ForwardPickerViewModel newInstance(ChatRepository chatRepository,
      MessageRepository messageRepository) {
    return new ForwardPickerViewModel(chatRepository, messageRepository);
  }
}
