package com.whatsappclone.feature.chat.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.whatsappclone.feature.chat.data.MessageRepository;
import dagger.internal.DaggerGenerated;
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
public final class PendingMessageWorker_Factory {
  private final Provider<MessageRepository> messageRepositoryProvider;

  public PendingMessageWorker_Factory(Provider<MessageRepository> messageRepositoryProvider) {
    this.messageRepositoryProvider = messageRepositoryProvider;
  }

  public PendingMessageWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, messageRepositoryProvider.get());
  }

  public static PendingMessageWorker_Factory create(
      Provider<MessageRepository> messageRepositoryProvider) {
    return new PendingMessageWorker_Factory(messageRepositoryProvider);
  }

  public static PendingMessageWorker newInstance(Context context, WorkerParameters workerParams,
      MessageRepository messageRepository) {
    return new PendingMessageWorker(context, workerParams, messageRepository);
  }
}
