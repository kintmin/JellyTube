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

# Parcelable 객체 보존
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# R & Manifest 보존
-keep class **.R
-keep class **.R$* {*;}

# hilt 주입 문제가 많아서 통으로 난독화 제외
-keep class com.kintmin.platform.** { *; }

# JDK 호환
-dontwarn java.lang.invoke.StringConcatFactory
