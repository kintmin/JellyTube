# 메타데이터 난독화 제외
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# 모든 네이티브 함수와 클래스는 난독화 제외
-keepclasseswithmembernames class * {
    native <methods>;
}

# kotlinx.serialization 보존
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Parcelable 객체 보존
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# R & Manifest 보존
-keep class **.R
-keep class **.R$* {*;}

# Jetpack Compose 관련 클래스 보존
-keep class androidx.compose.** { *; }

# Application을 상속받은 Public 클래스는 난독화 제외
-keep public class * extends android.app.Application

# Activity를 상속받은 Public 클래스는 난독화 제외
-keep public class * extends android.app.Activity

# JDK 호환
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean