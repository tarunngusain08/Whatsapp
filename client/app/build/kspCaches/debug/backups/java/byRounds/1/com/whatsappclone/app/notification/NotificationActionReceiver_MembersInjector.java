package com.whatsappclone.app.notification;

import com.whatsappclone.core.database.dao.ChatDao;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Named;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineScope;

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
public final class NotificationActionReceiver_MembersInjector implements MembersInjector<NotificationActionReceiver> {
  private final Provider<CoroutineScope> appScopeProvider;

  private final Provider<ChatDao> chatDaoProvider;

  public NotificationActionReceiver_MembersInjector(Provider<CoroutineScope> appScopeProvider,
      Provider<ChatDao> chatDaoProvider) {
    this.appScopeProvider = appScopeProvider;
    this.chatDaoProvider = chatDaoProvider;
  }

  public static MembersInjector<NotificationActionReceiver> create(
      Provider<CoroutineScope> appScopeProvider, Provider<ChatDao> chatDaoProvider) {
    return new NotificationActionReceiver_MembersInjector(appScopeProvider, chatDaoProvider);
  }

  @Override
  public void injectMembers(NotificationActionReceiver instance) {
    injectAppScope(instance, appScopeProvider.get());
    injectChatDao(instance, chatDaoProvider.get());
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.NotificationActionReceiver.appScope")
  @Named("appScope")
  public static void injectAppScope(NotificationActionReceiver instance, CoroutineScope appScope) {
    instance.appScope = appScope;
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.NotificationActionReceiver.chatDao")
  public static void injectChatDao(NotificationActionReceiver instance, ChatDao chatDao) {
    instance.chatDao = chatDao;
  }
}
