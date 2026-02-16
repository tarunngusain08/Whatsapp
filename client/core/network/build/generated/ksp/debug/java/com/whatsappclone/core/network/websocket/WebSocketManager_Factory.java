package com.whatsappclone.core.network.websocket;

import com.whatsappclone.core.network.token.TokenManager;
import com.whatsappclone.core.network.url.BaseUrlProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

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
public final class WebSocketManager_Factory implements Factory<WebSocketManager> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<BaseUrlProvider> baseUrlProvider;

  private final Provider<Json> jsonProvider;

  public WebSocketManager_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<BaseUrlProvider> baseUrlProvider,
      Provider<Json> jsonProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.baseUrlProvider = baseUrlProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public WebSocketManager get() {
    return newInstance(okHttpClientProvider.get(), tokenManagerProvider.get(), baseUrlProvider.get(), jsonProvider.get());
  }

  public static WebSocketManager_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<BaseUrlProvider> baseUrlProvider,
      Provider<Json> jsonProvider) {
    return new WebSocketManager_Factory(okHttpClientProvider, tokenManagerProvider, baseUrlProvider, jsonProvider);
  }

  public static WebSocketManager newInstance(OkHttpClient okHttpClient, TokenManager tokenManager,
      BaseUrlProvider baseUrlProvider, Json json) {
    return new WebSocketManager(okHttpClient, tokenManager, baseUrlProvider, json);
  }
}
