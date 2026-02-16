package com.whatsappclone.core.database.di;

import com.whatsappclone.core.database.AppDatabase;
import com.whatsappclone.core.database.dao.MediaDao;
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
public final class DatabaseModule_ProvideMediaDaoFactory implements Factory<MediaDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideMediaDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MediaDao get() {
    return provideMediaDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideMediaDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideMediaDaoFactory(databaseProvider);
  }

  public static MediaDao provideMediaDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMediaDao(database));
  }
}
