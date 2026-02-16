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
public final class ActiveChatTracker_Factory implements Factory<ActiveChatTracker> {
  @Override
  public ActiveChatTracker get() {
    return newInstance();
  }

  public static ActiveChatTracker_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ActiveChatTracker newInstance() {
    return new ActiveChatTracker();
  }

  private static final class InstanceHolder {
    private static final ActiveChatTracker_Factory INSTANCE = new ActiveChatTracker_Factory();
  }
}
