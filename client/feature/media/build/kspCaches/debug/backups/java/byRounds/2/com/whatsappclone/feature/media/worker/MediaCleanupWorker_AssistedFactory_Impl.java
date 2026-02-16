package com.whatsappclone.feature.media.worker;

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
public final class MediaCleanupWorker_AssistedFactory_Impl implements MediaCleanupWorker_AssistedFactory {
  private final MediaCleanupWorker_Factory delegateFactory;

  MediaCleanupWorker_AssistedFactory_Impl(MediaCleanupWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public MediaCleanupWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<MediaCleanupWorker_AssistedFactory> create(
      MediaCleanupWorker_Factory delegateFactory) {
    return InstanceFactory.create(new MediaCleanupWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<MediaCleanupWorker_AssistedFactory> createFactoryProvider(
      MediaCleanupWorker_Factory delegateFactory) {
    return InstanceFactory.create(new MediaCleanupWorker_AssistedFactory_Impl(delegateFactory));
  }
}
