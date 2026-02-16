package com.whatsappclone.feature.media.util;

import android.content.Context;
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
public final class VideoCompressor_Factory implements Factory<VideoCompressor> {
  private final Provider<Context> contextProvider;

  public VideoCompressor_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public VideoCompressor get() {
    return newInstance(contextProvider.get());
  }

  public static VideoCompressor_Factory create(Provider<Context> contextProvider) {
    return new VideoCompressor_Factory(contextProvider);
  }

  public static VideoCompressor newInstance(Context context) {
    return new VideoCompressor(context);
  }
}
