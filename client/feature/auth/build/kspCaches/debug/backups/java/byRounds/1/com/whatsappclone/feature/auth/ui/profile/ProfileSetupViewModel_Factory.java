package com.whatsappclone.feature.auth.ui.profile;

import com.whatsappclone.core.network.api.UserApi;
import com.whatsappclone.core.network.url.BaseUrlProvider;
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
public final class ProfileSetupViewModel_Factory implements Factory<ProfileSetupViewModel> {
  private final Provider<UserApi> userApiProvider;

  private final Provider<MediaRepository> mediaRepositoryProvider;

  private final Provider<BaseUrlProvider> baseUrlProvider;

  public ProfileSetupViewModel_Factory(Provider<UserApi> userApiProvider,
      Provider<MediaRepository> mediaRepositoryProvider,
      Provider<BaseUrlProvider> baseUrlProvider) {
    this.userApiProvider = userApiProvider;
    this.mediaRepositoryProvider = mediaRepositoryProvider;
    this.baseUrlProvider = baseUrlProvider;
  }

  @Override
  public ProfileSetupViewModel get() {
    return newInstance(userApiProvider.get(), mediaRepositoryProvider.get(), baseUrlProvider.get());
  }

  public static ProfileSetupViewModel_Factory create(Provider<UserApi> userApiProvider,
      Provider<MediaRepository> mediaRepositoryProvider,
      Provider<BaseUrlProvider> baseUrlProvider) {
    return new ProfileSetupViewModel_Factory(userApiProvider, mediaRepositoryProvider, baseUrlProvider);
  }

  public static ProfileSetupViewModel newInstance(UserApi userApi, MediaRepository mediaRepository,
      BaseUrlProvider baseUrlProvider) {
    return new ProfileSetupViewModel(userApi, mediaRepository, baseUrlProvider);
  }
}
