package com.whatsappclone.core.database.di;

import com.whatsappclone.core.database.AppDatabase;
import com.whatsappclone.core.database.dao.ChatParticipantDao;
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
public final class DatabaseModule_ProvideChatParticipantDaoFactory implements Factory<ChatParticipantDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideChatParticipantDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ChatParticipantDao get() {
    return provideChatParticipantDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideChatParticipantDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideChatParticipantDaoFactory(databaseProvider);
  }

  public static ChatParticipantDao provideChatParticipantDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideChatParticipantDao(database));
  }
}
