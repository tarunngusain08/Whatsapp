package com.whatsappclone.app.error;

import com.whatsappclone.core.network.token.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class GlobalErrorHandler_Factory implements Factory<GlobalErrorHandler> {
  private final Provider<TokenManager> tokenManagerProvider;

  public GlobalErrorHandler_Factory(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public GlobalErrorHandler get() {
    return newInstance(tokenManagerProvider.get());
  }

  public static GlobalErrorHandler_Factory create(Provider<TokenManager> tokenManagerProvider) {
    return new GlobalErrorHandler_Factory(tokenManagerProvider);
  }

  public static GlobalErrorHandler newInstance(TokenManager tokenManager) {
    return new GlobalErrorHandler(tokenManager);
  }
}
