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
public final class SendOtpUseCase_Factory implements Factory<SendOtpUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public SendOtpUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public SendOtpUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static SendOtpUseCase_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new SendOtpUseCase_Factory(authRepositoryProvider);
  }

  public static SendOtpUseCase newInstance(AuthRepository authRepository) {
    return new SendOtpUseCase(authRepository);
  }
}
