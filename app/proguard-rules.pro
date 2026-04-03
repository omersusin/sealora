# Sealora ProGuard Rules

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.omersusin.sealora.**$$serializer { *; }
-keepclassmembers class com.omersusin.sealora.** {
    *** Companion;
}
-keepclasseswithmembers class com.omersusin.sealora.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# SLF4J - Ktor logging dependency
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Domain models
-keep class com.omersusin.sealora.domain.model.** { *; }
-keep class com.omersusin.sealora.data.local.entity.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
