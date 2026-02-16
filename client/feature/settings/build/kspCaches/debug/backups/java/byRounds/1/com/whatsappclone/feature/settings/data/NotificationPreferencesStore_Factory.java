package com.whatsappclone.feature.settings.data;

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
public final class NotificationPreferencesStore_Factory implements Factory<NotificationPreferencesStore> {
  private final Provider<Context> contextProvider;

  public NotificationPreferencesStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationPreferencesStore get() {
    return newInstance(contextProvider.get());
  }

  public static NotificationPreferencesStore_Factory create(Provider<Context> contextProvider) {
    return new NotificationPreferencesStore_Factory(contextProvider);
  }

  public static NotificationPreferencesStore newInstance(Context context) {
    return new NotificationPreferencesStore(context);
  }
}
