package com.kintmin.domain.audio_edit.repository

import com.kintmin.domain.audio_edit.model.AudioEditSegment
import com.kintmin.domain.audio_media.model.AudioMedia

interface AudioEditRepository {

    /** 파형 표시용 진폭 배열. 편집은 항상 원본을 기준으로 하므로 원본 경로를 넘긴다. */
    suspend fun extractWaveform(audioFileFullPath: String, bucketCount: Int): Result<List<Int>>

    /**
     * 구간을 이어붙인 편집본을 만들어 해당 음원에 연결한다. 이미 편집본이 있으면 교체한다.
     * 원본 파일은 지우지 않는다.
     */
    suspend fun applyEditedAudio(
        audioMediaId: Int,
        segments: List<AudioEditSegment>,
    ): Result<AudioMedia>

    /** 편집본 연결을 해제해 원본으로 되돌리고 편집본 파일을 지운다. */
    suspend fun revertEditedAudio(audioMediaId: Int): Result<AudioMedia>
}
