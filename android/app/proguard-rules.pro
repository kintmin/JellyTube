# 제네릭/애노테이션 메타데이터 보존 (kotlinx.serialization 제네릭 직렬화에 필요)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# ML Kit·Firebase 는 ComponentRegistrar 를 매니페스트 meta-data 의 이름으로 reflection 생성한다.
# firebase-components 16.1.0 이 주는 `-keep class * implements ComponentRegistrar` 는 R8 full mode
# 에서 기본 생성자를 지키지 않아, 생성이 조용히 실패하고 ML Kit 컴포넌트가 하나도 등록되지 않는다.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    void <init>();
}

# JDK 호환
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
