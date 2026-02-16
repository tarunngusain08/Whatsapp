package com.whatsappclone.app.notification;

import android.content.Context;
import coil3.ImageLoader;
import com.whatsappclone.core.database.dao.ChatDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NotificationBuilder_Factory implements Factory<NotificationBuilder> {
  private final Provider<Context> contextProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<ImageLoader> imageLoaderProvider;

  public NotificationBuilder_Factory(Provider<Context> contextProvider,
      Provider<ChatDao> chatDaoProvider, Provider<ImageLoader> imageLoaderProvider) {
    this.contextProvider = contextProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.imageLoaderProvider = imageLoaderProvider;
  }

  @Override
  public NotificationBuilder get() {
    return newInstance(contextProvider.get(), chatDaoProvider.get(), imageLoaderProvider.get());
  }

  public static NotificationBuilder_Factory create(Provider<Context> contextProvider,
      Provider<ChatDao> chatDaoProvider, Provider<ImageLoader> imageLoaderProvider) {
    return new NotificationBuilder_Factory(contextProvider, chatDaoProvider, imageLoaderProvider);
  }

  public static NotificationBuilder newInstance(Context context, ChatDao chatDao,
      ImageLoader imageLoader) {
    return new NotificationBuilder(context, chatDao, imageLoader);
  }
}
