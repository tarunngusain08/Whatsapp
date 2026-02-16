package com.whatsappclone.feature.group.di;

import com.whatsappclone.feature.chat.data.ChatRepository;
import com.whatsappclone.feature.group.domain.CreateGroupUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class GroupModule_ProvideCreateGroupUseCaseFactory implements Factory<CreateGroupUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public GroupModule_ProvideCreateGroupUseCaseFactory(
      Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public CreateGroupUseCase get() {
    return provideCreateGroupUseCase(chatRepositoryProvider.get());
  }

  public static GroupModule_ProvideCreateGroupUseCaseFactory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new GroupModule_ProvideCreateGroupUseCaseFactory(chatRepositoryProvider);
  }

  public static CreateGroupUseCase provideCreateGroupUseCase(ChatRepository chatRepository) {
    return Preconditions.checkNotNullFromProvides(GroupModule.INSTANCE.provideCreateGroupUseCase(chatRepository));
  }
}
