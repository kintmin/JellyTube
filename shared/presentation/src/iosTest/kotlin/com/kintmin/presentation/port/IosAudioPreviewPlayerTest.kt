package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditPart
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

class IosAudioPreviewPlayerTest {

    @Test
    fun `원본을 열지 못하면 재생하지 않고 넘어간다`() {
        val player = IosAudioPreviewPlayer()

        player.play(
            audioFileFullPath = NSTemporaryDirectory() + NSUUID().UUIDString + ".m4a",
            parts = listOf(AudioEditPart.Audio(startMs = 0, endMs = 1_000)),
            startAtMs = 0,
        )

        assertFalse(player.isPlaying)
        assertNull(player.currentPositionMs)
    }
}
