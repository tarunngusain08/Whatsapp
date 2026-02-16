package com.whatsappclone.app.notification;

import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.core.database.dao.MessageDao;
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
public final class WhatsAppFCMService_MembersInjector implements MembersInjector<WhatsAppFCMService> {
  private final Provider<FCMTokenManager> fcmTokenManagerProvider;

  private final Provider<NotificationBuilder> notificationBuilderProvider;

  private final Provider<ActiveChatTracker> activeChatTrackerProvider;

  private final Provider<InAppNotificationManager> inAppNotificationManagerProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<CoroutineScope> appScopeProvider;

  public WhatsAppFCMService_MembersInjector(Provider<FCMTokenManager> fcmTokenManagerProvider,
      Provider<NotificationBuilder> notificationBuilderProvider,
      Provider<ActiveChatTracker> activeChatTrackerProvider,
      Provider<InAppNotificationManager> inAppNotificationManagerProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<CoroutineScope> appScopeProvider) {
    this.fcmTokenManagerProvider = fcmTokenManagerProvider;
    this.notificationBuilderProvider = notificationBuilderProvider;
    this.activeChatTrackerProvider = activeChatTrackerProvider;
    this.inAppNotificationManagerProvider = inAppNotificationManagerProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.appScopeProvider = appScopeProvider;
  }

  public static MembersInjector<WhatsAppFCMService> create(
      Provider<FCMTokenManager> fcmTokenManagerProvider,
      Provider<NotificationBuilder> notificationBuilderProvider,
      Provider<ActiveChatTracker> activeChatTrackerProvider,
      Provider<InAppNotificationManager> inAppNotificationManagerProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<CoroutineScope> appScopeProvider) {
    return new WhatsAppFCMService_MembersInjector(fcmTokenManagerProvider, notificationBuilderProvider, activeChatTrackerProvider, inAppNotificationManagerProvider, messageDaoProvider, chatDaoProvider, appScopeProvider);
  }

  @Override
  public void injectMembers(WhatsAppFCMService instance) {
    injectFcmTokenManager(instance, fcmTokenManagerProvider.get());
    injectNotificationBuilder(instance, notificationBuilderProvider.get());
    injectActiveChatTracker(instance, activeChatTrackerProvider.get());
    injectInAppNotificationManager(instance, inAppNotificationManagerProvider.get());
    injectMessageDao(instance, messageDaoProvider.get());
    injectChatDao(instance, chatDaoProvider.get());
    injectAppScope(instance, appScopeProvider.get());
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.WhatsAppFCMService.fcmTokenManager")
  public static void injectFcmTokenManager(WhatsAppFCMService instance,
      FCMTokenManager fcmTokenManager) {
    instance.fcmTokenManager = fcmTokenManager;
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.WhatsAppFCMService.notificationBuilder")
  public static void injectNotificationBuilder(WhatsAppFCMService instance,
      NotificationBuilder notificationBuilder) {
    instance.notificationBuilder = notificationBuilder;
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.WhatsAppFCMService.activeChatTracker")
  public static void injectActiveChatTracker(WhatsAppFCMService instance,
      ActiveChatTracker activeChatTracker) {
    instance.activeChatTracker = activeChatTracker;
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.WhatsAppFCMService.inAppNotificationManager")
  public static void injectInAppNotificationManager(WhatsAppFCMService instance,
      InAppNotificationManager inAppNotificationManager) {
    instance.inAppNotificationManager = inAppNotificationManager;
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.WhatsAppFCMService.messageDao")
  public static void injectMessageDao(WhatsAppFCMService instance, MessageDao messageDao) {
    instance.messageDao = messageDao;
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.WhatsAppFCMService.chatDao")
  public static void injectChatDao(WhatsAppFCMService instance, ChatDao chatDao) {
    instance.chatDao = chatDao;
  }

  @InjectedFieldSignature("com.whatsappclone.app.notification.WhatsAppFCMService.appScope")
  @Named("appScope")
  public static void injectAppScope(WhatsAppFCMService instance, CoroutineScope appScope) {
    instance.appScope = appScope;
  }
}
