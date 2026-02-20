package com.whatsappclone.app;

import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import coil3.ImageLoader;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.whatsappclone.app.data.websocket.SyncOnReconnectManager;
import com.whatsappclone.app.data.websocket.WsEventRouter;
import com.whatsappclone.app.di.AppModule_ProvideAppCoroutineScopeFactory;
import com.whatsappclone.app.di.AppModule_ProvideDeviceTokenManagerFactory;
import com.whatsappclone.app.di.AppModule_ProvideImageLoaderFactory;
import com.whatsappclone.app.error.GlobalErrorHandler;
import com.whatsappclone.app.lifecycle.WsLifecycleManager;
import com.whatsappclone.app.notification.ActiveChatTracker;
import com.whatsappclone.app.notification.FCMTokenManager;
import com.whatsappclone.app.notification.InAppNotificationManager;
import com.whatsappclone.app.notification.NotificationActionReceiver;
import com.whatsappclone.app.notification.NotificationActionReceiver_MembersInjector;
import com.whatsappclone.app.notification.NotificationBuilder;
import com.whatsappclone.app.notification.WhatsAppFCMService;
import com.whatsappclone.app.notification.WhatsAppFCMService_MembersInjector;
import com.whatsappclone.core.database.AppDatabase;
import com.whatsappclone.core.database.dao.ChatDao;
import com.whatsappclone.core.database.dao.ChatParticipantDao;
import com.whatsappclone.core.database.dao.ContactDao;
import com.whatsappclone.core.database.dao.GroupDao;
import com.whatsappclone.core.database.dao.MediaDao;
import com.whatsappclone.core.database.dao.MessageDao;
import com.whatsappclone.core.database.dao.UserDao;
import com.whatsappclone.core.database.di.DatabaseModule_ProvideAppDatabaseFactory;
import com.whatsappclone.core.database.di.DatabaseModule_ProvideChatDaoFactory;
import com.whatsappclone.core.database.di.DatabaseModule_ProvideChatParticipantDaoFactory;
import com.whatsappclone.core.database.di.DatabaseModule_ProvideContactDaoFactory;
import com.whatsappclone.core.database.di.DatabaseModule_ProvideGroupDaoFactory;
import com.whatsappclone.core.database.di.DatabaseModule_ProvideMediaDaoFactory;
import com.whatsappclone.core.database.di.DatabaseModule_ProvideMessageDaoFactory;
import com.whatsappclone.core.database.di.DatabaseModule_ProvideUserDaoFactory;
import com.whatsappclone.core.network.api.AuthApi;
import com.whatsappclone.core.network.api.ChatApi;
import com.whatsappclone.core.network.api.MediaApi;
import com.whatsappclone.core.network.api.MessageApi;
import com.whatsappclone.core.network.api.NotificationApi;
import com.whatsappclone.core.network.api.StatusApi;
import com.whatsappclone.core.network.api.UserApi;
import com.whatsappclone.core.network.di.NetworkModule_ProvideAuthApiFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideAuthInterceptorFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideBaseUrlProviderFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideChatApiFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideEncryptedSharedPreferencesFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideJsonFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideLoggingInterceptorFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideMediaApiFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideMessageApiFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideNetworkDataStoreFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideNotificationApiFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideOkHttpClientFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideRetrofitFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideStatusApiFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideTokenManagerFactory;
import com.whatsappclone.core.network.di.NetworkModule_ProvideUserApiFactory;
import com.whatsappclone.core.network.interceptor.AuthInterceptor;
import com.whatsappclone.core.network.token.DeviceTokenManager;
import com.whatsappclone.core.network.token.TokenManager;
import com.whatsappclone.core.network.url.BaseUrlProvider;
import com.whatsappclone.core.network.websocket.TypingStateHolder;
import com.whatsappclone.core.network.websocket.WebSocketManager;
import com.whatsappclone.feature.auth.data.AuthRepository;
import com.whatsappclone.feature.auth.data.AuthRepositoryImpl;
import com.whatsappclone.feature.auth.domain.SendOtpUseCase;
import com.whatsappclone.feature.auth.domain.VerifyOtpUseCase;
import com.whatsappclone.feature.auth.ui.login.LoginViewModel;
import com.whatsappclone.feature.auth.ui.login.LoginViewModel_HiltModules;
import com.whatsappclone.feature.auth.ui.login.LoginViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.auth.ui.login.LoginViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.auth.ui.otp.OtpViewModel;
import com.whatsappclone.feature.auth.ui.otp.OtpViewModel_HiltModules;
import com.whatsappclone.feature.auth.ui.otp.OtpViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.auth.ui.otp.OtpViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.auth.ui.profile.ProfileSetupViewModel;
import com.whatsappclone.feature.auth.ui.profile.ProfileSetupViewModel_HiltModules;
import com.whatsappclone.feature.auth.ui.profile.ProfileSetupViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.auth.ui.profile.ProfileSetupViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.call.CallService;
import com.whatsappclone.feature.chat.call.CallViewModel;
import com.whatsappclone.feature.chat.call.CallViewModel_HiltModules;
import com.whatsappclone.feature.chat.call.CallViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.call.CallViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.data.ChatRepositoryImpl;
import com.whatsappclone.feature.chat.data.MessageRepositoryImpl;
import com.whatsappclone.feature.chat.data.UserRepositoryImpl;
import com.whatsappclone.feature.chat.domain.GetChatsUseCase;
import com.whatsappclone.feature.chat.domain.MarkMessagesReadUseCase;
import com.whatsappclone.feature.chat.domain.SendMessageUseCase;
import com.whatsappclone.feature.chat.ui.archived.ArchivedChatsViewModel;
import com.whatsappclone.feature.chat.ui.archived.ArchivedChatsViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.archived.ArchivedChatsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.ui.archived.ArchivedChatsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailViewModel;
import com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.ui.chatlist.ChatListViewModel;
import com.whatsappclone.feature.chat.ui.chatlist.ChatListViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.chatlist.ChatListViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.ui.chatlist.ChatListViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.ui.forward.ForwardPickerViewModel;
import com.whatsappclone.feature.chat.ui.forward.ForwardPickerViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.forward.ForwardPickerViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.ui.forward.ForwardPickerViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.ui.receipts.ReceiptDetailsViewModel;
import com.whatsappclone.feature.chat.ui.receipts.ReceiptDetailsViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.receipts.ReceiptDetailsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.ui.receipts.ReceiptDetailsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.ui.search.GlobalSearchViewModel;
import com.whatsappclone.feature.chat.ui.search.GlobalSearchViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.search.GlobalSearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.ui.search.GlobalSearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.ui.starred.StarredMessagesViewModel;
import com.whatsappclone.feature.chat.ui.starred.StarredMessagesViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.starred.StarredMessagesViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.ui.starred.StarredMessagesViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.ui.status.StatusViewModel;
import com.whatsappclone.feature.chat.ui.status.StatusViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.status.StatusViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.chat.ui.status.StatusViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.chat.worker.PendingMessageWorker;
import com.whatsappclone.feature.chat.worker.PendingMessageWorker_AssistedFactory;
import com.whatsappclone.feature.chat.worker.ScheduledMessageWorker;
import com.whatsappclone.feature.chat.worker.ScheduledMessageWorker_AssistedFactory;
import com.whatsappclone.feature.contacts.data.ContactRepositoryImpl;
import com.whatsappclone.feature.contacts.di.ContactsModule_Companion_ProvideContentResolverFactory;
import com.whatsappclone.feature.contacts.ui.BlockedContactsViewModel;
import com.whatsappclone.feature.contacts.ui.BlockedContactsViewModel_HiltModules;
import com.whatsappclone.feature.contacts.ui.BlockedContactsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.contacts.ui.BlockedContactsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.contacts.ui.ContactInfoViewModel;
import com.whatsappclone.feature.contacts.ui.ContactInfoViewModel_HiltModules;
import com.whatsappclone.feature.contacts.ui.ContactInfoViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.contacts.ui.ContactInfoViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.contacts.ui.ContactPickerViewModel;
import com.whatsappclone.feature.contacts.ui.ContactPickerViewModel_HiltModules;
import com.whatsappclone.feature.contacts.ui.ContactPickerViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.contacts.ui.ContactPickerViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.contacts.worker.ContactSyncWorker;
import com.whatsappclone.feature.contacts.worker.ContactSyncWorker_AssistedFactory;
import com.whatsappclone.feature.group.di.GroupModule_ProvideCreateGroupUseCaseFactory;
import com.whatsappclone.feature.group.domain.CreateGroupUseCase;
import com.whatsappclone.feature.group.ui.info.GroupInfoViewModel;
import com.whatsappclone.feature.group.ui.info.GroupInfoViewModel_HiltModules;
import com.whatsappclone.feature.group.ui.info.GroupInfoViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.group.ui.info.GroupInfoViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.group.ui.newgroup.NewGroupViewModel;
import com.whatsappclone.feature.group.ui.newgroup.NewGroupViewModel_HiltModules;
import com.whatsappclone.feature.group.ui.newgroup.NewGroupViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.group.ui.newgroup.NewGroupViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.media.audio.VoiceRecorder;
import com.whatsappclone.feature.media.data.MediaRepositoryImpl;
import com.whatsappclone.feature.media.ui.MediaViewerViewModel;
import com.whatsappclone.feature.media.ui.MediaViewerViewModel_HiltModules;
import com.whatsappclone.feature.media.ui.MediaViewerViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.media.ui.MediaViewerViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.media.ui.SharedMediaViewModel;
import com.whatsappclone.feature.media.ui.SharedMediaViewModel_HiltModules;
import com.whatsappclone.feature.media.ui.SharedMediaViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.media.ui.SharedMediaViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.media.util.ImageCompressor;
import com.whatsappclone.feature.media.util.MediaDownloadManager;
import com.whatsappclone.feature.media.util.VideoCompressor;
import com.whatsappclone.feature.media.worker.MediaCleanupWorker;
import com.whatsappclone.feature.media.worker.MediaCleanupWorker_AssistedFactory;
import com.whatsappclone.feature.profile.ui.ProfileEditViewModel;
import com.whatsappclone.feature.profile.ui.ProfileEditViewModel_HiltModules;
import com.whatsappclone.feature.profile.ui.ProfileEditViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.profile.ui.ProfileEditViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.settings.data.NotificationPreferencesStore;
import com.whatsappclone.feature.settings.data.PrivacyPreferencesStore;
import com.whatsappclone.feature.settings.data.ThemePreferencesStore;
import com.whatsappclone.feature.settings.ui.NotificationSettingsViewModel;
import com.whatsappclone.feature.settings.ui.NotificationSettingsViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.NotificationSettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.settings.ui.NotificationSettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.settings.ui.PrivacySettingsViewModel;
import com.whatsappclone.feature.settings.ui.PrivacySettingsViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.PrivacySettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.settings.ui.PrivacySettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.settings.ui.SettingsViewModel;
import com.whatsappclone.feature.settings.ui.SettingsViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.settings.ui.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.settings.ui.StorageUsageViewModel;
import com.whatsappclone.feature.settings.ui.StorageUsageViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.StorageUsageViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.settings.ui.StorageUsageViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.whatsappclone.feature.settings.ui.ThemeSettingsViewModel;
import com.whatsappclone.feature.settings.ui.ThemeSettingsViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.ThemeSettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.whatsappclone.feature.settings.ui.ThemeSettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DelegateFactory;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

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
public final class DaggerWhatsAppApplication_HiltComponents_SingletonC {
  private DaggerWhatsAppApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public WhatsAppApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements WhatsAppApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public WhatsAppApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements WhatsAppApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public WhatsAppApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements WhatsAppApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public WhatsAppApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements WhatsAppApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public WhatsAppApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements WhatsAppApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public WhatsAppApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements WhatsAppApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public WhatsAppApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements WhatsAppApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public WhatsAppApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends WhatsAppApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends WhatsAppApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends WhatsAppApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends WhatsAppApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
      injectMainActivity2(arg0);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(25).put(ArchivedChatsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ArchivedChatsViewModel_HiltModules.KeyModule.provide()).put(BlockedContactsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, BlockedContactsViewModel_HiltModules.KeyModule.provide()).put(CallViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CallViewModel_HiltModules.KeyModule.provide()).put(ChatDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChatDetailViewModel_HiltModules.KeyModule.provide()).put(ChatListViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChatListViewModel_HiltModules.KeyModule.provide()).put(ContactInfoViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ContactInfoViewModel_HiltModules.KeyModule.provide()).put(ContactPickerViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ContactPickerViewModel_HiltModules.KeyModule.provide()).put(ForwardPickerViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ForwardPickerViewModel_HiltModules.KeyModule.provide()).put(GlobalSearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, GlobalSearchViewModel_HiltModules.KeyModule.provide()).put(GroupInfoViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, GroupInfoViewModel_HiltModules.KeyModule.provide()).put(LoginViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, LoginViewModel_HiltModules.KeyModule.provide()).put(MediaViewerViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, MediaViewerViewModel_HiltModules.KeyModule.provide()).put(NewGroupViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, NewGroupViewModel_HiltModules.KeyModule.provide()).put(NotificationSettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, NotificationSettingsViewModel_HiltModules.KeyModule.provide()).put(OtpViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, OtpViewModel_HiltModules.KeyModule.provide()).put(PrivacySettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, PrivacySettingsViewModel_HiltModules.KeyModule.provide()).put(ProfileEditViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ProfileEditViewModel_HiltModules.KeyModule.provide()).put(ProfileSetupViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ProfileSetupViewModel_HiltModules.KeyModule.provide()).put(ReceiptDetailsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ReceiptDetailsViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(SharedMediaViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SharedMediaViewModel_HiltModules.KeyModule.provide()).put(StarredMessagesViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, StarredMessagesViewModel_HiltModules.KeyModule.provide()).put(StatusViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, StatusViewModel_HiltModules.KeyModule.provide()).put(StorageUsageViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, StorageUsageViewModel_HiltModules.KeyModule.provide()).put(ThemeSettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ThemeSettingsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectGlobalErrorHandler(instance, singletonCImpl.globalErrorHandlerProvider.get());
      MainActivity_MembersInjector.injectAuthRepository(instance, singletonCImpl.bindAuthRepositoryProvider.get());
      MainActivity_MembersInjector.injectBaseUrlProvider(instance, singletonCImpl.provideBaseUrlProvider.get());
      MainActivity_MembersInjector.injectWsLifecycleManager(instance, singletonCImpl.wsLifecycleManagerProvider.get());
      MainActivity_MembersInjector.injectCallService(instance, singletonCImpl.callServiceProvider.get());
      MainActivity_MembersInjector.injectThemePreferencesStore(instance, singletonCImpl.themePreferencesStoreProvider.get());
      MainActivity_MembersInjector.injectPrivacyPreferencesStore(instance, singletonCImpl.privacyPreferencesStoreProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends WhatsAppApplication_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<ArchivedChatsViewModel> archivedChatsViewModelProvider;

    private Provider<BlockedContactsViewModel> blockedContactsViewModelProvider;

    private Provider<CallViewModel> callViewModelProvider;

    private Provider<ChatDetailViewModel> chatDetailViewModelProvider;

    private Provider<ChatListViewModel> chatListViewModelProvider;

    private Provider<ContactInfoViewModel> contactInfoViewModelProvider;

    private Provider<ContactPickerViewModel> contactPickerViewModelProvider;

    private Provider<ForwardPickerViewModel> forwardPickerViewModelProvider;

    private Provider<GlobalSearchViewModel> globalSearchViewModelProvider;

    private Provider<GroupInfoViewModel> groupInfoViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private Provider<MediaViewerViewModel> mediaViewerViewModelProvider;

    private Provider<NewGroupViewModel> newGroupViewModelProvider;

    private Provider<NotificationSettingsViewModel> notificationSettingsViewModelProvider;

    private Provider<OtpViewModel> otpViewModelProvider;

    private Provider<PrivacySettingsViewModel> privacySettingsViewModelProvider;

    private Provider<ProfileEditViewModel> profileEditViewModelProvider;

    private Provider<ProfileSetupViewModel> profileSetupViewModelProvider;

    private Provider<ReceiptDetailsViewModel> receiptDetailsViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SharedMediaViewModel> sharedMediaViewModelProvider;

    private Provider<StarredMessagesViewModel> starredMessagesViewModelProvider;

    private Provider<StatusViewModel> statusViewModelProvider;

    private Provider<StorageUsageViewModel> storageUsageViewModelProvider;

    private Provider<ThemeSettingsViewModel> themeSettingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private MarkMessagesReadUseCase markMessagesReadUseCase() {
      return new MarkMessagesReadUseCase(singletonCImpl.messageRepositoryImplProvider.get(), singletonCImpl.chatRepositoryImplProvider.get(), singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.webSocketManagerProvider.get());
    }

    private GetChatsUseCase getChatsUseCase() {
      return new GetChatsUseCase(singletonCImpl.chatRepositoryImplProvider.get());
    }

    private SendOtpUseCase sendOtpUseCase() {
      return new SendOtpUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private VerifyOtpUseCase verifyOtpUseCase() {
      return new VerifyOtpUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.archivedChatsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.blockedContactsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.callViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.chatDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.chatListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.contactInfoViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.contactPickerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.forwardPickerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.globalSearchViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.groupInfoViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.mediaViewerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.newGroupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.notificationSettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.otpViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
      this.privacySettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 15);
      this.profileEditViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 16);
      this.profileSetupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 17);
      this.receiptDetailsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 18);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 19);
      this.sharedMediaViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 20);
      this.starredMessagesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 21);
      this.statusViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 22);
      this.storageUsageViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 23);
      this.themeSettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 24);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(25).put(ArchivedChatsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) archivedChatsViewModelProvider)).put(BlockedContactsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) blockedContactsViewModelProvider)).put(CallViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) callViewModelProvider)).put(ChatDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) chatDetailViewModelProvider)).put(ChatListViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) chatListViewModelProvider)).put(ContactInfoViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) contactInfoViewModelProvider)).put(ContactPickerViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) contactPickerViewModelProvider)).put(ForwardPickerViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) forwardPickerViewModelProvider)).put(GlobalSearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) globalSearchViewModelProvider)).put(GroupInfoViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) groupInfoViewModelProvider)).put(LoginViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) loginViewModelProvider)).put(MediaViewerViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) mediaViewerViewModelProvider)).put(NewGroupViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) newGroupViewModelProvider)).put(NotificationSettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) notificationSettingsViewModelProvider)).put(OtpViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) otpViewModelProvider)).put(PrivacySettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) privacySettingsViewModelProvider)).put(ProfileEditViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) profileEditViewModelProvider)).put(ProfileSetupViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) profileSetupViewModelProvider)).put(ReceiptDetailsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) receiptDetailsViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(SharedMediaViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) sharedMediaViewModelProvider)).put(StarredMessagesViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) starredMessagesViewModelProvider)).put(StatusViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) statusViewModelProvider)).put(StorageUsageViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) storageUsageViewModelProvider)).put(ThemeSettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) themeSettingsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.whatsappclone.feature.chat.ui.archived.ArchivedChatsViewModel 
          return (T) new ArchivedChatsViewModel(singletonCImpl.chatRepositoryImplProvider.get());

          case 1: // com.whatsappclone.feature.contacts.ui.BlockedContactsViewModel 
          return (T) new BlockedContactsViewModel(singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.userRepositoryImplProvider.get());

          case 2: // com.whatsappclone.feature.chat.call.CallViewModel 
          return (T) new CallViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.callServiceProvider.get());

          case 3: // com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailViewModel 
          return (T) new ChatDetailViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.messageRepositoryImplProvider.get(), singletonCImpl.chatRepositoryImplProvider.get(), singletonCImpl.userRepositoryImplProvider.get(), singletonCImpl.sendMessageUseCase(), viewModelCImpl.markMessagesReadUseCase(), singletonCImpl.webSocketManagerProvider.get(), singletonCImpl.typingStateHolderProvider.get(), singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.provideChatParticipantDaoProvider.get(), singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.mediaRepositoryImplProvider.get(), singletonCImpl.voiceRecorderProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.whatsappclone.feature.chat.ui.chatlist.ChatListViewModel 
          return (T) new ChatListViewModel(viewModelCImpl.getChatsUseCase(), singletonCImpl.chatRepositoryImplProvider.get(), singletonCImpl.typingStateHolderProvider.get());

          case 5: // com.whatsappclone.feature.contacts.ui.ContactInfoViewModel 
          return (T) new ContactInfoViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.userRepositoryImplProvider.get(), singletonCImpl.provideChatDaoProvider.get());

          case 6: // com.whatsappclone.feature.contacts.ui.ContactPickerViewModel 
          return (T) new ContactPickerViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.contactRepositoryImplProvider.get(), singletonCImpl.chatRepositoryImplProvider.get(), singletonCImpl.userRepositoryImplProvider.get());

          case 7: // com.whatsappclone.feature.chat.ui.forward.ForwardPickerViewModel 
          return (T) new ForwardPickerViewModel(singletonCImpl.chatRepositoryImplProvider.get(), singletonCImpl.messageRepositoryImplProvider.get());

          case 8: // com.whatsappclone.feature.chat.ui.search.GlobalSearchViewModel 
          return (T) new GlobalSearchViewModel(singletonCImpl.provideContactDaoProvider.get(), singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 9: // com.whatsappclone.feature.group.ui.info.GroupInfoViewModel 
          return (T) new GroupInfoViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.provideGroupDaoProvider.get(), singletonCImpl.provideChatParticipantDaoProvider.get(), singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideChatApiProvider.get(), singletonCImpl.mediaRepositoryImplProvider.get(), singletonCImpl.provideBaseUrlProvider.get(), singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 10: // com.whatsappclone.feature.auth.ui.login.LoginViewModel 
          return (T) new LoginViewModel(viewModelCImpl.sendOtpUseCase());

          case 11: // com.whatsappclone.feature.media.ui.MediaViewerViewModel 
          return (T) new MediaViewerViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.mediaRepositoryImplProvider.get(), singletonCImpl.mediaDownloadManagerProvider.get());

          case 12: // com.whatsappclone.feature.group.ui.newgroup.NewGroupViewModel 
          return (T) new NewGroupViewModel(singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.createGroupUseCase(), singletonCImpl.mediaRepositoryImplProvider.get(), singletonCImpl.provideBaseUrlProvider.get());

          case 13: // com.whatsappclone.feature.settings.ui.NotificationSettingsViewModel 
          return (T) new NotificationSettingsViewModel(singletonCImpl.notificationPreferencesStoreProvider.get());

          case 14: // com.whatsappclone.feature.auth.ui.otp.OtpViewModel 
          return (T) new OtpViewModel(viewModelCImpl.savedStateHandle, viewModelCImpl.verifyOtpUseCase(), viewModelCImpl.sendOtpUseCase());

          case 15: // com.whatsappclone.feature.settings.ui.PrivacySettingsViewModel 
          return (T) new PrivacySettingsViewModel(singletonCImpl.privacyPreferencesStoreProvider.get());

          case 16: // com.whatsappclone.feature.profile.ui.ProfileEditViewModel 
          return (T) new ProfileEditViewModel(singletonCImpl.provideUserApiProvider.get(), singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.mediaRepositoryImplProvider.get(), singletonCImpl.provideBaseUrlProvider.get());

          case 17: // com.whatsappclone.feature.auth.ui.profile.ProfileSetupViewModel 
          return (T) new ProfileSetupViewModel(singletonCImpl.provideUserApiProvider.get(), singletonCImpl.mediaRepositoryImplProvider.get(), singletonCImpl.provideBaseUrlProvider.get());

          case 18: // com.whatsappclone.feature.chat.ui.receipts.ReceiptDetailsViewModel 
          return (T) new ReceiptDetailsViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.provideMessageApiProvider.get(), singletonCImpl.provideUserDaoProvider.get());

          case 19: // com.whatsappclone.feature.settings.ui.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.bindAuthRepositoryProvider.get(), singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideUserApiProvider.get(), singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 20: // com.whatsappclone.feature.media.ui.SharedMediaViewModel 
          return (T) new SharedMediaViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.provideMediaDaoProvider.get());

          case 21: // com.whatsappclone.feature.chat.ui.starred.StarredMessagesViewModel 
          return (T) new StarredMessagesViewModel(singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.provideUserDaoProvider.get());

          case 22: // com.whatsappclone.feature.chat.ui.status.StatusViewModel 
          return (T) new StatusViewModel(singletonCImpl.provideStatusApiProvider.get());

          case 23: // com.whatsappclone.feature.settings.ui.StorageUsageViewModel 
          return (T) new StorageUsageViewModel(singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideMessageDaoProvider.get());

          case 24: // com.whatsappclone.feature.settings.ui.ThemeSettingsViewModel 
          return (T) new ThemeSettingsViewModel(singletonCImpl.themePreferencesStoreProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends WhatsAppApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends WhatsAppApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectWhatsAppFCMService(WhatsAppFCMService arg0) {
      injectWhatsAppFCMService2(arg0);
    }

    @CanIgnoreReturnValue
    private WhatsAppFCMService injectWhatsAppFCMService2(WhatsAppFCMService instance) {
      WhatsAppFCMService_MembersInjector.injectFcmTokenManager(instance, singletonCImpl.fCMTokenManagerProvider.get());
      WhatsAppFCMService_MembersInjector.injectNotificationBuilder(instance, singletonCImpl.notificationBuilderProvider.get());
      WhatsAppFCMService_MembersInjector.injectActiveChatTracker(instance, singletonCImpl.activeChatTrackerProvider.get());
      WhatsAppFCMService_MembersInjector.injectInAppNotificationManager(instance, singletonCImpl.inAppNotificationManagerProvider.get());
      WhatsAppFCMService_MembersInjector.injectMessageDao(instance, singletonCImpl.provideMessageDaoProvider.get());
      WhatsAppFCMService_MembersInjector.injectChatDao(instance, singletonCImpl.provideChatDaoProvider.get());
      WhatsAppFCMService_MembersInjector.injectAppScope(instance, singletonCImpl.provideAppCoroutineScopeProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends WhatsAppApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<ContentResolver> provideContentResolverProvider;

    private Provider<SharedPreferences> provideEncryptedSharedPreferencesProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<AuthApi> provideAuthApiProvider;

    private Provider<TokenManager> provideTokenManagerProvider;

    private Provider<AuthInterceptor> provideAuthInterceptorProvider;

    private Provider<HttpLoggingInterceptor> provideLoggingInterceptorProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<DataStore<Preferences>> provideNetworkDataStoreProvider;

    private Provider<BaseUrlProvider> provideBaseUrlProvider;

    private Provider<UserApi> provideUserApiProvider;

    private Provider<AppDatabase> provideAppDatabaseProvider;

    private Provider<ContactDao> provideContactDaoProvider;

    private Provider<UserDao> provideUserDaoProvider;

    private Provider<ContactRepositoryImpl> contactRepositoryImplProvider;

    private Provider<ContactSyncWorker_AssistedFactory> contactSyncWorker_AssistedFactoryProvider;

    private Provider<MediaDao> provideMediaDaoProvider;

    private Provider<MediaCleanupWorker_AssistedFactory> mediaCleanupWorker_AssistedFactoryProvider;

    private Provider<MessageApi> provideMessageApiProvider;

    private Provider<MessageDao> provideMessageDaoProvider;

    private Provider<ChatDao> provideChatDaoProvider;

    private Provider<WebSocketManager> webSocketManagerProvider;

    private Provider<MessageRepositoryImpl> messageRepositoryImplProvider;

    private Provider<PendingMessageWorker_AssistedFactory> pendingMessageWorker_AssistedFactoryProvider;

    private Provider<ScheduledMessageWorker_AssistedFactory> scheduledMessageWorker_AssistedFactoryProvider;

    private Provider<ImageLoader> provideImageLoaderProvider;

    private Provider<CoroutineScope> provideAppCoroutineScopeProvider;

    private Provider<GlobalErrorHandler> globalErrorHandlerProvider;

    private Provider<NotificationApi> provideNotificationApiProvider;

    private Provider<FCMTokenManager> fCMTokenManagerProvider;

    private Provider<DeviceTokenManager> provideDeviceTokenManagerProvider;

    private Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    private Provider<AuthRepository> bindAuthRepositoryProvider;

    private Provider<ChatParticipantDao> provideChatParticipantDaoProvider;

    private Provider<TypingStateHolder> typingStateHolderProvider;

    private Provider<CallService> callServiceProvider;

    private Provider<WsEventRouter> wsEventRouterProvider;

    private Provider<ChatApi> provideChatApiProvider;

    private Provider<SyncOnReconnectManager> syncOnReconnectManagerProvider;

    private Provider<WsLifecycleManager> wsLifecycleManagerProvider;

    private Provider<ThemePreferencesStore> themePreferencesStoreProvider;

    private Provider<PrivacyPreferencesStore> privacyPreferencesStoreProvider;

    private Provider<ChatRepositoryImpl> chatRepositoryImplProvider;

    private Provider<UserRepositoryImpl> userRepositoryImplProvider;

    private Provider<MediaApi> provideMediaApiProvider;

    private Provider<ImageCompressor> imageCompressorProvider;

    private Provider<VideoCompressor> videoCompressorProvider;

    private Provider<MediaRepositoryImpl> mediaRepositoryImplProvider;

    private Provider<VoiceRecorder> voiceRecorderProvider;

    private Provider<GroupDao> provideGroupDaoProvider;

    private Provider<MediaDownloadManager> mediaDownloadManagerProvider;

    private Provider<NotificationPreferencesStore> notificationPreferencesStoreProvider;

    private Provider<StatusApi> provideStatusApiProvider;

    private Provider<NotificationBuilder> notificationBuilderProvider;

    private Provider<ActiveChatTracker> activeChatTrackerProvider;

    private Provider<InAppNotificationManager> inAppNotificationManagerProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);
      initialize2(applicationContextModuleParam);
      initialize3(applicationContextModuleParam);

    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return ImmutableMap.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>of("com.whatsappclone.feature.contacts.worker.ContactSyncWorker", ((Provider) contactSyncWorker_AssistedFactoryProvider), "com.whatsappclone.feature.media.worker.MediaCleanupWorker", ((Provider) mediaCleanupWorker_AssistedFactoryProvider), "com.whatsappclone.feature.chat.worker.PendingMessageWorker", ((Provider) pendingMessageWorker_AssistedFactoryProvider), "com.whatsappclone.feature.chat.worker.ScheduledMessageWorker", ((Provider) scheduledMessageWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    private SendMessageUseCase sendMessageUseCase() {
      return new SendMessageUseCase(messageRepositoryImplProvider.get());
    }

    private CreateGroupUseCase createGroupUseCase() {
      return GroupModule_ProvideCreateGroupUseCaseFactory.provideCreateGroupUseCase(chatRepositoryImplProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideContentResolverProvider = DoubleCheck.provider(new SwitchingProvider<ContentResolver>(singletonCImpl, 2));
      this.provideEncryptedSharedPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<SharedPreferences>(singletonCImpl, 8));
      this.provideRetrofitProvider = new DelegateFactory<>();
      this.provideAuthApiProvider = DoubleCheck.provider(new SwitchingProvider<AuthApi>(singletonCImpl, 9));
      this.provideTokenManagerProvider = DoubleCheck.provider(new SwitchingProvider<TokenManager>(singletonCImpl, 7));
      this.provideAuthInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<AuthInterceptor>(singletonCImpl, 6));
      this.provideLoggingInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<HttpLoggingInterceptor>(singletonCImpl, 10));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 5));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 11));
      this.provideNetworkDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 13));
      this.provideBaseUrlProvider = DoubleCheck.provider(new SwitchingProvider<BaseUrlProvider>(singletonCImpl, 12));
      DelegateFactory.setDelegate(provideRetrofitProvider, DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 4)));
      this.provideUserApiProvider = DoubleCheck.provider(new SwitchingProvider<UserApi>(singletonCImpl, 3));
      this.provideAppDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 15));
      this.provideContactDaoProvider = DoubleCheck.provider(new SwitchingProvider<ContactDao>(singletonCImpl, 14));
      this.provideUserDaoProvider = DoubleCheck.provider(new SwitchingProvider<UserDao>(singletonCImpl, 16));
      this.contactRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ContactRepositoryImpl>(singletonCImpl, 1));
      this.contactSyncWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<ContactSyncWorker_AssistedFactory>(singletonCImpl, 0));
      this.provideMediaDaoProvider = DoubleCheck.provider(new SwitchingProvider<MediaDao>(singletonCImpl, 18));
      this.mediaCleanupWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<MediaCleanupWorker_AssistedFactory>(singletonCImpl, 17));
      this.provideMessageApiProvider = DoubleCheck.provider(new SwitchingProvider<MessageApi>(singletonCImpl, 21));
      this.provideMessageDaoProvider = DoubleCheck.provider(new SwitchingProvider<MessageDao>(singletonCImpl, 22));
      this.provideChatDaoProvider = DoubleCheck.provider(new SwitchingProvider<ChatDao>(singletonCImpl, 23));
      this.webSocketManagerProvider = DoubleCheck.provider(new SwitchingProvider<WebSocketManager>(singletonCImpl, 24));
      this.messageRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<MessageRepositoryImpl>(singletonCImpl, 20));
    }

    @SuppressWarnings("unchecked")
    private void initialize2(final ApplicationContextModule applicationContextModuleParam) {
      this.pendingMessageWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<PendingMessageWorker_AssistedFactory>(singletonCImpl, 19));
      this.scheduledMessageWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<ScheduledMessageWorker_AssistedFactory>(singletonCImpl, 25));
      this.provideImageLoaderProvider = DoubleCheck.provider(new SwitchingProvider<ImageLoader>(singletonCImpl, 26));
      this.provideAppCoroutineScopeProvider = DoubleCheck.provider(new SwitchingProvider<CoroutineScope>(singletonCImpl, 27));
      this.globalErrorHandlerProvider = DoubleCheck.provider(new SwitchingProvider<GlobalErrorHandler>(singletonCImpl, 28));
      this.provideNotificationApiProvider = DoubleCheck.provider(new SwitchingProvider<NotificationApi>(singletonCImpl, 32));
      this.fCMTokenManagerProvider = DoubleCheck.provider(new SwitchingProvider<FCMTokenManager>(singletonCImpl, 31));
      this.provideDeviceTokenManagerProvider = DoubleCheck.provider(new SwitchingProvider<DeviceTokenManager>(singletonCImpl, 30));
      this.authRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 29);
      this.bindAuthRepositoryProvider = DoubleCheck.provider((Provider) authRepositoryImplProvider);
      this.provideChatParticipantDaoProvider = DoubleCheck.provider(new SwitchingProvider<ChatParticipantDao>(singletonCImpl, 35));
      this.typingStateHolderProvider = DoubleCheck.provider(new SwitchingProvider<TypingStateHolder>(singletonCImpl, 36));
      this.callServiceProvider = DoubleCheck.provider(new SwitchingProvider<CallService>(singletonCImpl, 37));
      this.wsEventRouterProvider = DoubleCheck.provider(new SwitchingProvider<WsEventRouter>(singletonCImpl, 34));
      this.provideChatApiProvider = DoubleCheck.provider(new SwitchingProvider<ChatApi>(singletonCImpl, 39));
      this.syncOnReconnectManagerProvider = DoubleCheck.provider(new SwitchingProvider<SyncOnReconnectManager>(singletonCImpl, 38));
      this.wsLifecycleManagerProvider = DoubleCheck.provider(new SwitchingProvider<WsLifecycleManager>(singletonCImpl, 33));
      this.themePreferencesStoreProvider = DoubleCheck.provider(new SwitchingProvider<ThemePreferencesStore>(singletonCImpl, 40));
      this.privacyPreferencesStoreProvider = DoubleCheck.provider(new SwitchingProvider<PrivacyPreferencesStore>(singletonCImpl, 41));
      this.chatRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ChatRepositoryImpl>(singletonCImpl, 42));
      this.userRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<UserRepositoryImpl>(singletonCImpl, 43));
      this.provideMediaApiProvider = DoubleCheck.provider(new SwitchingProvider<MediaApi>(singletonCImpl, 45));
      this.imageCompressorProvider = DoubleCheck.provider(new SwitchingProvider<ImageCompressor>(singletonCImpl, 46));
      this.videoCompressorProvider = DoubleCheck.provider(new SwitchingProvider<VideoCompressor>(singletonCImpl, 47));
      this.mediaRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<MediaRepositoryImpl>(singletonCImpl, 44));
    }

    @SuppressWarnings("unchecked")
    private void initialize3(final ApplicationContextModule applicationContextModuleParam) {
      this.voiceRecorderProvider = DoubleCheck.provider(new SwitchingProvider<VoiceRecorder>(singletonCImpl, 48));
      this.provideGroupDaoProvider = DoubleCheck.provider(new SwitchingProvider<GroupDao>(singletonCImpl, 49));
      this.mediaDownloadManagerProvider = DoubleCheck.provider(new SwitchingProvider<MediaDownloadManager>(singletonCImpl, 50));
      this.notificationPreferencesStoreProvider = DoubleCheck.provider(new SwitchingProvider<NotificationPreferencesStore>(singletonCImpl, 51));
      this.provideStatusApiProvider = DoubleCheck.provider(new SwitchingProvider<StatusApi>(singletonCImpl, 52));
      this.notificationBuilderProvider = DoubleCheck.provider(new SwitchingProvider<NotificationBuilder>(singletonCImpl, 53));
      this.activeChatTrackerProvider = DoubleCheck.provider(new SwitchingProvider<ActiveChatTracker>(singletonCImpl, 54));
      this.inAppNotificationManagerProvider = DoubleCheck.provider(new SwitchingProvider<InAppNotificationManager>(singletonCImpl, 55));
    }

    @Override
    public void injectWhatsAppApplication(WhatsAppApplication whatsAppApplication) {
      injectWhatsAppApplication2(whatsAppApplication);
    }

    @Override
    public void injectNotificationActionReceiver(NotificationActionReceiver arg0) {
      injectNotificationActionReceiver2(arg0);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private WhatsAppApplication injectWhatsAppApplication2(WhatsAppApplication instance) {
      WhatsAppApplication_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      WhatsAppApplication_MembersInjector.injectImageLoader(instance, provideImageLoaderProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private NotificationActionReceiver injectNotificationActionReceiver2(
        NotificationActionReceiver instance2) {
      NotificationActionReceiver_MembersInjector.injectAppScope(instance2, provideAppCoroutineScopeProvider.get());
      NotificationActionReceiver_MembersInjector.injectChatDao(instance2, provideChatDaoProvider.get());
      NotificationActionReceiver_MembersInjector.injectSendMessageUseCase(instance2, sendMessageUseCase());
      return instance2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.whatsappclone.feature.contacts.worker.ContactSyncWorker_AssistedFactory 
          return (T) new ContactSyncWorker_AssistedFactory() {
            @Override
            public ContactSyncWorker create(Context appContext, WorkerParameters workerParams) {
              return new ContactSyncWorker(appContext, workerParams, singletonCImpl.contactRepositoryImplProvider.get());
            }
          };

          case 1: // com.whatsappclone.feature.contacts.data.ContactRepositoryImpl 
          return (T) new ContactRepositoryImpl(singletonCImpl.provideContentResolverProvider.get(), singletonCImpl.provideUserApiProvider.get(), singletonCImpl.provideContactDaoProvider.get(), singletonCImpl.provideUserDaoProvider.get());

          case 2: // android.content.ContentResolver 
          return (T) ContactsModule_Companion_ProvideContentResolverFactory.provideContentResolver(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.whatsappclone.core.network.api.UserApi 
          return (T) NetworkModule_ProvideUserApiFactory.provideUserApi(singletonCImpl.provideRetrofitProvider.get());

          case 4: // retrofit2.Retrofit 
          return (T) NetworkModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get(), singletonCImpl.provideBaseUrlProvider.get());

          case 5: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient(singletonCImpl.provideAuthInterceptorProvider.get(), singletonCImpl.provideLoggingInterceptorProvider.get());

          case 6: // com.whatsappclone.core.network.interceptor.AuthInterceptor 
          return (T) NetworkModule_ProvideAuthInterceptorFactory.provideAuthInterceptor(singletonCImpl.provideTokenManagerProvider.get());

          case 7: // com.whatsappclone.core.network.token.TokenManager 
          return (T) NetworkModule_ProvideTokenManagerFactory.provideTokenManager(singletonCImpl.provideEncryptedSharedPreferencesProvider.get(), DoubleCheck.lazy(singletonCImpl.provideAuthApiProvider));

          case 8: // @javax.inject.Named("encrypted") android.content.SharedPreferences 
          return (T) NetworkModule_ProvideEncryptedSharedPreferencesFactory.provideEncryptedSharedPreferences(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 9: // com.whatsappclone.core.network.api.AuthApi 
          return (T) NetworkModule_ProvideAuthApiFactory.provideAuthApi(singletonCImpl.provideRetrofitProvider.get());

          case 10: // okhttp3.logging.HttpLoggingInterceptor 
          return (T) NetworkModule_ProvideLoggingInterceptorFactory.provideLoggingInterceptor();

          case 11: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 12: // com.whatsappclone.core.network.url.BaseUrlProvider 
          return (T) NetworkModule_ProvideBaseUrlProviderFactory.provideBaseUrlProvider(singletonCImpl.provideNetworkDataStoreProvider.get());

          case 13: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) NetworkModule_ProvideNetworkDataStoreFactory.provideNetworkDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 14: // com.whatsappclone.core.database.dao.ContactDao 
          return (T) DatabaseModule_ProvideContactDaoFactory.provideContactDao(singletonCImpl.provideAppDatabaseProvider.get());

          case 15: // com.whatsappclone.core.database.AppDatabase 
          return (T) DatabaseModule_ProvideAppDatabaseFactory.provideAppDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 16: // com.whatsappclone.core.database.dao.UserDao 
          return (T) DatabaseModule_ProvideUserDaoFactory.provideUserDao(singletonCImpl.provideAppDatabaseProvider.get());

          case 17: // com.whatsappclone.feature.media.worker.MediaCleanupWorker_AssistedFactory 
          return (T) new MediaCleanupWorker_AssistedFactory() {
            @Override
            public MediaCleanupWorker create(Context context, WorkerParameters workerParams2) {
              return new MediaCleanupWorker(context, workerParams2, singletonCImpl.provideMediaDaoProvider.get());
            }
          };

          case 18: // com.whatsappclone.core.database.dao.MediaDao 
          return (T) DatabaseModule_ProvideMediaDaoFactory.provideMediaDao(singletonCImpl.provideAppDatabaseProvider.get());

          case 19: // com.whatsappclone.feature.chat.worker.PendingMessageWorker_AssistedFactory 
          return (T) new PendingMessageWorker_AssistedFactory() {
            @Override
            public PendingMessageWorker create(Context context2, WorkerParameters workerParams3) {
              return new PendingMessageWorker(context2, workerParams3, singletonCImpl.messageRepositoryImplProvider.get());
            }
          };

          case 20: // com.whatsappclone.feature.chat.data.MessageRepositoryImpl 
          return (T) new MessageRepositoryImpl(singletonCImpl.provideMessageApiProvider.get(), singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.webSocketManagerProvider.get(), singletonCImpl.provideJsonProvider.get(), singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 21: // com.whatsappclone.core.network.api.MessageApi 
          return (T) NetworkModule_ProvideMessageApiFactory.provideMessageApi(singletonCImpl.provideRetrofitProvider.get());

          case 22: // com.whatsappclone.core.database.dao.MessageDao 
          return (T) DatabaseModule_ProvideMessageDaoFactory.provideMessageDao(singletonCImpl.provideAppDatabaseProvider.get());

          case 23: // com.whatsappclone.core.database.dao.ChatDao 
          return (T) DatabaseModule_ProvideChatDaoFactory.provideChatDao(singletonCImpl.provideAppDatabaseProvider.get());

          case 24: // com.whatsappclone.core.network.websocket.WebSocketManager 
          return (T) new WebSocketManager(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideTokenManagerProvider.get(), singletonCImpl.provideBaseUrlProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 25: // com.whatsappclone.feature.chat.worker.ScheduledMessageWorker_AssistedFactory 
          return (T) new ScheduledMessageWorker_AssistedFactory() {
            @Override
            public ScheduledMessageWorker create(Context appContext2, WorkerParameters params) {
              return new ScheduledMessageWorker(appContext2, params, singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.messageRepositoryImplProvider.get());
            }
          };

          case 26: // coil3.ImageLoader 
          return (T) AppModule_ProvideImageLoaderFactory.provideImageLoader(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideOkHttpClientProvider.get());

          case 27: // @javax.inject.Named("appScope") kotlinx.coroutines.CoroutineScope 
          return (T) AppModule_ProvideAppCoroutineScopeFactory.provideAppCoroutineScope();

          case 28: // com.whatsappclone.app.error.GlobalErrorHandler 
          return (T) new GlobalErrorHandler(singletonCImpl.provideTokenManagerProvider.get());

          case 29: // com.whatsappclone.feature.auth.data.AuthRepositoryImpl 
          return (T) new AuthRepositoryImpl(singletonCImpl.provideAuthApiProvider.get(), singletonCImpl.provideTokenManagerProvider.get(), singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideDeviceTokenManagerProvider.get(), singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 30: // com.whatsappclone.core.network.token.DeviceTokenManager 
          return (T) AppModule_ProvideDeviceTokenManagerFactory.provideDeviceTokenManager(singletonCImpl.fCMTokenManagerProvider.get());

          case 31: // com.whatsappclone.app.notification.FCMTokenManager 
          return (T) new FCMTokenManager(singletonCImpl.provideNotificationApiProvider.get(), singletonCImpl.provideTokenManagerProvider.get(), singletonCImpl.provideAppCoroutineScopeProvider.get());

          case 32: // com.whatsappclone.core.network.api.NotificationApi 
          return (T) NetworkModule_ProvideNotificationApiFactory.provideNotificationApi(singletonCImpl.provideRetrofitProvider.get());

          case 33: // com.whatsappclone.app.lifecycle.WsLifecycleManager 
          return (T) new WsLifecycleManager(singletonCImpl.webSocketManagerProvider.get(), singletonCImpl.wsEventRouterProvider.get(), singletonCImpl.syncOnReconnectManagerProvider.get(), singletonCImpl.provideTokenManagerProvider.get());

          case 34: // com.whatsappclone.app.data.websocket.WsEventRouter 
          return (T) new WsEventRouter(singletonCImpl.webSocketManagerProvider.get(), singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideChatParticipantDaoProvider.get(), singletonCImpl.typingStateHolderProvider.get(), singletonCImpl.callServiceProvider.get(), singletonCImpl.provideJsonProvider.get(), singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 35: // com.whatsappclone.core.database.dao.ChatParticipantDao 
          return (T) DatabaseModule_ProvideChatParticipantDaoFactory.provideChatParticipantDao(singletonCImpl.provideAppDatabaseProvider.get());

          case 36: // com.whatsappclone.core.network.websocket.TypingStateHolder 
          return (T) new TypingStateHolder();

          case 37: // com.whatsappclone.feature.chat.call.CallService 
          return (T) new CallService(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.webSocketManagerProvider.get());

          case 38: // com.whatsappclone.app.data.websocket.SyncOnReconnectManager 
          return (T) new SyncOnReconnectManager(singletonCImpl.webSocketManagerProvider.get(), singletonCImpl.provideChatApiProvider.get(), singletonCImpl.provideMessageApiProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideChatParticipantDaoProvider.get(), singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.provideNetworkDataStoreProvider.get());

          case 39: // com.whatsappclone.core.network.api.ChatApi 
          return (T) NetworkModule_ProvideChatApiFactory.provideChatApi(singletonCImpl.provideRetrofitProvider.get());

          case 40: // com.whatsappclone.feature.settings.data.ThemePreferencesStore 
          return (T) new ThemePreferencesStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 41: // com.whatsappclone.feature.settings.data.PrivacyPreferencesStore 
          return (T) new PrivacyPreferencesStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 42: // com.whatsappclone.feature.chat.data.ChatRepositoryImpl 
          return (T) new ChatRepositoryImpl(singletonCImpl.provideChatApiProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideChatParticipantDaoProvider.get(), singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideMessageDaoProvider.get(), singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 43: // com.whatsappclone.feature.chat.data.UserRepositoryImpl 
          return (T) new UserRepositoryImpl(singletonCImpl.provideUserApiProvider.get(), singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 44: // com.whatsappclone.feature.media.data.MediaRepositoryImpl 
          return (T) new MediaRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideMediaApiProvider.get(), singletonCImpl.provideMediaDaoProvider.get(), singletonCImpl.imageCompressorProvider.get(), singletonCImpl.videoCompressorProvider.get());

          case 45: // com.whatsappclone.core.network.api.MediaApi 
          return (T) NetworkModule_ProvideMediaApiFactory.provideMediaApi(singletonCImpl.provideRetrofitProvider.get());

          case 46: // com.whatsappclone.feature.media.util.ImageCompressor 
          return (T) new ImageCompressor(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 47: // com.whatsappclone.feature.media.util.VideoCompressor 
          return (T) new VideoCompressor(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 48: // com.whatsappclone.feature.media.audio.VoiceRecorder 
          return (T) new VoiceRecorder(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 49: // com.whatsappclone.core.database.dao.GroupDao 
          return (T) DatabaseModule_ProvideGroupDaoFactory.provideGroupDao(singletonCImpl.provideAppDatabaseProvider.get());

          case 50: // com.whatsappclone.feature.media.util.MediaDownloadManager 
          return (T) new MediaDownloadManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideMediaDaoProvider.get());

          case 51: // com.whatsappclone.feature.settings.data.NotificationPreferencesStore 
          return (T) new NotificationPreferencesStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 52: // com.whatsappclone.core.network.api.StatusApi 
          return (T) NetworkModule_ProvideStatusApiFactory.provideStatusApi(singletonCImpl.provideRetrofitProvider.get());

          case 53: // com.whatsappclone.app.notification.NotificationBuilder 
          return (T) new NotificationBuilder(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideImageLoaderProvider.get());

          case 54: // com.whatsappclone.app.notification.ActiveChatTracker 
          return (T) new ActiveChatTracker();

          case 55: // com.whatsappclone.app.notification.InAppNotificationManager 
          return (T) new InAppNotificationManager();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
