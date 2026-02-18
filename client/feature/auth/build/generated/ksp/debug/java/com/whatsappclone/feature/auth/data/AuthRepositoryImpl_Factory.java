package com.whatsappclone.feature.auth.data;

import android.content.SharedPreferences;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.api.AuthApi;
import com.whatsappclone.core.network.token.DeviceTokenManager;
import com.whatsappclone.core.network.token.TokenManager;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<AuthApi> authApiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<DeviceTokenManager> deviceTokenManagerProvider;

  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public AuthRepositoryImpl_Factory(Provider<AuthApi> authApiProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<UserDao> userDaoProvider,
      Provider<DeviceTokenManager> deviceTokenManagerProvider,
      Provider<SharedPreferences> encryptedPrefsProvider) {
    this.authApiProvider = authApiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.userDaoProvider = userDaoProvider;
    this.deviceTokenManagerProvider = deviceTokenManagerProvider;
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(authApiProvider.get(), tokenManagerProvider.get(), userDaoProvider.get(), deviceTokenManagerProvider.get(), encryptedPrefsProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<AuthApi> authApiProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<UserDao> userDaoProvider,
      Provider<DeviceTokenManager> deviceTokenManagerProvider,
      Provider<SharedPreferences> encryptedPrefsProvider) {
    return new AuthRepositoryImpl_Factory(authApiProvider, tokenManagerProvider, userDaoProvider, deviceTokenManagerProvider, encryptedPrefsProvider);
  }

  public static AuthRepositoryImpl newInstance(AuthApi authApi, TokenManager tokenManager,
      UserDao userDao, DeviceTokenManager deviceTokenManager, SharedPreferences encryptedPrefs) {
    return new AuthRepositoryImpl(authApi, tokenManager, userDao, deviceTokenManager, encryptedPrefs);
  }
}
