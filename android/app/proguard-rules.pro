# EchoMind ProGuard Rules

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep EchoMind data models (serialized via kotlinx.serialization)
-keep class com.echomind.app.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep API layer DTOs (SupabaseApi / NotionApi, serialized via kotlinx.serialization)
-keep class com.echomind.app.data.api.** { *; }

# Sherpa-ONNX offline voice recognition
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class com.k2fsa.sherpa.onnx.** { *; }

