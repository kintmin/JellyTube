package com.kintmin.presentation.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class LaunchBackgroundColorTest {

    // 호스트 테스트는 모듈 디렉터리에서 돈다.
    private val root = File("../..")
    private val expected = hex(DarkColorScheme.background.toArgb())

    @Test
    fun `Android 스플래시 배경은 앱 배경과 같다`() {
        val colors = File(root, "android/app/src/main/res/values/colors.xml").readText()
        val actual = Regex("""<color name="app_background">#([0-9A-Fa-f]{8})</color>""").find(colors)
            ?.groupValues?.get(1)?.uppercase()

        assertEquals(expected, actual)
    }

    @Test
    fun `iOS 런치 화면 배경은 앱 배경과 같다`() {
        val json = File(root, "ios/app/JellyTubeIos/Assets.xcassets/LaunchBackground.colorset/Contents.json").readText()
        fun channel(name: String): Int =
            Regex(""""$name"\s*:\s*"0x([0-9A-Fa-f]{2})"""").find(json)?.groupValues?.get(1)?.toInt(16)
                ?: error("LaunchBackground 의 $name 성분을 찾지 못했습니다")
        val alpha = Regex(""""alpha"\s*:\s*"([0-9.]+)"""").find(json)?.groupValues?.get(1)?.toDouble()
            ?: error("LaunchBackground 의 alpha 성분을 찾지 못했습니다")
        val argb = ((alpha * 255).roundToInt() shl 24) or
            (channel("red") shl 16) or (channel("green") shl 8) or channel("blue")

        assertEquals(expected, hex(argb))
    }

    private fun hex(argb: Int) = "%08X".format(argb)
}
