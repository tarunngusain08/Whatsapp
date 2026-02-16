package com.whatsappclone.core.network.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.whatsappclone.core.network.url.BaseUrlProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NetworkModule_ProvideBaseUrlProviderFactory implements Factory<BaseUrlProvider> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public NetworkModule_ProvideBaseUrlProviderFactory(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public BaseUrlProvider get() {
    return provideBaseUrlProvider(dataStoreProvider.get());
  }

  public static NetworkModule_ProvideBaseUrlProviderFactory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new NetworkModule_ProvideBaseUrlProviderFactory(dataStoreProvider);
  }

  public static BaseUrlProvider provideBaseUrlProvider(DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideBaseUrlProvider(dataStore));
  }
}
