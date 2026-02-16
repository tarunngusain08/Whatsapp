package com.whatsappclone.feature.auth.ui.profile;

import com.whatsappclone.core.network.api.UserApi;
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

  public ProfileSetupViewModel_Factory(Provider<UserApi> userApiProvider) {
    this.userApiProvider = userApiProvider;
  }

  @Override
  public ProfileSetupViewModel get() {
    return newInstance(userApiProvider.get());
  }

  public static ProfileSetupViewModel_Factory create(Provider<UserApi> userApiProvider) {
    return new ProfileSetupViewModel_Factory(userApiProvider);
  }

  public static ProfileSetupViewModel newInstance(UserApi userApi) {
    return new ProfileSetupViewModel(userApi);
  }
}
