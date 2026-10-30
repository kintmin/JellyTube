package com.kintmin.domain.audio_edit.repository

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.AudioWaveform
import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.coroutines.flow.Flow

interface AudioEditRepository {

    fun observeSavingAudioMediaIds(): Flow<Set<Int>>

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
