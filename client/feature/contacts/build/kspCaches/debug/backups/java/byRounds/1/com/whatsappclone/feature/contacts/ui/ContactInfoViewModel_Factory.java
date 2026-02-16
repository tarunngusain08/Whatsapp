package com.whatsappclone.feature.contacts.ui;

import androidx.lifecycle.SavedStateHandle;
import com.whatsappclone.feature.chat.data.UserRepository;
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
public final class ContactInfoViewModel_Factory implements Factory<ContactInfoViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public ContactInfoViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public ContactInfoViewModel get() {
    return newInstance(savedStateHandleProvider.get(), userRepositoryProvider.get());
  }

  public static ContactInfoViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new ContactInfoViewModel_Factory(savedStateHandleProvider, userRepositoryProvider);
  }

  public static ContactInfoViewModel newInstance(SavedStateHandle savedStateHandle,
      UserRepository userRepository) {
    return new ContactInfoViewModel(savedStateHandle, userRepository);
  }
}
