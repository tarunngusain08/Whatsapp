package com.whatsappclone.feature.group.ui.info;

import android.content.SharedPreferences;
import androidx.lifecycle.SavedStateHandle;
import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.core.database.dao.ChatParticipantDao;
import com.whatsappclone.core.database.dao.GroupDao;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.api.ChatApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("javax.inject.Named")
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
public final class GroupInfoViewModel_Factory implements Factory<GroupInfoViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<ChatParticipantDao> chatParticipantDaoProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<ChatApi> chatApiProvider;

  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public GroupInfoViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<GroupDao> groupDaoProvider, Provider<ChatParticipantDao> chatParticipantDaoProvider,
      Provider<UserDao> userDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<ChatApi> chatApiProvider, Provider<SharedPreferences> encryptedPrefsProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.groupDaoProvider = groupDaoProvider;
    this.chatParticipantDaoProvider = chatParticipantDaoProvider;
    this.userDaoProvider = userDaoProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.chatApiProvider = chatApiProvider;
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public GroupInfoViewModel get() {
    return newInstance(savedStateHandleProvider.get(), groupDaoProvider.get(), chatParticipantDaoProvider.get(), userDaoProvider.get(), chatDaoProvider.get(), chatApiProvider.get(), encryptedPrefsProvider.get());
  }

  public static GroupInfoViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider, Provider<GroupDao> groupDaoProvider,
      Provider<ChatParticipantDao> chatParticipantDaoProvider, Provider<UserDao> userDaoProvider,
      Provider<ChatDao> chatDaoProvider, Provider<ChatApi> chatApiProvider,
      Provider<SharedPreferences> encryptedPrefsProvider) {
    return new GroupInfoViewModel_Factory(savedStateHandleProvider, groupDaoProvider, chatParticipantDaoProvider, userDaoProvider, chatDaoProvider, chatApiProvider, encryptedPrefsProvider);
  }

  public static GroupInfoViewModel newInstance(SavedStateHandle savedStateHandle, GroupDao groupDao,
      ChatParticipantDao chatParticipantDao, UserDao userDao, ChatDao chatDao, ChatApi chatApi,
      SharedPreferences encryptedPrefs) {
    return new GroupInfoViewModel(savedStateHandle, groupDao, chatParticipantDao, userDao, chatDao, chatApi, encryptedPrefs);
  }
}
