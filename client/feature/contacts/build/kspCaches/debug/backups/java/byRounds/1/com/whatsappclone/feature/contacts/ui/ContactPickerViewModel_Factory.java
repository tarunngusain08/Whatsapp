package com.whatsappclone.feature.contacts.ui;

import android.content.Context;
import com.whatsappclone.feature.chat.data.ChatRepository;
import com.whatsappclone.feature.chat.data.UserRepository;
import com.whatsappclone.feature.contacts.data.ContactRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ContactPickerViewModel_Factory implements Factory<ContactPickerViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public ContactPickerViewModel_Factory(Provider<Context> contextProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public ContactPickerViewModel get() {
    return newInstance(contextProvider.get(), contactRepositoryProvider.get(), chatRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static ContactPickerViewModel_Factory create(Provider<Context> contextProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new ContactPickerViewModel_Factory(contextProvider, contactRepositoryProvider, chatRepositoryProvider, userRepositoryProvider);
  }

  public static ContactPickerViewModel newInstance(Context context,
      ContactRepository contactRepository, ChatRepository chatRepository,
      UserRepository userRepository) {
    return new ContactPickerViewModel(context, contactRepository, chatRepository, userRepository);
  }
}
