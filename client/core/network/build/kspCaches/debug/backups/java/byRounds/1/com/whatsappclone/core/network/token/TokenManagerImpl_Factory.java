package com.whatsappclone.core.network.token;

import android.content.SharedPreferences;
import com.whatsappclone.core.network.api.AuthApi;
import dagger.Lazy;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
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
public final class TokenManagerImpl_Factory implements Factory<TokenManagerImpl> {
  private final Provider<SharedPreferences> encryptedPrefsProvider;

  private final Provider<AuthApi> authApiProvider;

  public TokenManagerImpl_Factory(Provider<SharedPreferences> encryptedPrefsProvider,
      Provider<AuthApi> authApiProvider) {
    this.encryptedPrefsProvider = encryptedPrefsProvider;
    this.authApiProvider = authApiProvider;
  }

  @Override
  public TokenManagerImpl get() {
    return newInstance(encryptedPrefsProvider.get(), DoubleCheck.lazy(authApiProvider));
  }

  public static TokenManagerImpl_Factory create(Provider<SharedPreferences> encryptedPrefsProvider,
      Provider<AuthApi> authApiProvider) {
    return new TokenManagerImpl_Factory(encryptedPrefsProvider, authApiProvider);
  }

  public static TokenManagerImpl newInstance(SharedPreferences encryptedPrefs,
      Lazy<AuthApi> authApi) {
    return new TokenManagerImpl(encryptedPrefs, authApi);
  }
}
