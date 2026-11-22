package com.kintmin.presentation.ui.player_detail.artwork

import androidx.compose.animation.core.Transition
import com.kintmin.presentation.theme.artworkBackdropFallback
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerArtworkTest {

    @Test
    fun `첫 값은 바로 붙인다`() {
        assertTrue(segment(null, artwork("a.jpg")).isInstant())
    }

    @Test
    fun `같은 곡을 다시 읽은 값은 바로 붙인다`() {
        assertTrue(segment(artwork("a.jpg"), artwork("a.jpg")).isInstant())
    }

    @Test
    fun `곡이 바뀌면 전환한다`() {
        assertFalse(segment(artwork("a.jpg"), artwork("b.jpg")).isInstant())
        assertFalse(segment(artwork("a.jpg"), artwork(null)).isInstant())
    }

    private fun artwork(path: String?) = PlayerArtwork(path, null, artworkBackdropFallback)

    private fun segment(initial: PlayerArtwork?, target: PlayerArtwork?) =
        object : Transition.Segment<PlayerArtwork?> {
            override val initialState = initial
            override val targetState = target
        }
}
