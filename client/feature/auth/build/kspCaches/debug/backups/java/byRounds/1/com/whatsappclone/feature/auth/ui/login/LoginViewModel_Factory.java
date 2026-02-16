package com.whatsappclone.feature.auth.ui.login;

import com.whatsappclone.feature.auth.domain.SendOtpUseCase;
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
public final class LoginViewModel_Factory implements Factory<LoginViewModel> {
  private final Provider<SendOtpUseCase> sendOtpUseCaseProvider;

  public LoginViewModel_Factory(Provider<SendOtpUseCase> sendOtpUseCaseProvider) {
    this.sendOtpUseCaseProvider = sendOtpUseCaseProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(sendOtpUseCaseProvider.get());
  }

  public static LoginViewModel_Factory create(Provider<SendOtpUseCase> sendOtpUseCaseProvider) {
    return new LoginViewModel_Factory(sendOtpUseCaseProvider);
  }

  public static LoginViewModel newInstance(SendOtpUseCase sendOtpUseCase) {
    return new LoginViewModel(sendOtpUseCase);
  }
}
