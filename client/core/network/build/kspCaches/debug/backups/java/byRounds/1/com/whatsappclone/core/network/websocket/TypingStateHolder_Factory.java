package com.whatsappclone.core.network.websocket;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class TypingStateHolder_Factory implements Factory<TypingStateHolder> {
  @Override
  public TypingStateHolder get() {
    return newInstance();
  }

  public static TypingStateHolder_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TypingStateHolder newInstance() {
    return new TypingStateHolder();
  }

  private static final class InstanceHolder {
    private static final TypingStateHolder_Factory INSTANCE = new TypingStateHolder_Factory();
  }
}
