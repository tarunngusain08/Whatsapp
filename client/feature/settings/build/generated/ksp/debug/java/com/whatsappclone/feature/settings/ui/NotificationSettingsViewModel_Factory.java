package com.whatsappclone.feature.settings.ui;

import com.whatsappclone.feature.settings.data.NotificationPreferencesStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class NotificationSettingsViewModel_Factory implements Factory<NotificationSettingsViewModel> {
  private final Provider<NotificationPreferencesStore> storeProvider;

  public NotificationSettingsViewModel_Factory(
      Provider<NotificationPreferencesStore> storeProvider) {
    this.storeProvider = storeProvider;
  }

  @Override
  public NotificationSettingsViewModel get() {
    return newInstance(storeProvider.get());
  }

  public static NotificationSettingsViewModel_Factory create(
      Provider<NotificationPreferencesStore> storeProvider) {
    return new NotificationSettingsViewModel_Factory(storeProvider);
  }

  public static NotificationSettingsViewModel newInstance(NotificationPreferencesStore store) {
    return new NotificationSettingsViewModel(store);
  }
}
