package com.kintmin.domain.audio_edit.repository

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_media.model.AudioMedia

interface AudioEditRepository {

    /** 파형 표시용 진폭 배열. 편집은 항상 원본을 기준으로 하므로 원본 경로를 넘긴다. */
    suspend fun extractWaveform(audioFileFullPath: String, bucketCount: Int): Result<List<Int>>

    /**
     * 소리·무음 조각을 이어붙인 편집본을 만들어 해당 음원에 연결한다. 이미 편집본이 있으면 교체한다.
     * 원본 파일은 지우지 않는다.
     */
    suspend fun applyEditedAudio(
        audioMediaId: Int,
        parts: List<AudioEditPart>,
    ): Result<AudioMedia>

    /** 편집본 연결을 해제해 원본으로 되돌리고 편집본 파일을 지운다. */
    suspend fun revertEditedAudio(audioMediaId: Int): Result<AudioMedia>

    /**
     * 소리·무음 조각을 이어붙인 결과를 **별 음원으로** 저장한다. 원본 음원은 그대로 남는다.
     *
     * 썸네일은 복사하고 노래방 번호는 물려받되 가사는 가져오지 않는다. 길이가 달라져
     * 시간이 맞춰진 가사는 어차피 맞지 않는다.
     */
    suspend fun saveEditedAudioAsNewMedia(
        audioMediaId: Int,
        parts: List<AudioEditPart>,
        name: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AudioMedia>
}
