# Hilt/Dagger rules
-keepattributes *Annotation*
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ComponentManager { *; }

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep @retrofit2.http.* interface * { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * extends kotlinx.serialization.internal.AbstractPolymorphicSerializer { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Apollo GraphQL
-keep class com.apollographql.apollo.** { *; }
-keep class in.aboobacker.labdroid.** { *; }

# Navigation
-keep class androidx.navigation.** { *; }

# Coil
-keep class coil.** { *; }

# AppAuth
-keep class net.openid.appauth.** { *; }
