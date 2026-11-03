# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase { abstract *; }

# MediaPipe
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Firebase / Firestore
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Gson (used for data serialization)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# App data models (prevent field name obfuscation for Room and Firestore)
-keep class com.jsf.app.pravin.data.model.** { *; }
-keep class com.jsf.app.pravin.data.remote.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Coil
-dontwarn coil.**

# Keep Compose preview annotations
-dontwarn androidx.compose.ui.tooling.**
