package com.whatsappclone.feature.settings.ui;

import com.whatsappclone.feature.settings.data.ThemePreferencesStore;
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
public final class ThemeSettingsViewModel_Factory implements Factory<ThemeSettingsViewModel> {
  private final Provider<ThemePreferencesStore> themePreferencesStoreProvider;

  public ThemeSettingsViewModel_Factory(
      Provider<ThemePreferencesStore> themePreferencesStoreProvider) {
    this.themePreferencesStoreProvider = themePreferencesStoreProvider;
  }

  @Override
  public ThemeSettingsViewModel get() {
    return newInstance(themePreferencesStoreProvider.get());
  }

  public static ThemeSettingsViewModel_Factory create(
      Provider<ThemePreferencesStore> themePreferencesStoreProvider) {
    return new ThemeSettingsViewModel_Factory(themePreferencesStoreProvider);
  }

  public static ThemeSettingsViewModel newInstance(ThemePreferencesStore themePreferencesStore) {
    return new ThemeSettingsViewModel(themePreferencesStore);
  }
}
