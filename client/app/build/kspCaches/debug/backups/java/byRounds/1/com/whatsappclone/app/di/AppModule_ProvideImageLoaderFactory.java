package com.whatsappclone.app.di;

import android.content.Context;
import coil3.ImageLoader;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class AppModule_ProvideImageLoaderFactory implements Factory<ImageLoader> {
  private final Provider<Context> contextProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  public AppModule_ProvideImageLoaderFactory(Provider<Context> contextProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    this.contextProvider = contextProvider;
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public ImageLoader get() {
    return provideImageLoader(contextProvider.get(), okHttpClientProvider.get());
  }

  public static AppModule_ProvideImageLoaderFactory create(Provider<Context> contextProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    return new AppModule_ProvideImageLoaderFactory(contextProvider, okHttpClientProvider);
  }

  public static ImageLoader provideImageLoader(Context context, OkHttpClient okHttpClient) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideImageLoader(context, okHttpClient));
  }
}
