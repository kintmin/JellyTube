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

# 테마는 난독화 제외
-keep class com.kintmin.presentation.theme.ThemeKt { *; }

# 네비게이션 관련 클래스 난독화 제외
-keep class **NavigationKt { *; }

# ViewModels 보존
-keep class ** extends androidx.lifecycle.ViewModel { *; }
-keep class **_HiltModules$KeyModule { *; }

# JDK 호환
-dontwarn java.lang.invoke.StringConcatFactory
