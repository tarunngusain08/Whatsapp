package com.whatsappclone.feature.profile.ui;

import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.api.UserApi;
import com.whatsappclone.feature.media.data.MediaRepository;
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
public final class ProfileEditViewModel_Factory implements Factory<ProfileEditViewModel> {
  private final Provider<UserApi> userApiProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<MediaRepository> mediaRepositoryProvider;

  public ProfileEditViewModel_Factory(Provider<UserApi> userApiProvider,
      Provider<UserDao> userDaoProvider, Provider<MediaRepository> mediaRepositoryProvider) {
    this.userApiProvider = userApiProvider;
    this.userDaoProvider = userDaoProvider;
    this.mediaRepositoryProvider = mediaRepositoryProvider;
  }

  @Override
  public ProfileEditViewModel get() {
    return newInstance(userApiProvider.get(), userDaoProvider.get(), mediaRepositoryProvider.get());
  }

  public static ProfileEditViewModel_Factory create(Provider<UserApi> userApiProvider,
      Provider<UserDao> userDaoProvider, Provider<MediaRepository> mediaRepositoryProvider) {
    return new ProfileEditViewModel_Factory(userApiProvider, userDaoProvider, mediaRepositoryProvider);
  }

  public static ProfileEditViewModel newInstance(UserApi userApi, UserDao userDao,
      MediaRepository mediaRepository) {
    return new ProfileEditViewModel(userApi, userDao, mediaRepository);
  }
}
