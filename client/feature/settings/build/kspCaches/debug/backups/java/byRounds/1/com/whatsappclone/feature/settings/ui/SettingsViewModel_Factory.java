package com.whatsappclone.feature.settings.ui;

import android.content.SharedPreferences;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.feature.auth.data.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public SettingsViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<UserDao> userDaoProvider, Provider<SharedPreferences> encryptedPrefsProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.userDaoProvider = userDaoProvider;
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(authRepositoryProvider.get(), userDaoProvider.get(), encryptedPrefsProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<UserDao> userDaoProvider, Provider<SharedPreferences> encryptedPrefsProvider) {
    return new SettingsViewModel_Factory(authRepositoryProvider, userDaoProvider, encryptedPrefsProvider);
  }

  public static SettingsViewModel newInstance(AuthRepository authRepository, UserDao userDao,
      SharedPreferences encryptedPrefs) {
    return new SettingsViewModel(authRepository, userDao, encryptedPrefs);
  }
}
