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
public final class GetChatsUseCase_Factory implements Factory<GetChatsUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public GetChatsUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public GetChatsUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static GetChatsUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new GetChatsUseCase_Factory(chatRepositoryProvider);
  }

  public static GetChatsUseCase newInstance(ChatRepository chatRepository) {
    return new GetChatsUseCase(chatRepository);
  }
}
