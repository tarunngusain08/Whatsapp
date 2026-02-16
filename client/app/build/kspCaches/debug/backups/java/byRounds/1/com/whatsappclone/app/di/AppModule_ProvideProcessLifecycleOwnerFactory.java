package com.whatsappclone.app.di;

import androidx.lifecycle.LifecycleOwner;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideProcessLifecycleOwnerFactory implements Factory<LifecycleOwner> {
  @Override
  public LifecycleOwner get() {
    return provideProcessLifecycleOwner();
  }

  public static AppModule_ProvideProcessLifecycleOwnerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LifecycleOwner provideProcessLifecycleOwner() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideProcessLifecycleOwner());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideProcessLifecycleOwnerFactory INSTANCE = new AppModule_ProvideProcessLifecycleOwnerFactory();
  }
}
