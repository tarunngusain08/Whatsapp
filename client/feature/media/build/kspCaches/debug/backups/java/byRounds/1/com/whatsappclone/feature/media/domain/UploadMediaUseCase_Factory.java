package com.whatsappclone.feature.media.domain;

import android.content.Context;
import com.whatsappclone.feature.media.data.MediaRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class UploadMediaUseCase_Factory implements Factory<UploadMediaUseCase> {
  private final Provider<Context> contextProvider;

  private final Provider<MediaRepository> mediaRepositoryProvider;

  public UploadMediaUseCase_Factory(Provider<Context> contextProvider,
      Provider<MediaRepository> mediaRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.mediaRepositoryProvider = mediaRepositoryProvider;
  }

  @Override
  public UploadMediaUseCase get() {
    return newInstance(contextProvider.get(), mediaRepositoryProvider.get());
  }

  public static UploadMediaUseCase_Factory create(Provider<Context> contextProvider,
      Provider<MediaRepository> mediaRepositoryProvider) {
    return new UploadMediaUseCase_Factory(contextProvider, mediaRepositoryProvider);
  }

  public static UploadMediaUseCase newInstance(Context context, MediaRepository mediaRepository) {
    return new UploadMediaUseCase(context, mediaRepository);
  }
}
