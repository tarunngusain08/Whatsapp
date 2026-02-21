package com.whatsappclone.feature.contacts.data;

import android.content.ContentResolver;
import android.content.Context;
import com.whatsappclone.core.database.dao.ContactDao;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.api.UserApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ContactRepositoryImpl_Factory implements Factory<ContactRepositoryImpl> {
  private final Provider<ContentResolver> contentResolverProvider;

  private final Provider<Context> contextProvider;

  private final Provider<UserApi> userApiProvider;

  private final Provider<ContactDao> contactDaoProvider;

  private final Provider<UserDao> userDaoProvider;

  public ContactRepositoryImpl_Factory(Provider<ContentResolver> contentResolverProvider,
      Provider<Context> contextProvider, Provider<UserApi> userApiProvider,
      Provider<ContactDao> contactDaoProvider, Provider<UserDao> userDaoProvider) {
    this.contentResolverProvider = contentResolverProvider;
    this.contextProvider = contextProvider;
    this.userApiProvider = userApiProvider;
    this.contactDaoProvider = contactDaoProvider;
    this.userDaoProvider = userDaoProvider;
  }

  @Override
  public ContactRepositoryImpl get() {
    return newInstance(contentResolverProvider.get(), contextProvider.get(), userApiProvider.get(), contactDaoProvider.get(), userDaoProvider.get());
  }

  public static ContactRepositoryImpl_Factory create(
      Provider<ContentResolver> contentResolverProvider, Provider<Context> contextProvider,
      Provider<UserApi> userApiProvider, Provider<ContactDao> contactDaoProvider,
      Provider<UserDao> userDaoProvider) {
    return new ContactRepositoryImpl_Factory(contentResolverProvider, contextProvider, userApiProvider, contactDaoProvider, userDaoProvider);
  }

  public static ContactRepositoryImpl newInstance(ContentResolver contentResolver, Context context,
      UserApi userApi, ContactDao contactDao, UserDao userDao) {
    return new ContactRepositoryImpl(contentResolver, context, userApi, contactDao, userDao);
  }
}
