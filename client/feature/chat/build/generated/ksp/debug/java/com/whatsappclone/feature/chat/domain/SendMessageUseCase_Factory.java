package com.whatsappclone.feature.chat.domain;

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
public final class SendMessageUseCase_Factory implements Factory<SendMessageUseCase> {
  private final Provider<MessageRepository> messageRepositoryProvider;

  public SendMessageUseCase_Factory(Provider<MessageRepository> messageRepositoryProvider) {
    this.messageRepositoryProvider = messageRepositoryProvider;
  }

  @Override
  public SendMessageUseCase get() {
    return newInstance(messageRepositoryProvider.get());
  }

  public static SendMessageUseCase_Factory create(
      Provider<MessageRepository> messageRepositoryProvider) {
    return new SendMessageUseCase_Factory(messageRepositoryProvider);
  }

  public static SendMessageUseCase newInstance(MessageRepository messageRepository) {
    return new SendMessageUseCase(messageRepository);
  }
}
