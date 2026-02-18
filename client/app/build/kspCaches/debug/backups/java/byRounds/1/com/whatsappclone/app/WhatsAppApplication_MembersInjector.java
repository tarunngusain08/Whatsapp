package com.whatsappclone.app;

import androidx.hilt.work.HiltWorkerFactory;
import coil3.ImageLoader;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class WhatsAppApplication_MembersInjector implements MembersInjector<WhatsAppApplication> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  private final Provider<ImageLoader> imageLoaderProvider;

  public WhatsAppApplication_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<ImageLoader> imageLoaderProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
    this.imageLoaderProvider = imageLoaderProvider;
  }

  public static MembersInjector<WhatsAppApplication> create(
      Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<ImageLoader> imageLoaderProvider) {
    return new WhatsAppApplication_MembersInjector(workerFactoryProvider, imageLoaderProvider);
  }

  @Override
  public void injectMembers(WhatsAppApplication instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
    injectImageLoader(instance, imageLoaderProvider.get());
  }

  @InjectedFieldSignature("com.whatsappclone.app.WhatsAppApplication.workerFactory")
  public static void injectWorkerFactory(WhatsAppApplication instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }

  @InjectedFieldSignature("com.whatsappclone.app.WhatsAppApplication.imageLoader")
  public static void injectImageLoader(WhatsAppApplication instance, ImageLoader imageLoader) {
    instance.imageLoader = imageLoader;
  }
}
