package com.whatsappclone.app;

import androidx.hilt.work.HiltWorkerFactory;
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

  public WhatsAppApplication_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<WhatsAppApplication> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new WhatsAppApplication_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(WhatsAppApplication instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.whatsappclone.app.WhatsAppApplication.workerFactory")
  public static void injectWorkerFactory(WhatsAppApplication instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
