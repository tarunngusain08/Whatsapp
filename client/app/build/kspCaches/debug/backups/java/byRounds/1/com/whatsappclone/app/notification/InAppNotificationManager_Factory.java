package com.whatsappclone.app.notification;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class InAppNotificationManager_Factory implements Factory<InAppNotificationManager> {
  @Override
  public InAppNotificationManager get() {
    return newInstance();
  }

  public static InAppNotificationManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static InAppNotificationManager newInstance() {
    return new InAppNotificationManager();
  }

  private static final class InstanceHolder {
    private static final InAppNotificationManager_Factory INSTANCE = new InAppNotificationManager_Factory();
  }
}
