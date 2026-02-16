package com.whatsappclone.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class AppModule_ProvideAppCoroutineScopeFactory implements Factory<CoroutineScope> {
  @Override
  public CoroutineScope get() {
    return provideAppCoroutineScope();
  }

  public static AppModule_ProvideAppCoroutineScopeFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CoroutineScope provideAppCoroutineScope() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAppCoroutineScope());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideAppCoroutineScopeFactory INSTANCE = new AppModule_ProvideAppCoroutineScopeFactory();
  }
}
