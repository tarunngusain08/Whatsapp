package com.whatsappclone.feature.contacts.worker;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = ContactSyncWorker.class
)
public interface ContactSyncWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("com.whatsappclone.feature.contacts.worker.ContactSyncWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(ContactSyncWorker_AssistedFactory factory);
}
