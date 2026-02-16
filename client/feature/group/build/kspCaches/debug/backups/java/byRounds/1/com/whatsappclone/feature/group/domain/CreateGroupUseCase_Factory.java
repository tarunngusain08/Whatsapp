package com.whatsappclone.feature.group.domain;

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
public final class CreateGroupUseCase_Factory implements Factory<CreateGroupUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public CreateGroupUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public CreateGroupUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static CreateGroupUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new CreateGroupUseCase_Factory(chatRepositoryProvider);
  }

  public static CreateGroupUseCase newInstance(ChatRepository chatRepository) {
    return new CreateGroupUseCase(chatRepository);
  }
}
