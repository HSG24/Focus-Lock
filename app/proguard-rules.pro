# Focus Lock R8 / ProGuard Keep Rules

# Preserve generic signatures, inner classes, enclosing methods, and runtime annotations for reflection/serialization
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable
-keepattributes *Annotation*

# Preserve Firebase SDKs (Realtime Database / AI / App Check / Core / Install)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepnames class com.fasterxml.jackson.** { *; }

# Preserve all Focus Lock data classes, GitHub updater models, share models, settings, and session records
-keep class com.example.BuildConfig { *; }
-keep class com.example.Session { *; }
-keep class com.example.LockService { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# Preserve Kotlin reflection/metadata if used by serialization/Moshi
-keep class kotlin.Metadata { *; }

# Gson @SerializedName keep rule
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# kotlinx.serialization keep rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# Moshi keep rules
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @interface com.squareup.moshi.JsonClass
-keepclassmembers @com.squareup.moshi.JsonClass class * { *; }

# org.json keep rules
-keep class org.json.** { *; }
-dontwarn org.json.**

# Retrofit & OkHttp network metadata
-keepattributes MethodParameters
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
