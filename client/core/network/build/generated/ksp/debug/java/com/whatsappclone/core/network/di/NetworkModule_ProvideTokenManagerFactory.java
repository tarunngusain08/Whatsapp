package com.whatsappclone.core.network.di;

import android.content.SharedPreferences;
import com.whatsappclone.core.network.api.AuthApi;
import com.whatsappclone.core.network.token.TokenManager;
import dagger.Lazy;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class NetworkModule_ProvideTokenManagerFactory implements Factory<TokenManager> {
  private final Provider<SharedPreferences> encryptedPrefsProvider;

  private final Provider<AuthApi> authApiProvider;

  public NetworkModule_ProvideTokenManagerFactory(
      Provider<SharedPreferences> encryptedPrefsProvider, Provider<AuthApi> authApiProvider) {
    this.encryptedPrefsProvider = encryptedPrefsProvider;
    this.authApiProvider = authApiProvider;
  }

  @Override
  public TokenManager get() {
    return provideTokenManager(encryptedPrefsProvider.get(), DoubleCheck.lazy(authApiProvider));
  }

  public static NetworkModule_ProvideTokenManagerFactory create(
      Provider<SharedPreferences> encryptedPrefsProvider, Provider<AuthApi> authApiProvider) {
    return new NetworkModule_ProvideTokenManagerFactory(encryptedPrefsProvider, authApiProvider);
  }

  public static TokenManager provideTokenManager(SharedPreferences encryptedPrefs,
      Lazy<AuthApi> authApi) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideTokenManager(encryptedPrefs, authApi));
  }
}
