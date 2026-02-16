package com.whatsappclone.feature.auth.ui.otp;

import androidx.lifecycle.SavedStateHandle;
import com.whatsappclone.feature.auth.domain.SendOtpUseCase;
import com.whatsappclone.feature.auth.domain.VerifyOtpUseCase;
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
public final class OtpViewModel_Factory implements Factory<OtpViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider;

  private final Provider<SendOtpUseCase> sendOtpUseCaseProvider;

  public OtpViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider,
      Provider<SendOtpUseCase> sendOtpUseCaseProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.verifyOtpUseCaseProvider = verifyOtpUseCaseProvider;
    this.sendOtpUseCaseProvider = sendOtpUseCaseProvider;
  }

  @Override
  public OtpViewModel get() {
    return newInstance(savedStateHandleProvider.get(), verifyOtpUseCaseProvider.get(), sendOtpUseCaseProvider.get());
  }

  public static OtpViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider,
      Provider<SendOtpUseCase> sendOtpUseCaseProvider) {
    return new OtpViewModel_Factory(savedStateHandleProvider, verifyOtpUseCaseProvider, sendOtpUseCaseProvider);
  }

  public static OtpViewModel newInstance(SavedStateHandle savedStateHandle,
      VerifyOtpUseCase verifyOtpUseCase, SendOtpUseCase sendOtpUseCase) {
    return new OtpViewModel(savedStateHandle, verifyOtpUseCase, sendOtpUseCase);
  }
}
