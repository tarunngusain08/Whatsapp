package com.whatsappclone.feature.group.ui.newgroup;

import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.feature.group.domain.CreateGroupUseCase;
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

  public NewGroupViewModel_Factory(Provider<UserDao> userDaoProvider,
      Provider<CreateGroupUseCase> createGroupUseCaseProvider) {
    this.userDaoProvider = userDaoProvider;
    this.createGroupUseCaseProvider = createGroupUseCaseProvider;
  }

  @Override
  public NewGroupViewModel get() {
    return newInstance(userDaoProvider.get(), createGroupUseCaseProvider.get());
  }

  public static NewGroupViewModel_Factory create(Provider<UserDao> userDaoProvider,
      Provider<CreateGroupUseCase> createGroupUseCaseProvider) {
    return new NewGroupViewModel_Factory(userDaoProvider, createGroupUseCaseProvider);
  }

  public static NewGroupViewModel newInstance(UserDao userDao,
      CreateGroupUseCase createGroupUseCase) {
    return new NewGroupViewModel(userDao, createGroupUseCase);
  }
}
