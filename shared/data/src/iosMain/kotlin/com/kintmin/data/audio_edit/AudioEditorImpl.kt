package com.kintmin.data.audio_edit

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioWaveform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 음원 편집 미구현 스텁. [AudioEditor]를 쓰는 Repository가 광범위 single로 등록되기 때문에,
 * 바인딩을 비워두면 그 Repository를 꺼내는 순간 Koin이 터진다.
 */
internal class AudioEditorImpl : AudioEditor {

    override suspend fun measureDurationMs(audioFileFullPath: String): Result<Long> = unsupported()

    override fun extractWaveform(
        audioFileFullPath: String,
        bucketCount: Int,
    ): Flow<AudioWaveform> = flow { throw unsupportedException() }

    override suspend fun render(
        sourceFileFullPath: String,
        parts: List<AudioEditPart>,
        outputFileFullPath: String,
    ): Result<Long> = unsupported()

    private fun <T> unsupported(): Result<T> = Result.failure(unsupportedException())

    private fun unsupportedException() = UnsupportedOperationException("iOS에서는 음원 편집을 지원하지 않습니다.")
}
