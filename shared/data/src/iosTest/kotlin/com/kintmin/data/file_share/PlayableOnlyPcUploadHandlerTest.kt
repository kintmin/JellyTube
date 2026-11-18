package com.kintmin.data.file_share

import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.file_share.PcUploadHandler
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayableOnlyPcUploadHandlerTest {

    private val handler = PlayableOnlyPcUploadHandler(
        object : PcUploadHandler {
            override fun validateFileName(rawFileName: String?) = Result.success(rawFileName.orEmpty())
            override suspend fun importUploadedAudio(uploadedFileFullPath: String, originalFileName: String) =
                Result.failure<ImportedAudioMediaResult>(NotImplementedError())
            override suspend fun applyArtist(audioMediaIds: List<Int>, artist: String) = Result.success(Unit)
            override suspend fun applyThumbnail(audioMediaIds: List<Int>, imageData: ByteArray) = Result.success(Unit)
        },
    )

    @Test
    fun `재생할 수 있는 형식은 그대로 받는다`() {
        assertEquals("곡.M4A", handler.validateFileName("곡.M4A").getOrThrow())
    }

    @Test
    fun `재생할 수 없는 형식은 이유와 함께 거절한다`() {
        listOf("곡.ogg", "곡.opus", "곡.wma").forEach {
            assertEquals("이 기기에서 재생할 수 없는 형식입니다.", handler.validateFileName(it).exceptionOrNull()?.message)
        }
    }
}
