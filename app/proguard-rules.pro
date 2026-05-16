# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified in
# /path/to/android-sdk/tools/proguard/proguard-android.txt

# Keep TMDB API data classes
-keep class com.cinefilx.app.data.remote.dto.** { *; }
-keep class com.cinefilx.app.domain.model.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coil
-dontwarn coil.**
