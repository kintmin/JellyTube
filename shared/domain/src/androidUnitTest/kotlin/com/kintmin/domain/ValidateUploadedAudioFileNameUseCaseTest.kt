package com.kintmin.domain

import com.kintmin.domain.audio_media.usecase.InvalidUploadedFileExtension
import com.kintmin.domain.audio_media.usecase.InvalidUploadedFileName
import com.kintmin.domain.audio_media.usecase.MissingUploadedFileExtension
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateUploadedAudioFileNameUseCaseTest {

    private val useCase = ValidateUploadedAudioFileNameUseCase()

    private fun accepted(rawFileName: String?): String =
        useCase(rawFileName).getOrThrow()

    private inline fun <reified T : Throwable> assertRejectedWith(rawFileName: String?) {
        val error = useCase(rawFileName).exceptionOrNull()
        assertTrue(
            "rawFileName=$rawFileName 는 ${T::class.simpleName}로 거부돼야 하는데 ${error?.let { it::class.simpleName }} 였다",
            error is T,
        )
    }

    // ─── 정상 통과: 확장자는 변형 없이 보존된다 ─────────────────────────────

    @Test
    fun `일반 파일 이름은 그대로 통과한다`() {
        assertEquals("song.mp3", accepted("song.mp3"))
    }

    @Test
    fun `확장자 대소문자를 변형하지 않는다`() {
        // iOS가 확장자를 신뢰하므로 정규화로 건드리면 안 된다
        assertEquals("song.FLAC", accepted("song.FLAC"))
        assertEquals("song.Opus", accepted("song.Opus"))
    }

    @Test
    fun `한글과 공백이 든 이름은 보존한다`() {
        assertEquals("아이유 - 밤편지.m4a", accepted("아이유 - 밤편지.m4a"))
    }

    @Test
    fun `제목에 정상적으로 쓰이는 파일시스템 금지문자는 이름 부분에서 살려둔다`() {
        // ':' 와 '?' 는 FAT에서 금지지만 이름 부분은 제목 폴백으로만 쓰여 파일시스템에 닿지 않는다.
        // 실제 저장 파일명은 "<UUID>.mp3" 이므로 여기서 지우면 멀쩡한 제목만 망가진다.
        assertEquals("Track 1: Intro?.mp3", accepted("Track 1: Intro?.mp3"))
    }

    @Test
    fun `점이 여러 개면 마지막 점 뒤만 확장자로 본다`() {
        assertEquals("a.b.c.wav", accepted("a.b.c.wav"))
    }

    @Test
    fun `확장자에 허용된 밑줄과 붙임표를 통과시킨다`() {
        // POSIX Portable Filename Character Set 에 포함된 문자
        assertEquals("song.my_ext", accepted("song.my_ext"))
        assertEquals("song.my-ext", accepted("song.my-ext"))
    }

    // ─── 정규화: 경로 성분과 제어 문자 ───────────────────────────────────

    @Test
    fun `유닉스 경로 성분을 제거한다`() {
        assertEquals("song.mp3", accepted("/etc/passwd/../song.mp3"))
    }

    @Test
    fun `윈도우 경로 성분을 제거한다`() {
        assertEquals("song.mp3", accepted("C:\\Users\\me\\song.mp3"))
    }

    @Test
    fun `상위 경로 표기가 섞여도 파일 이름만 남는다`() {
        assertEquals("song.mp3", accepted("../../../../song.mp3"))
    }

    @Test
    fun `제어 문자를 제거한다`() {
        // 로그와 UI로 흘러가면 표시가 깨지므로 정규화 대상이다
        assertEquals("songname.mp3", accepted("song\u0000\u001Fname\u007F.mp3"))
    }

    @Test
    fun `앞뒤 공백을 제거한다`() {
        assertEquals("song.mp3", accepted("   song.mp3   "))
    }

    @Test
    fun `이름 부분이 아주 길면 잘라낸다`() {
        val result = accepted("가".repeat(500) + ".mp3")

        assertEquals(255, result.substringBeforeLast('.').length)
        assertEquals("mp3", result.substringAfterLast('.'))
    }

    @Test
    fun `길이 제한이 서로게이트 페어를 쪼개지 않는다`() {
        // 255번째 문자가 서로게이트 페어의 앞쪽이면 경계를 앞으로 당겨 U+FFFD 발생을 막는다
        val emoji = "\uD83C\uDFB5" // 🎵
        val result = accepted("a".repeat(254) + emoji.repeat(10) + ".mp3")
        val name = result.substringBeforeLast('.')

        assertEquals(254, name.length)
        assertTrue("잘린 끝에 홀로 남은 상위 서로게이트가 없어야 한다", !name.last().isHighSurrogate())
    }

    // ─── 거부: 확장자는 추측하지 않는다 (폴백 없음) ────────────────────────

    @Test
    fun `헤더가 없으면 거부한다`() {
        assertRejectedWith<InvalidUploadedFileName>(null)
    }

    @Test
    fun `빈 이름은 거부한다`() {
        assertRejectedWith<InvalidUploadedFileName>("")
        assertRejectedWith<InvalidUploadedFileName>("    ")
    }

    @Test
    fun `확장자가 없으면 mp3로 추측하지 않고 거부한다`() {
        assertRejectedWith<MissingUploadedFileExtension>("song")
        assertRejectedWith<MissingUploadedFileExtension>("song.")
    }

    @Test
    fun `이름 없이 확장자만 있으면 거부한다`() {
        assertRejectedWith<InvalidUploadedFileName>(".mp3")
    }

    @Test
    fun `점으로만 이루어진 이름은 거부한다`() {
        assertRejectedWith<InvalidUploadedFileName>(".")
        assertRejectedWith<InvalidUploadedFileName>("..")
    }

    @Test
    fun `경로 성분을 지운 뒤 남는 게 없으면 거부한다`() {
        assertRejectedWith<InvalidUploadedFileName>("song.mp3/")
        assertRejectedWith<InvalidUploadedFileName>("C:\\dir\\")
    }

    // ─── 거부: FAT 금지문자가 확장자에 있는 경우 ───────────────────────────

    @Test
    fun `AOSP가 FAT 파일명에서 거부하는 문자가 확장자에 있으면 거부한다`() {
        // FileUtils.isValidFatFilenameChar 가 거부하는 9개 문자.
        // '/' 와 '\' 는 경로 성분으로 먼저 잘려나가므로 나머지 7개를 확인한다.
        listOf('"', '*', ':', '<', '>', '?', '|').forEach { invalidChar ->
            assertRejectedWith<InvalidUploadedFileExtension>("song.mp3$invalidChar")
        }
    }

    @Test
    fun `확장자에 공백이 있으면 거부한다`() {
        assertRejectedWith<InvalidUploadedFileExtension>("song.mp 3")
    }

    @Test
    fun `비ASCII 확장자는 거부한다`() {
        // Char.isLetterOrDigit() 은 유니코드 기준이라 한글을 통과시킨다. 명시적 ASCII 범위여야 걸러진다.
        assertRejectedWith<InvalidUploadedFileExtension>("song.엠피3")
        // NBSP(U+00A0)\uB294 Kotlin trim()\uC774 \uAC77\uC5B4\uB0B4\uBBC0\uB85C, \uACF5\uBC31\uC774 \uC544\uB2CC \uBE44ASCII \uBB38\uC790\uB85C \uD655\uC778\uD55C\uB2E4
        assertRejectedWith<InvalidUploadedFileExtension>("song.mp\u00E9")
    }

    @Test
    fun `확장자 길이는 상한까지 통과하고 넘으면 거부한다`() {
        assertEquals("song." + "a".repeat(16), accepted("song." + "a".repeat(16)))
        assertRejectedWith<InvalidUploadedFileExtension>("song." + "a".repeat(17))
    }

    @Test
    fun `거부 메시지에는 잘린 확장자만 담기고 원문이 그대로 흐르지 않는다`() {
        val error = useCase("song." + "x".repeat(500)).exceptionOrNull()

        assertTrue(error is InvalidUploadedFileExtension)
        assertTrue(
            "메시지에 담긴 확장자는 상한 이하로 잘려야 한다",
            (error?.message?.length ?: 0) < 100,
        )
    }
}
