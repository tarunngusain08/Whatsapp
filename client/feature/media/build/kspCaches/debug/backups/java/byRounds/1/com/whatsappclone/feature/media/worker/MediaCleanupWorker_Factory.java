package com.whatsappclone.feature.media.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.whatsappclone.core.database.dao.MediaDao;
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
public final class MediaCleanupWorker_Factory {
  private final Provider<MediaDao> mediaDaoProvider;

  public MediaCleanupWorker_Factory(Provider<MediaDao> mediaDaoProvider) {
    this.mediaDaoProvider = mediaDaoProvider;
  }

  public MediaCleanupWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, mediaDaoProvider.get());
  }

  public static MediaCleanupWorker_Factory create(Provider<MediaDao> mediaDaoProvider) {
    return new MediaCleanupWorker_Factory(mediaDaoProvider);
  }

  public static MediaCleanupWorker newInstance(Context context, WorkerParameters workerParams,
      MediaDao mediaDao) {
    return new MediaCleanupWorker(context, workerParams, mediaDao);
  }
}
