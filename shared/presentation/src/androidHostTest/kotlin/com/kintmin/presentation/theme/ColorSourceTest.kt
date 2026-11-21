package com.kintmin.presentation.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ColorSourceTest {

    // 호스트 테스트는 모듈 디렉터리에서 돈다.
    private val commonMain = File("src/commonMain/kotlin/com/kintmin/presentation")

    // Transparent·Unspecified 는 색이 아니라 "칠하지 않음" 이라 허용한다.
    private val literal = Regex("""\bColor\((0x|\d)|\bColor\.(White|Black|Red|Green|Blue|Gray|DarkGray|LightGray|Yellow|Cyan|Magenta)\b""")

    @Test
    fun `theme 밖에서는 색 값을 직접 쓰지 않는다`() {
        assertTrue("소스 경로를 찾지 못했습니다: ${commonMain.absolutePath}", commonMain.isDirectory)

        val violations = commonMain.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.parentFile.name != "theme" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    "${file.relativeTo(commonMain)}:${index + 1}: ${line.trim()}".takeIf { literal.containsMatchIn(line) }
                }
            }
            .toList()

        assertTrue(violations.joinToString("\n", prefix = "theme 의 토큰을 쓰세요:\n"), violations.isEmpty())
    }
}
