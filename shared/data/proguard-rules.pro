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

# R & Manifest 보존
-keep class **.R
-keep class **.R$* {*;}

# JDK 호환
-dontwarn java.lang.invoke.StringConcatFactory
