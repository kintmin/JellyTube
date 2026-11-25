package com.kintmin.presentation.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkTintTest {

    // 색이 어두워 보정 없이는 묻히는 대표색들. 두 번째는 앱이 쓰는 fallback 대표색이다.
    private val darkSeeds = listOf(
        Color(0xFF101010),
        Color(0xFF4B453B),
        Color(0xFF1A224C),
        Color(0xFF5A0607),
    )

    @Test
    fun `어두운 대표색도 보정하면 대비 바닥값을 넘는다`() {
        darkSeeds.forEach { seed ->
            val ratio = contrastRatio(seed.liftedForAccent(), accentContrastSurface)
            assertTrue("$seed 의 보정 결과가 $ratio:1 로 모자랍니다", ratio >= 3f)
        }
    }

    @Test
    fun `보정 전에는 바닥값에 못 미친다`() {
        darkSeeds.forEach { seed ->
            val ratio = contrastRatio(seed, accentContrastSurface)
            assertTrue("$seed 는 보정 없이도 $ratio:1 이라 이 테스트의 전제가 틀렸습니다", ratio < 3f)
        }
    }

    @Test
    fun `이미 밝은 색은 건드리지 않는다`() {
        val bright = Color(0xFFACE5EE)
        assertTrue(contrastRatio(bright, accentContrastSurface) >= 3f)
        assertEquals(bright, bright.liftedForAccent())
    }

    @Test
    fun `대표색을 못 읽었을 때 쓰는 색도 바닥값을 넘는다`() {
        val ratio = contrastRatio(artworkAccentFallback, accentContrastSurface)
        assertTrue("$artworkAccentFallback 가 $ratio:1 로 모자랍니다", ratio >= 3f)
    }

    @Test
    fun `보정은 어느 채널도 낮추지 않는다`() {
        darkSeeds.forEach { seed ->
            val lifted = seed.liftedForAccent()
            assertTrue("$seed 의 red 가 낮아졌습니다", lifted.red >= seed.red)
            assertTrue("$seed 의 green 이 낮아졌습니다", lifted.green >= seed.green)
            assertTrue("$seed 의 blue 가 낮아졌습니다", lifted.blue >= seed.blue)
        }
    }
}
