package com.whatsappclone.feature.chat.domain;

import com.whatsappclone.feature.chat.data.ChatRepository;
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
public final class CreateDirectChatUseCase_Factory implements Factory<CreateDirectChatUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public CreateDirectChatUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public CreateDirectChatUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static CreateDirectChatUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new CreateDirectChatUseCase_Factory(chatRepositoryProvider);
  }

  public static CreateDirectChatUseCase newInstance(ChatRepository chatRepository) {
    return new CreateDirectChatUseCase(chatRepository);
  }
}
