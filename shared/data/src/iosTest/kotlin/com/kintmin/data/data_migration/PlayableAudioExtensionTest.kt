package com.kintmin.data.data_migration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayableAudioExtensionTest {

    @Test
    fun `AVAudioFile 이 여는 컨테이너만 재생 가능하다`() {
        listOf("m4a", "M4A", "mp4", "mp3", "aac", "flac", "wav", "aiff", "caf").forEach {
            assertTrue(isPlayableAudioExtension(it), it)
        }
        listOf("webm", "ogg", "opus", "wma", "amr", "").forEach {
            assertFalse(isPlayableAudioExtension(it), it)
        }
    }
}
