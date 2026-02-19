package com.whatsappclone.app;

import androidx.hilt.work.HiltWrapper_WorkerFactoryModule;
import com.whatsappclone.app.di.AppModule;
import com.whatsappclone.app.notification.NotificationActionReceiver_GeneratedInjector;
import com.whatsappclone.app.notification.WhatsAppFCMService_GeneratedInjector;
import com.whatsappclone.core.database.di.DatabaseModule;
import com.whatsappclone.core.network.di.NetworkModule;
import com.whatsappclone.feature.auth.di.AuthModule;
import com.whatsappclone.feature.auth.ui.login.LoginViewModel_HiltModules;
import com.whatsappclone.feature.auth.ui.otp.OtpViewModel_HiltModules;
import com.whatsappclone.feature.auth.ui.profile.ProfileSetupViewModel_HiltModules;
import com.whatsappclone.feature.chat.di.ChatModule;
import com.whatsappclone.feature.chat.ui.archived.ArchivedChatsViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.chatdetail.ChatDetailViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.chatlist.ChatListViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.forward.ForwardPickerViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.receipts.ReceiptDetailsViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.search.GlobalSearchViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.starred.StarredMessagesViewModel_HiltModules;
import com.whatsappclone.feature.chat.ui.status.StatusViewModel_HiltModules;
import com.whatsappclone.feature.chat.worker.PendingMessageWorker_HiltModule;
import com.whatsappclone.feature.chat.worker.ScheduledMessageWorker_HiltModule;
import com.whatsappclone.feature.contacts.di.ContactsModule;
import com.whatsappclone.feature.contacts.ui.BlockedContactsViewModel_HiltModules;
import com.whatsappclone.feature.contacts.ui.ContactInfoViewModel_HiltModules;
import com.whatsappclone.feature.contacts.ui.ContactPickerViewModel_HiltModules;
import com.whatsappclone.feature.contacts.worker.ContactSyncWorker_HiltModule;
import com.whatsappclone.feature.group.di.GroupModule;
import com.whatsappclone.feature.group.ui.info.GroupInfoViewModel_HiltModules;
import com.whatsappclone.feature.group.ui.newgroup.NewGroupViewModel_HiltModules;
import com.whatsappclone.feature.media.di.MediaModule;
import com.whatsappclone.feature.media.ui.MediaViewerViewModel_HiltModules;
import com.whatsappclone.feature.media.ui.SharedMediaViewModel_HiltModules;
import com.whatsappclone.feature.media.worker.MediaCleanupWorker_HiltModule;
import com.whatsappclone.feature.profile.di.ProfileModule;
import com.whatsappclone.feature.profile.ui.ProfileEditViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.NotificationSettingsViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.PrivacySettingsViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.SettingsViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.StorageUsageViewModel_HiltModules;
import com.whatsappclone.feature.settings.ui.ThemeSettingsViewModel_HiltModules;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.Subcomponent;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.android.components.ViewComponent;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.components.ViewWithFragmentComponent;
import dagger.hilt.android.flags.FragmentGetContextFix;
import dagger.hilt.android.flags.HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories_ActivityModule;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ViewModelModule;
import dagger.hilt.android.internal.managers.ActivityComponentManager;
import dagger.hilt.android.internal.managers.FragmentComponentManager;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_LifecycleModule;
import dagger.hilt.android.internal.managers.HiltWrapper_SavedStateHandleModule;
import dagger.hilt.android.internal.managers.ServiceComponentManager;
import dagger.hilt.android.internal.managers.ViewComponentManager;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.HiltWrapper_ActivityModule;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import dagger.hilt.android.scopes.ActivityScoped;
import dagger.hilt.android.scopes.FragmentScoped;
import dagger.hilt.android.scopes.ServiceScoped;
import dagger.hilt.android.scopes.ViewModelScoped;
import dagger.hilt.android.scopes.ViewScoped;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedComponent;
import dagger.hilt.migration.DisableInstallInCheck;
import javax.annotation.processing.Generated;
import javax.inject.Singleton;

@Generated("dagger.hilt.processor.internal.root.RootProcessor")
public final class WhatsAppApplication_HiltComponents {
  private WhatsAppApplication_HiltComponents() {
  }

  @Module(
      subcomponents = ServiceC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ServiceCBuilderModule {
    @Binds
    ServiceComponentBuilder bind(ServiceC.Builder builder);
  }

  @Module(
      subcomponents = ActivityRetainedC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityRetainedCBuilderModule {
    @Binds
    ActivityRetainedComponentBuilder bind(ActivityRetainedC.Builder builder);
  }

  @Module(
      subcomponents = ActivityC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityCBuilderModule {
    @Binds
    ActivityComponentBuilder bind(ActivityC.Builder builder);
  }

  @Module(
      subcomponents = ViewModelC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewModelCBuilderModule {
    @Binds
    ViewModelComponentBuilder bind(ViewModelC.Builder builder);
  }

  @Module(
      subcomponents = ViewC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewCBuilderModule {
    @Binds
    ViewComponentBuilder bind(ViewC.Builder builder);
  }

  @Module(
      subcomponents = FragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface FragmentCBuilderModule {
    @Binds
    FragmentComponentBuilder bind(FragmentC.Builder builder);
  }

  @Module(
      subcomponents = ViewWithFragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewWithFragmentCBuilderModule {
    @Binds
    ViewWithFragmentComponentBuilder bind(ViewWithFragmentC.Builder builder);
  }

  @Component(
      modules = {
          AppModule.class,
          ApplicationContextModule.class,
          AuthModule.class,
          ChatModule.class,
          ContactSyncWorker_HiltModule.class,
          ContactsModule.class,
          DatabaseModule.class,
          GroupModule.class,
          HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule.class,
          HiltWrapper_WorkerFactoryModule.class,
          MediaCleanupWorker_HiltModule.class,
          MediaModule.class,
          NetworkModule.class,
          PendingMessageWorker_HiltModule.class,
          ProfileModule.class,
          ScheduledMessageWorker_HiltModule.class,
          ActivityRetainedCBuilderModule.class,
          ServiceCBuilderModule.class
      }
  )
  @Singleton
  public abstract static class SingletonC implements WhatsAppApplication_GeneratedInjector,
      NotificationActionReceiver_GeneratedInjector,
      FragmentGetContextFix.FragmentGetContextFixEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint,
      ServiceComponentManager.ServiceComponentBuilderEntryPoint,
      SingletonComponent,
      GeneratedComponent {
  }

  @Subcomponent
  @ServiceScoped
  public abstract static class ServiceC implements WhatsAppFCMService_GeneratedInjector,
      ServiceComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ServiceComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          ArchivedChatsViewModel_HiltModules.KeyModule.class,
          BlockedContactsViewModel_HiltModules.KeyModule.class,
          ChatDetailViewModel_HiltModules.KeyModule.class,
          ChatListViewModel_HiltModules.KeyModule.class,
          ContactInfoViewModel_HiltModules.KeyModule.class,
          ContactPickerViewModel_HiltModules.KeyModule.class,
          ForwardPickerViewModel_HiltModules.KeyModule.class,
          GlobalSearchViewModel_HiltModules.KeyModule.class,
          GroupInfoViewModel_HiltModules.KeyModule.class,
          HiltWrapper_ActivityRetainedComponentManager_LifecycleModule.class,
          HiltWrapper_SavedStateHandleModule.class,
          LoginViewModel_HiltModules.KeyModule.class,
          MediaViewerViewModel_HiltModules.KeyModule.class,
          NewGroupViewModel_HiltModules.KeyModule.class,
          NotificationSettingsViewModel_HiltModules.KeyModule.class,
          OtpViewModel_HiltModules.KeyModule.class,
          PrivacySettingsViewModel_HiltModules.KeyModule.class,
          ProfileEditViewModel_HiltModules.KeyModule.class,
          ProfileSetupViewModel_HiltModules.KeyModule.class,
          ReceiptDetailsViewModel_HiltModules.KeyModule.class,
          SettingsViewModel_HiltModules.KeyModule.class,
          SharedMediaViewModel_HiltModules.KeyModule.class,
          StarredMessagesViewModel_HiltModules.KeyModule.class,
          StatusViewModel_HiltModules.KeyModule.class,
          StorageUsageViewModel_HiltModules.KeyModule.class,
          ThemeSettingsViewModel_HiltModules.KeyModule.class,
          ActivityCBuilderModule.class,
          ViewModelCBuilderModule.class
      }
  )
  @ActivityRetainedScoped
  public abstract static class ActivityRetainedC implements ActivityRetainedComponent,
      ActivityComponentManager.ActivityComponentBuilderEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityRetainedComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          HiltWrapper_ActivityModule.class,
          HiltWrapper_DefaultViewModelFactories_ActivityModule.class,
          FragmentCBuilderModule.class,
          ViewCBuilderModule.class
      }
  )
  @ActivityScoped
  public abstract static class ActivityC implements MainActivity_GeneratedInjector,
      ActivityComponent,
      DefaultViewModelFactories.ActivityEntryPoint,
      HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint,
      FragmentComponentManager.FragmentComponentBuilderEntryPoint,
      ViewComponentManager.ViewComponentBuilderEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          ArchivedChatsViewModel_HiltModules.BindsModule.class,
          BlockedContactsViewModel_HiltModules.BindsModule.class,
          ChatDetailViewModel_HiltModules.BindsModule.class,
          ChatListViewModel_HiltModules.BindsModule.class,
          ContactInfoViewModel_HiltModules.BindsModule.class,
          ContactPickerViewModel_HiltModules.BindsModule.class,
          ForwardPickerViewModel_HiltModules.BindsModule.class,
          GlobalSearchViewModel_HiltModules.BindsModule.class,
          GroupInfoViewModel_HiltModules.BindsModule.class,
          HiltWrapper_HiltViewModelFactory_ViewModelModule.class,
          LoginViewModel_HiltModules.BindsModule.class,
          MediaViewerViewModel_HiltModules.BindsModule.class,
          NewGroupViewModel_HiltModules.BindsModule.class,
          NotificationSettingsViewModel_HiltModules.BindsModule.class,
          OtpViewModel_HiltModules.BindsModule.class,
          PrivacySettingsViewModel_HiltModules.BindsModule.class,
          ProfileEditViewModel_HiltModules.BindsModule.class,
          ProfileSetupViewModel_HiltModules.BindsModule.class,
          ReceiptDetailsViewModel_HiltModules.BindsModule.class,
          SettingsViewModel_HiltModules.BindsModule.class,
          SharedMediaViewModel_HiltModules.BindsModule.class,
          StarredMessagesViewModel_HiltModules.BindsModule.class,
          StatusViewModel_HiltModules.BindsModule.class,
          StorageUsageViewModel_HiltModules.BindsModule.class,
          ThemeSettingsViewModel_HiltModules.BindsModule.class
      }
  )
  @ViewModelScoped
  public abstract static class ViewModelC implements ViewModelComponent,
      HiltViewModelFactory.ViewModelFactoriesEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewModelComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewC implements ViewComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewComponentBuilder {
    }
  }

  @Subcomponent(
      modules = ViewWithFragmentCBuilderModule.class
  )
  @FragmentScoped
  public abstract static class FragmentC implements FragmentComponent,
      DefaultViewModelFactories.FragmentEntryPoint,
      ViewComponentManager.ViewWithFragmentComponentBuilderEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends FragmentComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewWithFragmentC implements ViewWithFragmentComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewWithFragmentComponentBuilder {
    }
  }
}
