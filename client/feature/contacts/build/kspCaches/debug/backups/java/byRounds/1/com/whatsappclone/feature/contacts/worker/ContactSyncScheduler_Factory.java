package com.whatsappclone.feature.contacts.worker;

import androidx.work.WorkManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ContactSyncScheduler_Factory implements Factory<ContactSyncScheduler> {
  private final Provider<WorkManager> workManagerProvider;

  public ContactSyncScheduler_Factory(Provider<WorkManager> workManagerProvider) {
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public ContactSyncScheduler get() {
    return newInstance(workManagerProvider.get());
  }

  public static ContactSyncScheduler_Factory create(Provider<WorkManager> workManagerProvider) {
    return new ContactSyncScheduler_Factory(workManagerProvider);
  }

  public static ContactSyncScheduler newInstance(WorkManager workManager) {
    return new ContactSyncScheduler(workManager);
  }
}
