package com.whatsappclone.feature.media.data;

import android.content.Context;
import com.whatsappclone.core.database.dao.MediaDao;
import com.whatsappclone.core.network.api.MediaApi;
import com.whatsappclone.feature.media.util.ImageCompressor;
import com.whatsappclone.feature.media.util.VideoCompressor;
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
public final class MediaRepositoryImpl_Factory implements Factory<MediaRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<MediaApi> mediaApiProvider;

  private final Provider<MediaDao> mediaDaoProvider;

  private final Provider<ImageCompressor> imageCompressorProvider;

  private final Provider<VideoCompressor> videoCompressorProvider;

  public MediaRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<MediaApi> mediaApiProvider, Provider<MediaDao> mediaDaoProvider,
      Provider<ImageCompressor> imageCompressorProvider,
      Provider<VideoCompressor> videoCompressorProvider) {
    this.contextProvider = contextProvider;
    this.mediaApiProvider = mediaApiProvider;
    this.mediaDaoProvider = mediaDaoProvider;
    this.imageCompressorProvider = imageCompressorProvider;
    this.videoCompressorProvider = videoCompressorProvider;
  }

  @Override
  public MediaRepositoryImpl get() {
    return newInstance(contextProvider.get(), mediaApiProvider.get(), mediaDaoProvider.get(), imageCompressorProvider.get(), videoCompressorProvider.get());
  }

  public static MediaRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<MediaApi> mediaApiProvider, Provider<MediaDao> mediaDaoProvider,
      Provider<ImageCompressor> imageCompressorProvider,
      Provider<VideoCompressor> videoCompressorProvider) {
    return new MediaRepositoryImpl_Factory(contextProvider, mediaApiProvider, mediaDaoProvider, imageCompressorProvider, videoCompressorProvider);
  }

  public static MediaRepositoryImpl newInstance(Context context, MediaApi mediaApi,
      MediaDao mediaDao, ImageCompressor imageCompressor, VideoCompressor videoCompressor) {
    return new MediaRepositoryImpl(context, mediaApi, mediaDao, imageCompressor, videoCompressor);
  }
}
