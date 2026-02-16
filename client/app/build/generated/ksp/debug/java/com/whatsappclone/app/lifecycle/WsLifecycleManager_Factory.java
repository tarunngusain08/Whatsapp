package com.whatsappclone.app.lifecycle;

import com.whatsappclone.app.data.websocket.SyncOnReconnectManager;
import com.whatsappclone.app.data.websocket.WsEventRouter;
import com.whatsappclone.core.network.token.TokenManager;
import com.whatsappclone.core.network.websocket.WebSocketManager;
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
public final class WsLifecycleManager_Factory implements Factory<WsLifecycleManager> {
  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<WsEventRouter> wsEventRouterProvider;

  private final Provider<SyncOnReconnectManager> syncOnReconnectManagerProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public WsLifecycleManager_Factory(Provider<WebSocketManager> webSocketManagerProvider,
      Provider<WsEventRouter> wsEventRouterProvider,
      Provider<SyncOnReconnectManager> syncOnReconnectManagerProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.wsEventRouterProvider = wsEventRouterProvider;
    this.syncOnReconnectManagerProvider = syncOnReconnectManagerProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public WsLifecycleManager get() {
    return newInstance(webSocketManagerProvider.get(), wsEventRouterProvider.get(), syncOnReconnectManagerProvider.get(), tokenManagerProvider.get());
  }

  public static WsLifecycleManager_Factory create(
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<WsEventRouter> wsEventRouterProvider,
      Provider<SyncOnReconnectManager> syncOnReconnectManagerProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new WsLifecycleManager_Factory(webSocketManagerProvider, wsEventRouterProvider, syncOnReconnectManagerProvider, tokenManagerProvider);
  }

  public static WsLifecycleManager newInstance(WebSocketManager webSocketManager,
      WsEventRouter wsEventRouter, SyncOnReconnectManager syncOnReconnectManager,
      TokenManager tokenManager) {
    return new WsLifecycleManager(webSocketManager, wsEventRouter, syncOnReconnectManager, tokenManager);
  }
}
