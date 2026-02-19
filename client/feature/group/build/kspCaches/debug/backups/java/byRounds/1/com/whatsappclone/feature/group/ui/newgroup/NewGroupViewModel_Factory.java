package com.whatsappclone.feature.group.ui.newgroup;

import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.url.BaseUrlProvider;
import com.whatsappclone.feature.group.domain.CreateGroupUseCase;
import com.whatsappclone.feature.media.data.MediaRepository;
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
public final class NewGroupViewModel_Factory implements Factory<NewGroupViewModel> {
  private final Provider<UserDao> userDaoProvider;

  private final Provider<CreateGroupUseCase> createGroupUseCaseProvider;

  private final Provider<MediaRepository> mediaRepositoryProvider;

  private final Provider<BaseUrlProvider> baseUrlProvider;

  public NewGroupViewModel_Factory(Provider<UserDao> userDaoProvider,
      Provider<CreateGroupUseCase> createGroupUseCaseProvider,
      Provider<MediaRepository> mediaRepositoryProvider,
      Provider<BaseUrlProvider> baseUrlProvider) {
    this.userDaoProvider = userDaoProvider;
    this.createGroupUseCaseProvider = createGroupUseCaseProvider;
    this.mediaRepositoryProvider = mediaRepositoryProvider;
    this.baseUrlProvider = baseUrlProvider;
  }

  @Override
  public NewGroupViewModel get() {
    return newInstance(userDaoProvider.get(), createGroupUseCaseProvider.get(), mediaRepositoryProvider.get(), baseUrlProvider.get());
  }

  public static NewGroupViewModel_Factory create(Provider<UserDao> userDaoProvider,
      Provider<CreateGroupUseCase> createGroupUseCaseProvider,
      Provider<MediaRepository> mediaRepositoryProvider,
      Provider<BaseUrlProvider> baseUrlProvider) {
    return new NewGroupViewModel_Factory(userDaoProvider, createGroupUseCaseProvider, mediaRepositoryProvider, baseUrlProvider);
  }

  public static NewGroupViewModel newInstance(UserDao userDao,
      CreateGroupUseCase createGroupUseCase, MediaRepository mediaRepository,
      BaseUrlProvider baseUrlProvider) {
    return new NewGroupViewModel(userDao, createGroupUseCase, mediaRepository, baseUrlProvider);
  }
}
