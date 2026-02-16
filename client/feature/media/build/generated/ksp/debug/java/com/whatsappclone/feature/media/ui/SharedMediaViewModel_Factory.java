package com.whatsappclone.feature.media.ui;

import androidx.lifecycle.SavedStateHandle;
import com.whatsappclone.core.database.dao.MediaDao;
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
public final class SharedMediaViewModel_Factory implements Factory<SharedMediaViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<MediaDao> mediaDaoProvider;

  public SharedMediaViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MessageDao> messageDaoProvider, Provider<MediaDao> mediaDaoProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.mediaDaoProvider = mediaDaoProvider;
  }

  @Override
  public SharedMediaViewModel get() {
    return newInstance(savedStateHandleProvider.get(), messageDaoProvider.get(), mediaDaoProvider.get());
  }

  public static SharedMediaViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider, Provider<MessageDao> messageDaoProvider,
      Provider<MediaDao> mediaDaoProvider) {
    return new SharedMediaViewModel_Factory(savedStateHandleProvider, messageDaoProvider, mediaDaoProvider);
  }

  public static SharedMediaViewModel newInstance(SavedStateHandle savedStateHandle,
      MessageDao messageDao, MediaDao mediaDao) {
    return new SharedMediaViewModel(savedStateHandle, messageDao, mediaDao);
  }
}
