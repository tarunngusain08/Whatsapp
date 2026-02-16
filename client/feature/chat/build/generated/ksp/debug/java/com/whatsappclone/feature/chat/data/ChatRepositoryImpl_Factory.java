package com.whatsappclone.feature.chat.data;

import android.content.SharedPreferences;
import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.core.database.dao.ChatParticipantDao;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.network.api.ChatApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ChatRepositoryImpl_Factory implements Factory<ChatRepositoryImpl> {
  private final Provider<ChatApi> chatApiProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<ChatParticipantDao> chatParticipantDaoProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public ChatRepositoryImpl_Factory(Provider<ChatApi> chatApiProvider,
      Provider<ChatDao> chatDaoProvider, Provider<ChatParticipantDao> chatParticipantDaoProvider,
      Provider<UserDao> userDaoProvider, Provider<SharedPreferences> encryptedPrefsProvider) {
    this.chatApiProvider = chatApiProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.chatParticipantDaoProvider = chatParticipantDaoProvider;
    this.userDaoProvider = userDaoProvider;
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public ChatRepositoryImpl get() {
    return newInstance(chatApiProvider.get(), chatDaoProvider.get(), chatParticipantDaoProvider.get(), userDaoProvider.get(), encryptedPrefsProvider.get());
  }

  public static ChatRepositoryImpl_Factory create(Provider<ChatApi> chatApiProvider,
      Provider<ChatDao> chatDaoProvider, Provider<ChatParticipantDao> chatParticipantDaoProvider,
      Provider<UserDao> userDaoProvider, Provider<SharedPreferences> encryptedPrefsProvider) {
    return new ChatRepositoryImpl_Factory(chatApiProvider, chatDaoProvider, chatParticipantDaoProvider, userDaoProvider, encryptedPrefsProvider);
  }

  public static ChatRepositoryImpl newInstance(ChatApi chatApi, ChatDao chatDao,
      ChatParticipantDao chatParticipantDao, UserDao userDao, SharedPreferences encryptedPrefs) {
    return new ChatRepositoryImpl(chatApi, chatDao, chatParticipantDao, userDao, encryptedPrefs);
  }
}
