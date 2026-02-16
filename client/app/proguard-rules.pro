# ============================================================================
# ProGuard / R8 Rules for WhatsApp Clone
# ============================================================================

# ----------------------------------------------------------------------------
# General Android rules
# ----------------------------------------------------------------------------
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep the application class
-keep class com.whatsappclone.app.** { *; }

# ----------------------------------------------------------------------------
# Kotlinx Serialization
# ----------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep serializers
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `@Serializable` classes and their generated serializers
-keep,includedescriptorclasses class com.whatsappclone.**$$serializer { *; }
-keepclassmembers class com.whatsappclone.** {
    *** Companion;
}
-keepclasseswithmembers class com.whatsappclone.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all classes annotated with @Serializable
-if @kotlinx.serialization.Serializable class **
-keep class <1> { *; }
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----------------------------------------------------------------------------
# Retrofit
# ----------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep Retrofit service methods
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ----------------------------------------------------------------------------
# OkHttp
# ----------------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ----------------------------------------------------------------------------
# Room
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room DAOs
-keep interface * extends androidx.room.** { *; }
-keep class * implements androidx.room.** { *; }

# Keep all Room-generated classes
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# ----------------------------------------------------------------------------
# Hilt / Dagger
# ----------------------------------------------------------------------------
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Hilt generated components
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# ----------------------------------------------------------------------------
# Coroutines
# ----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ----------------------------------------------------------------------------
# Compose
# ----------------------------------------------------------------------------
-dontwarn androidx.compose.**

# Keep Compose runtime stability
-keep class androidx.compose.runtime.** { *; }

# ----------------------------------------------------------------------------
# Coil
# ----------------------------------------------------------------------------
-keep class coil3.** { *; }
-dontwarn coil3.**

# ----------------------------------------------------------------------------
# Firebase
# ----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ----------------------------------------------------------------------------
# Enum classes
# ----------------------------------------------------------------------------
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------
# Parcelable
# ----------------------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ----------------------------------------------------------------------------
# R8 compatibility
# ----------------------------------------------------------------------------
-dontwarn java.lang.invoke.StringConcatFactory
