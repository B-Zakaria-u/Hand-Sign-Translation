# Keep TFLite classes
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }

# Keep MediaPipe
-keep class com.google.mediapipe.** { *; }

# Keep ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Keep PyTorch Mobile
-keep class org.pytorch.** { *; }

# Keep Room entities
-keep class com.handsign.poc.data.db.entity.** { *; }

# Keep Gson model config
-keep class com.handsign.poc.data.model.** { *; }
-keepattributes Signature, *Annotation*

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.** { *; }
