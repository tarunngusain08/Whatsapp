package com.whatsappclone.feature.auth.data;

import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.api.AuthApi;
import com.whatsappclone.core.network.api.NotificationApi;
import com.whatsappclone.core.network.token.TokenManager;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<AuthApi> authApiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<NotificationApi> notificationApiProvider;

  public AuthRepositoryImpl_Factory(Provider<AuthApi> authApiProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<UserDao> userDaoProvider,
      Provider<NotificationApi> notificationApiProvider) {
    this.authApiProvider = authApiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.userDaoProvider = userDaoProvider;
    this.notificationApiProvider = notificationApiProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(authApiProvider.get(), tokenManagerProvider.get(), userDaoProvider.get(), notificationApiProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<AuthApi> authApiProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<UserDao> userDaoProvider,
      Provider<NotificationApi> notificationApiProvider) {
    return new AuthRepositoryImpl_Factory(authApiProvider, tokenManagerProvider, userDaoProvider, notificationApiProvider);
  }

  public static AuthRepositoryImpl newInstance(AuthApi authApi, TokenManager tokenManager,
      UserDao userDao, NotificationApi notificationApi) {
    return new AuthRepositoryImpl(authApi, tokenManager, userDao, notificationApi);
  }
}
