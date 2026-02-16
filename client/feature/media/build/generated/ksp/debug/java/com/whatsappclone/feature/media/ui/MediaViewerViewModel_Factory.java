package com.whatsappclone.feature.media.ui;

import androidx.lifecycle.SavedStateHandle;
import com.whatsappclone.feature.media.data.MediaRepository;
import com.whatsappclone.feature.media.util.MediaDownloadManager;
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
public final class MediaViewerViewModel_Factory implements Factory<MediaViewerViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<MediaRepository> mediaRepositoryProvider;

  private final Provider<MediaDownloadManager> downloadManagerProvider;

  public MediaViewerViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MediaRepository> mediaRepositoryProvider,
      Provider<MediaDownloadManager> downloadManagerProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.mediaRepositoryProvider = mediaRepositoryProvider;
    this.downloadManagerProvider = downloadManagerProvider;
  }

  @Override
  public MediaViewerViewModel get() {
    return newInstance(savedStateHandleProvider.get(), mediaRepositoryProvider.get(), downloadManagerProvider.get());
  }

  public static MediaViewerViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<MediaRepository> mediaRepositoryProvider,
      Provider<MediaDownloadManager> downloadManagerProvider) {
    return new MediaViewerViewModel_Factory(savedStateHandleProvider, mediaRepositoryProvider, downloadManagerProvider);
  }

  public static MediaViewerViewModel newInstance(SavedStateHandle savedStateHandle,
      MediaRepository mediaRepository, MediaDownloadManager downloadManager) {
    return new MediaViewerViewModel(savedStateHandle, mediaRepository, downloadManager);
  }
}
