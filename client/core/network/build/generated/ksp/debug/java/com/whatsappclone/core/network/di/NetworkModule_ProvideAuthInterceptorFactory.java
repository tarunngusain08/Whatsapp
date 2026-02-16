package com.whatsappclone.core.network.di;

import com.whatsappclone.core.network.interceptor.AuthInterceptor;
import com.whatsappclone.core.network.token.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class NetworkModule_ProvideAuthInterceptorFactory implements Factory<AuthInterceptor> {
  private final Provider<TokenManager> tokenManagerProvider;

  public NetworkModule_ProvideAuthInterceptorFactory(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public AuthInterceptor get() {
    return provideAuthInterceptor(tokenManagerProvider.get());
  }

  public static NetworkModule_ProvideAuthInterceptorFactory create(
      Provider<TokenManager> tokenManagerProvider) {
    return new NetworkModule_ProvideAuthInterceptorFactory(tokenManagerProvider);
  }

  public static AuthInterceptor provideAuthInterceptor(TokenManager tokenManager) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAuthInterceptor(tokenManager));
  }
}
