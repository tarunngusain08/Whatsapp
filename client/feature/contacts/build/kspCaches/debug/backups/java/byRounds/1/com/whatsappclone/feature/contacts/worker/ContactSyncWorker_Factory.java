package com.whatsappclone.feature.contacts.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.whatsappclone.feature.contacts.data.ContactRepository;
import dagger.internal.DaggerGenerated;
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
public final class ContactSyncWorker_Factory {
  private final Provider<ContactRepository> contactRepositoryProvider;

  public ContactSyncWorker_Factory(Provider<ContactRepository> contactRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  public ContactSyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, contactRepositoryProvider.get());
  }

  public static ContactSyncWorker_Factory create(
      Provider<ContactRepository> contactRepositoryProvider) {
    return new ContactSyncWorker_Factory(contactRepositoryProvider);
  }

  public static ContactSyncWorker newInstance(Context appContext, WorkerParameters workerParams,
      ContactRepository contactRepository) {
    return new ContactSyncWorker(appContext, workerParams, contactRepository);
  }
}
