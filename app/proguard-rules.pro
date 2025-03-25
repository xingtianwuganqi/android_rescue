# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 保留类名，避免混淆
-keep class com.example.myapp.** { *; }

# 保留所有枚举类
-keepclassmembers enum * { *; }

# 保留使用 Gson 反序列化的类
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# 保留 Room 相关类
-keep class androidx.room.** { *; }

# 保留 Retrofit 相关类
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# 解决 R8 移除 Parcelable 的问题
-keep class * implements android.os.Parcelable { *; }

# 保留带注解的类
-keepattributes *Annotation*

# 保护混淆后的日志，防止泄露类信息
-assumenosideeffects class android.util.Log { public static *** v(...); }
