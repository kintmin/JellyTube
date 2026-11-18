package com.kintmin.buildSrc

import java.io.File

/**
 * 딥링크 스킴·호스트의 진원지는 `shared:presentation` 의 `DeepLinkConstants` 다.
 *
 * Android Manifest 와 iOS `Info.plist` 는 Kotlin 상수를 참조할 수 없다. 그래서 빌드가 여기서
 * 값을 읽어 Manifest 에 주입하고(`manifestPlaceholders`), plist 는 대조해 어긋나면 실패시킨다.
 * 값을 복제해 두면 한쪽만 바뀌어도 딥링크가 조용히 죽는다.
 */
object DeepLinkConstantsSource {

    private const val RELATIVE_PATH =
        "shared/presentation/src/commonMain/kotlin/com/kintmin/presentation/deeplink/DeepLinkConstants.kt"

    fun file(rootDir: File): File = File(rootDir, RELATIVE_PATH)

    fun read(rootDir: File, name: String): String {
        val source = file(rootDir)
        val value = Regex("""const\s+val\s+$name\s*=\s*"([^"]*)"""")
            .find(source.readText())
            ?.groupValues
            ?.get(1)
        return requireNotNull(value) {
            "DeepLinkConstants.$name 을 ${source.path} 에서 읽지 못했다. 상수 선언 형태가 바뀌었는지 확인한다."
        }
    }
}
