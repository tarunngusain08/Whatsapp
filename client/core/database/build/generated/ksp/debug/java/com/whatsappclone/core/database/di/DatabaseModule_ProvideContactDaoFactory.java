package com.whatsappclone.core.database.di;

import com.whatsappclone.core.database.AppDatabase;
import com.whatsappclone.core.database.dao.ContactDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DatabaseModule_ProvideContactDaoFactory implements Factory<ContactDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideContactDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ContactDao get() {
    return provideContactDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideContactDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideContactDaoFactory(databaseProvider);
  }

  public static ContactDao provideContactDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideContactDao(database));
  }
}
