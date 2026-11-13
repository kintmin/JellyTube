package com.kintmin.domain.audio_media

import com.kintmin.domain.audio_media.model.InvalidUploadedFileNameException
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateUploadedAudioFileNameUseCaseTest {

    private val useCase = ValidateUploadedAudioFileNameUseCase()

    private fun accepted(rawFileName: String?): String = useCase(rawFileName).getOrThrow()

    private fun assertRejected(rawFileName: String?) {
        assertTrue("$rawFileName 는 거부돼야 한다", useCase(rawFileName).exceptionOrNull() is InvalidUploadedFileNameException)
    }

    @Test
    fun `확장자는 대소문자까지 그대로 둔다`() {
        assertEquals("song.mp3", accepted("song.mp3"))
        assertEquals("song.FLAC", accepted("song.FLAC"))
        assertEquals("song.my_ext", accepted("song.my_ext"))
        assertEquals("song.my-ext", accepted("song.my-ext"))
    }

    @Test
    fun `이름 부분의 한글 공백 파일시스템 금지 문자는 제목으로 살려 둔다`() {
        assertEquals("아이유 - 밤편지.m4a", accepted("아이유 - 밤편지.m4a"))
        assertEquals("Track 1: Intro?.mp3", accepted("Track 1: Intro?.mp3"))
        assertEquals("a.b.c.wav", accepted("a.b.c.wav"))
    }

    @Test
    fun `경로 성분 제어 문자 앞뒤 공백을 걷어낸다`() {
        assertEquals("song.mp3", accepted("/etc/passwd/../song.mp3"))
        assertEquals("song.mp3", accepted("C:\\Users\\me\\song.mp3"))
        assertEquals("song.mp3", accepted("../../../../song.mp3"))
        assertEquals("songname.mp3", accepted("song\u0000\u001Fname\u007F.mp3"))
        assertEquals("song.mp3", accepted("   song.mp3   "))
    }

    @Test
    fun `긴 이름은 서로게이트 쌍을 쪼개지 않고 자른다`() {
        assertEquals(255, accepted("가".repeat(500) + ".mp3").substringBeforeLast('.').length)

        val name = accepted("a".repeat(254) + "🎵".repeat(10) + ".mp3").substringBeforeLast('.')

        assertEquals(254, name.length)
        assertFalse(name.last().isHighSurrogate())
    }

    @Test
    fun `이름을 알 수 없으면 거부한다`() {
        listOf(null, "", "    ", ".", "..", ".mp3", "song.mp3/", "C:\\dir\\").forEach(::assertRejected)
    }

    @Test
    fun `확장자가 없으면 mp3 로 추측하지 않고 거부한다`() {
        listOf("song", "song.").forEach(::assertRejected)
    }

    @Test
    fun `확장자에 ASCII 영숫자 밑줄 붙임표 밖의 문자가 있으면 거부한다`() {
        listOf('"', '*', ':', '<', '>', '?', '|').forEach { assertRejected("song.mp3$it") }
        assertRejected("song.mp 3")
        assertRejected("song.엠피3")
        assertRejected("song.mp\u00E9")
    }

    @Test
    fun `확장자는 16자까지 받고 거부 메시지에 원문을 다 싣지 않는다`() {
        assertEquals("song." + "a".repeat(16), accepted("song." + "a".repeat(16)))

        val error = useCase("song." + "x".repeat(500)).exceptionOrNull()

        assertTrue(error is InvalidUploadedFileNameException)
        assertTrue((error?.message?.length ?: 0) < 100)
    }
}
