package com.whatsappclone.feature.chat.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PendingMessageWorker_AssistedFactory_Impl implements PendingMessageWorker_AssistedFactory {
  private final PendingMessageWorker_Factory delegateFactory;

  PendingMessageWorker_AssistedFactory_Impl(PendingMessageWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public PendingMessageWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<PendingMessageWorker_AssistedFactory> create(
      PendingMessageWorker_Factory delegateFactory) {
    return InstanceFactory.create(new PendingMessageWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<PendingMessageWorker_AssistedFactory> createFactoryProvider(
      PendingMessageWorker_Factory delegateFactory) {
    return InstanceFactory.create(new PendingMessageWorker_AssistedFactory_Impl(delegateFactory));
  }
}
