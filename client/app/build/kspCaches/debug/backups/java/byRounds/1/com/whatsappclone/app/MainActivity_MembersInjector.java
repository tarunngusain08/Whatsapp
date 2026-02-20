package com.whatsappclone.app;

import com.whatsappclone.app.error.GlobalErrorHandler;
import com.whatsappclone.app.lifecycle.WsLifecycleManager;
import com.whatsappclone.core.network.url.BaseUrlProvider;
import com.whatsappclone.feature.auth.data.AuthRepository;
import com.whatsappclone.feature.chat.call.CallService;
import com.whatsappclone.feature.settings.data.PrivacyPreferencesStore;
import com.whatsappclone.feature.settings.data.ThemePreferencesStore;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<GlobalErrorHandler> globalErrorHandlerProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<BaseUrlProvider> baseUrlProvider;

  private final Provider<WsLifecycleManager> wsLifecycleManagerProvider;

  private final Provider<CallService> callServiceProvider;

  private final Provider<ThemePreferencesStore> themePreferencesStoreProvider;

  private final Provider<PrivacyPreferencesStore> privacyPreferencesStoreProvider;

  public MainActivity_MembersInjector(Provider<GlobalErrorHandler> globalErrorHandlerProvider,
      Provider<AuthRepository> authRepositoryProvider, Provider<BaseUrlProvider> baseUrlProvider,
      Provider<WsLifecycleManager> wsLifecycleManagerProvider,
      Provider<CallService> callServiceProvider,
      Provider<ThemePreferencesStore> themePreferencesStoreProvider,
      Provider<PrivacyPreferencesStore> privacyPreferencesStoreProvider) {
    this.globalErrorHandlerProvider = globalErrorHandlerProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.baseUrlProvider = baseUrlProvider;
    this.wsLifecycleManagerProvider = wsLifecycleManagerProvider;
    this.callServiceProvider = callServiceProvider;
    this.themePreferencesStoreProvider = themePreferencesStoreProvider;
    this.privacyPreferencesStoreProvider = privacyPreferencesStoreProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<GlobalErrorHandler> globalErrorHandlerProvider,
      Provider<AuthRepository> authRepositoryProvider, Provider<BaseUrlProvider> baseUrlProvider,
      Provider<WsLifecycleManager> wsLifecycleManagerProvider,
      Provider<CallService> callServiceProvider,
      Provider<ThemePreferencesStore> themePreferencesStoreProvider,
      Provider<PrivacyPreferencesStore> privacyPreferencesStoreProvider) {
    return new MainActivity_MembersInjector(globalErrorHandlerProvider, authRepositoryProvider, baseUrlProvider, wsLifecycleManagerProvider, callServiceProvider, themePreferencesStoreProvider, privacyPreferencesStoreProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectGlobalErrorHandler(instance, globalErrorHandlerProvider.get());
    injectAuthRepository(instance, authRepositoryProvider.get());
    injectBaseUrlProvider(instance, baseUrlProvider.get());
    injectWsLifecycleManager(instance, wsLifecycleManagerProvider.get());
    injectCallService(instance, callServiceProvider.get());
    injectThemePreferencesStore(instance, themePreferencesStoreProvider.get());
    injectPrivacyPreferencesStore(instance, privacyPreferencesStoreProvider.get());
  }

  @InjectedFieldSignature("com.whatsappclone.app.MainActivity.globalErrorHandler")
  public static void injectGlobalErrorHandler(MainActivity instance,
      GlobalErrorHandler globalErrorHandler) {
    instance.globalErrorHandler = globalErrorHandler;
  }

  @InjectedFieldSignature("com.whatsappclone.app.MainActivity.authRepository")
  public static void injectAuthRepository(MainActivity instance, AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }

  @InjectedFieldSignature("com.whatsappclone.app.MainActivity.baseUrlProvider")
  public static void injectBaseUrlProvider(MainActivity instance, BaseUrlProvider baseUrlProvider) {
    instance.baseUrlProvider = baseUrlProvider;
  }

  @InjectedFieldSignature("com.whatsappclone.app.MainActivity.wsLifecycleManager")
  public static void injectWsLifecycleManager(MainActivity instance,
      WsLifecycleManager wsLifecycleManager) {
    instance.wsLifecycleManager = wsLifecycleManager;
  }

  @InjectedFieldSignature("com.whatsappclone.app.MainActivity.callService")
  public static void injectCallService(MainActivity instance, CallService callService) {
    instance.callService = callService;
  }

  @InjectedFieldSignature("com.whatsappclone.app.MainActivity.themePreferencesStore")
  public static void injectThemePreferencesStore(MainActivity instance,
      ThemePreferencesStore themePreferencesStore) {
    instance.themePreferencesStore = themePreferencesStore;
  }

  @InjectedFieldSignature("com.whatsappclone.app.MainActivity.privacyPreferencesStore")
  public static void injectPrivacyPreferencesStore(MainActivity instance,
      PrivacyPreferencesStore privacyPreferencesStore) {
    instance.privacyPreferencesStore = privacyPreferencesStore;
  }
}
