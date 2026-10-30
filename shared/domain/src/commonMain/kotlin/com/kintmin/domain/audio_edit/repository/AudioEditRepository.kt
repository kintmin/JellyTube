package com.kintmin.domain.audio_edit.repository

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.AudioWaveform
import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.coroutines.flow.Flow

interface AudioEditRepository {

    fun observeSavingAudioMediaIds(): Flow<Set<Int>>

    /**
     * 저장을 백그라운드에 맡기기 전에 저장 중으로 미리 표시한다. 작업이 시작되기 전에도 중복 저장과
     * 되돌리기를 막기 위해서다. 이미 저장 중이면 false.
     */
    fun reserveSaving(audioMediaId: Int): Boolean

    /** 저장을 맡기지 못했거나 저장이 시작 전에 끝났을 때 예약을 푼다. */
    fun releaseSaving(audioMediaId: Int)

    /** 파일의 실제 길이(ms). */
    suspend fun measureAudioDurationMs(audioFileFullPath: String): Result<Long>

    fun extractWaveform(audioFileFullPath: String, bucketCount: Int): Flow<AudioWaveform>

    suspend fun fetchEditTimeline(audioMediaId: Int): Result<AudioEditTimeline?>

    suspend fun applyEditedAudio(
        audioMediaId: Int,
        timeline: AudioEditTimeline,
    ): Result<AudioMedia>

    suspend fun revertEditedAudio(audioMediaId: Int): Result<AudioMedia>

    suspend fun saveEditedAudioAsNewMedia(
        audioMediaId: Int,
        parts: List<AudioEditPart>,
        name: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AudioMedia>
}
