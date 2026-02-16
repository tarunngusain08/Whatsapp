package com.whatsappclone.app.notification;

import com.whatsappclone.core.network.api.NotificationApi;
import com.whatsappclone.core.network.token.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineScope;

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
public final class FCMTokenManager_Factory implements Factory<FCMTokenManager> {
  private final Provider<NotificationApi> notificationApiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<CoroutineScope> coroutineScopeProvider;

  public FCMTokenManager_Factory(Provider<NotificationApi> notificationApiProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    this.notificationApiProvider = notificationApiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.coroutineScopeProvider = coroutineScopeProvider;
  }

  @Override
  public FCMTokenManager get() {
    return newInstance(notificationApiProvider.get(), tokenManagerProvider.get(), coroutineScopeProvider.get());
  }

  public static FCMTokenManager_Factory create(Provider<NotificationApi> notificationApiProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<CoroutineScope> coroutineScopeProvider) {
    return new FCMTokenManager_Factory(notificationApiProvider, tokenManagerProvider, coroutineScopeProvider);
  }

  public static FCMTokenManager newInstance(NotificationApi notificationApi,
      TokenManager tokenManager, CoroutineScope coroutineScope) {
    return new FCMTokenManager(notificationApi, tokenManager, coroutineScope);
  }
}
