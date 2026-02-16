package com.whatsappclone.feature.settings.ui;

import com.whatsappclone.feature.settings.data.PrivacyPreferencesStore;
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
public final class PrivacySettingsViewModel_Factory implements Factory<PrivacySettingsViewModel> {
  private final Provider<PrivacyPreferencesStore> privacyPreferencesStoreProvider;

  public PrivacySettingsViewModel_Factory(
      Provider<PrivacyPreferencesStore> privacyPreferencesStoreProvider) {
    this.privacyPreferencesStoreProvider = privacyPreferencesStoreProvider;
  }

  @Override
  public PrivacySettingsViewModel get() {
    return newInstance(privacyPreferencesStoreProvider.get());
  }

  public static PrivacySettingsViewModel_Factory create(
      Provider<PrivacyPreferencesStore> privacyPreferencesStoreProvider) {
    return new PrivacySettingsViewModel_Factory(privacyPreferencesStoreProvider);
  }

  public static PrivacySettingsViewModel newInstance(
      PrivacyPreferencesStore privacyPreferencesStore) {
    return new PrivacySettingsViewModel(privacyPreferencesStore);
  }
}
