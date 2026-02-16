package com.whatsappclone.feature.media.util;

import android.content.Context;
import com.whatsappclone.core.database.dao.MediaDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class MediaDownloadManager_Factory implements Factory<MediaDownloadManager> {
  private final Provider<Context> contextProvider;

  private final Provider<MediaDao> mediaDaoProvider;

  public MediaDownloadManager_Factory(Provider<Context> contextProvider,
      Provider<MediaDao> mediaDaoProvider) {
    this.contextProvider = contextProvider;
    this.mediaDaoProvider = mediaDaoProvider;
  }

  @Override
  public MediaDownloadManager get() {
    return newInstance(contextProvider.get(), mediaDaoProvider.get());
  }

  public static MediaDownloadManager_Factory create(Provider<Context> contextProvider,
      Provider<MediaDao> mediaDaoProvider) {
    return new MediaDownloadManager_Factory(contextProvider, mediaDaoProvider);
  }

  public static MediaDownloadManager newInstance(Context context, MediaDao mediaDao) {
    return new MediaDownloadManager(context, mediaDao);
  }
}
