package com.kintmin.domain.audio_media.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioContainerSnifferTest {

    // 실제 파일의 앞 64바이트. yt_* 는 YouTube 140(m4a)·251(webm) 을 yt-dlp 로 받은 것이다.
    private val samples = mapOf(
        "yt_140.m4a" to (AudioContainer.MP4 to "0000001866747970646173680000000069736f366d703431000002bb6d6f6f760000006c6d76686400000000e5799a29e5799a290000ac44000cd40000010000"),
        "yt_251.webm" to (AudioContainer.WEBM to "1a45dfa39f4286810142f7810142f2810442f381084282847765626d428781044285810218538067010000000003d8e6114d9b74bf4dbb9253ab841549a96653"),
        "mp3.mp3" to (AudioContainer.MP3 to "49443304000000000022545353450000000e0000034c61766636332e312e3130300000000000000000000000fffb540000000000000000000000000000000000"),
        "aac_adts.aac" to (AudioContainer.ADTS to "fff14c80269ffcdc004c61766336332e312e3130300042551ffffffff80262db4f6190b52f69ebdfaeb5c26b8de8b8912f24dc7273a1009ba61fe159a0cec322"),
        "flac.flac" to (AudioContainer.FLAC to "664c614300000022044a044a0000780001910bb802f000023280bd083fe15cfa73bf72b84078f1125e930400002c0c0000004c61766636332e312e3130300100"),
        "wav.wav" to (AudioContainer.WAVE to "5249464646ca080057415645666d7420100000000100020080bb000000ee0200040010004c4953541a000000494e464f495346540d0000004c61766636332e31"),
        "aiff.aiff" to (AudioContainer.AIFF to "464f524d0008ca2e41494646434f4d4d000000120002000232800010400ebb8000000000000053534e440008ca0800000000000000000000000000a000a00152"),
        "opus_caf.caf" to (AudioContainer.CAF to "636166660001000064657363000000000000002040e77000000000006f7075730000000000000000000003c000000002000000006b756b69000000000000001c"),
        "opus_ogg.opus" to (AudioContainer.OGG_OPUS to "4f676753000200000000000000002ae6d05d00000000198c596201134f707573486561640102380180bb00000000004f676753000000000000000000002ae6d0"),
        "vorbis_ogg.ogg" to (AudioContainer.OGG to "4f676753000200000000000000002608b2b4000000000ed77836011e01766f72626973000000000280bb00000000000080b5010000000000b8014f6767530000"),
        "wma.wma" to (AudioContainer.ASF to "3026b2758e66cf11a6d900aa0062ce6cec01000000000000050000000102a1dcab8c47a9cf118ee400c00c205365680000000000000000000000000000000000"),
        "alac.m4a" to (AudioContainer.MP4 to "0000001c667479704d344120000002004d34412069736f6d69736f3200000008667265650000d5d66d64617420000000020f0c01860001ffd7ff85ffd4004c0f"),
    )

    @Test
    fun `실제 파일 헤더로 컨테이너를 가른다`() {
        samples.forEach { (name, expected) ->
            assertEquals(name, expected.first, AudioContainerSniffer.detect(expected.second.hexToBytes()))
        }
    }

    @Test
    fun `ID3 없는 MP3 프레임과 ADTS 를 layer 비트로 가른다`() {
        assertEquals(AudioContainer.MP3, AudioContainerSniffer.detect("fffb9064".hexToBytes()))
        assertEquals(AudioContainer.ADTS, AudioContainerSniffer.detect("fff14c80".hexToBytes()))
        assertEquals(AudioContainer.ADTS, AudioContainerSniffer.detect("fff94c80".hexToBytes()))
    }

    @Test
    fun `모르는 내용이나 짧은 헤더는 판별하지 않는다`() {
        assertNull(AudioContainerSniffer.detect(ByteArray(0)))
        assertNull(AudioContainerSniffer.detect("00".hexToBytes()))
        assertNull(AudioContainerSniffer.detect("3c68746d6c3e".hexToBytes()))
    }

    @Test
    fun `내용과 다른 확장자만 바로잡는다`() {
        val m4a = samples.getValue("yt_140.m4a").second.hexToBytes()

        assertEquals("uuid.m4a", AudioContainerSniffer.correctedFileName("uuid.mp3", m4a))
        assertEquals("uuid.m4a", AudioContainerSniffer.correctedFileName("uuid", m4a))
        assertNull(AudioContainerSniffer.correctedFileName("uuid.m4a", m4a))
        assertNull(AudioContainerSniffer.correctedFileName("uuid.M4A", m4a))
        assertNull(AudioContainerSniffer.correctedFileName("uuid.mp3", "3c68746d6c3e".hexToBytes()))
    }

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
