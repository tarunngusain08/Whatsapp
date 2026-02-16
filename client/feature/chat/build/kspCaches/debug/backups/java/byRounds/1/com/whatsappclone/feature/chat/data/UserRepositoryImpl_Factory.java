package com.whatsappclone.feature.chat.data;

import android.content.SharedPreferences;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.api.UserApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class UserRepositoryImpl_Factory implements Factory<UserRepositoryImpl> {
  private final Provider<UserApi> userApiProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public UserRepositoryImpl_Factory(Provider<UserApi> userApiProvider,
      Provider<UserDao> userDaoProvider, Provider<SharedPreferences> encryptedPrefsProvider) {
    this.userApiProvider = userApiProvider;
    this.userDaoProvider = userDaoProvider;
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public UserRepositoryImpl get() {
    return newInstance(userApiProvider.get(), userDaoProvider.get(), encryptedPrefsProvider.get());
  }

  public static UserRepositoryImpl_Factory create(Provider<UserApi> userApiProvider,
      Provider<UserDao> userDaoProvider, Provider<SharedPreferences> encryptedPrefsProvider) {
    return new UserRepositoryImpl_Factory(userApiProvider, userDaoProvider, encryptedPrefsProvider);
  }

  public static UserRepositoryImpl newInstance(UserApi userApi, UserDao userDao,
      SharedPreferences encryptedPrefs) {
    return new UserRepositoryImpl(userApi, userDao, encryptedPrefs);
  }
}
