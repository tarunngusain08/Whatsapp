package com.whatsappclone.feature.chat.ui.search;

import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.core.database.dao.ContactDao;
import com.whatsappclone.core.database.dao.MessageDao;
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
public final class GlobalSearchViewModel_Factory implements Factory<GlobalSearchViewModel> {
  private final Provider<ContactDao> contactDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<ChatDao> chatDaoProvider;

  public GlobalSearchViewModel_Factory(Provider<ContactDao> contactDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider) {
    this.contactDaoProvider = contactDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.chatDaoProvider = chatDaoProvider;
  }

  @Override
  public GlobalSearchViewModel get() {
    return newInstance(contactDaoProvider.get(), messageDaoProvider.get(), chatDaoProvider.get());
  }

  public static GlobalSearchViewModel_Factory create(Provider<ContactDao> contactDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider) {
    return new GlobalSearchViewModel_Factory(contactDaoProvider, messageDaoProvider, chatDaoProvider);
  }

  public static GlobalSearchViewModel newInstance(ContactDao contactDao, MessageDao messageDao,
      ChatDao chatDao) {
    return new GlobalSearchViewModel(contactDao, messageDao, chatDao);
  }
}
