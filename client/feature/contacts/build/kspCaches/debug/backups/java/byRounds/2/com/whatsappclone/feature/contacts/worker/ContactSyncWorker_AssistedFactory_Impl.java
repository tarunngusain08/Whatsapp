package com.whatsappclone.feature.contacts.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class ContactSyncWorker_AssistedFactory_Impl implements ContactSyncWorker_AssistedFactory {
  private final ContactSyncWorker_Factory delegateFactory;

  ContactSyncWorker_AssistedFactory_Impl(ContactSyncWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public ContactSyncWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<ContactSyncWorker_AssistedFactory> create(
      ContactSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new ContactSyncWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<ContactSyncWorker_AssistedFactory> createFactoryProvider(
      ContactSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new ContactSyncWorker_AssistedFactory_Impl(delegateFactory));
  }
}
