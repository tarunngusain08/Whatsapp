package com.whatsappclone.feature.auth.domain;

import com.whatsappclone.feature.auth.data.AuthRepository;
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
public final class VerifyOtpUseCase_Factory implements Factory<VerifyOtpUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public VerifyOtpUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public VerifyOtpUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static VerifyOtpUseCase_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new VerifyOtpUseCase_Factory(authRepositoryProvider);
  }

  public static VerifyOtpUseCase newInstance(AuthRepository authRepository) {
    return new VerifyOtpUseCase(authRepository);
  }
}
