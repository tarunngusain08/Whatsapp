package com.whatsappclone.app.notification;

import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.feature.chat.domain.SendMessageUseCase;
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

  private final Provider<SendMessageUseCase> sendMessageUseCaseProvider;

  public NotificationActionReceiver_MembersInjector(Provider<CoroutineScope> appScopeProvider,
      Provider<ChatDao> chatDaoProvider, Provider<SendMessageUseCase> sendMessageUseCaseProvider) {
    this.appScopeProvider = appScopeProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.sendMessageUseCaseProvider = sendMessageUseCaseProvider;
  }

  public static MembersInjector<NotificationActionReceiver> create(
      Provider<CoroutineScope> appScopeProvider, Provider<ChatDao> chatDaoProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider) {
    return new NotificationActionReceiver_MembersInjector(appScopeProvider, chatDaoProvider, sendMessageUseCaseProvider);
  }

  @Override
  public void injectMembers(NotificationActionReceiver instance) {
    injectAppScope(instance, appScopeProvider.get());
    injectChatDao(instance, chatDaoProvider.get());
    injectSendMessageUseCase(instance, sendMessageUseCaseProvider.get());
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

  @InjectedFieldSignature("com.whatsappclone.app.notification.NotificationActionReceiver.sendMessageUseCase")
  public static void injectSendMessageUseCase(NotificationActionReceiver instance,
      SendMessageUseCase sendMessageUseCase) {
    instance.sendMessageUseCase = sendMessageUseCase;
  }
}
