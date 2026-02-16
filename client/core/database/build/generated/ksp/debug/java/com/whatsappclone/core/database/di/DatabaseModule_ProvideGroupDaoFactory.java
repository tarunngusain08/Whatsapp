package com.whatsappclone.core.database.di;

import com.whatsappclone.core.database.AppDatabase;
import com.whatsappclone.core.database.dao.GroupDao;
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
public final class DatabaseModule_ProvideGroupDaoFactory implements Factory<GroupDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideGroupDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public GroupDao get() {
    return provideGroupDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideGroupDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideGroupDaoFactory(databaseProvider);
  }

  public static GroupDao provideGroupDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideGroupDao(database));
  }
}
