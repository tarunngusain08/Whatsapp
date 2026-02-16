package com.whatsappclone.core.network.di;

import android.content.Context;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class NetworkModule_ProvideNetworkDataStoreFactory implements Factory<DataStore<Preferences>> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvideNetworkDataStoreFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DataStore<Preferences> get() {
    return provideNetworkDataStore(contextProvider.get());
  }

  public static NetworkModule_ProvideNetworkDataStoreFactory create(
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvideNetworkDataStoreFactory(contextProvider);
  }

  public static DataStore<Preferences> provideNetworkDataStore(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideNetworkDataStore(context));
  }
}
