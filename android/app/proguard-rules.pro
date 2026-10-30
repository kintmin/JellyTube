# 제네릭/애노테이션 메타데이터 보존 (kotlinx.serialization 제네릭 직렬화에 필요)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# JDK 호환
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
