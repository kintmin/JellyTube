package com.kintmin.platform.audio_preview

import com.kintmin.domain.audio_edit.model.AudioEditPart

/**
 * 음원 편집 화면 전용 미리듣기 플레이어.
 */
interface AudioPreviewPlayer {

    val isPlaying: Boolean

    val currentPositionMs: Long?

    /**
     * [parts]를 순서대로 이어 타임라인 [startAtMs] 위치부터 재생한다.
     * 이미 재생 중이면 새 조각으로 교체한다.
     */
    fun play(audioFileFullPath: String, parts: List<AudioEditPart>, startAtMs: Long)

    fun pause()
    fun release()
}
