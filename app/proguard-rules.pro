# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# 保持类名不被混淆
-keepattributes SourceFile,LineNumberTable

# 保持内部类
-keepattributes *InnerClass
-keepattributes *EnclosingMethod

# 保持注解
-keepattributes *Annotation*

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Material Components
-keep class com.google.android.material.** { *; }

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# Application类
-keep class com.gufeng.adhelper.AdHelperApplication { *; }

# 服务类
-keep class com.gufeng.adhelper.service.** { *; }

# 广播接收器
-keep class com.gufeng.adhelper.receiver.** { *; }

# 检测器类
-keep class com.gufeng.adhelper.detector.** { *; }

# 工具类
-keep class com.gufeng.adhelper.utils.** { *; }

# ViewModel
-keep class com.gufeng.adhelper.viewmodel.** { *; }

# 界面类
-keep class com.gufeng.adhelper.ui.** { *; }

# 保活相关
-keep class com.gufeng.adhelper.keepalive.** { *; }

# SharedPreferences
-keep class * extends android.content.SharedPreferences {
    public <methods>;
}

# 通知
-keep class * extends android.app.Notification {
    <fields>;
}
