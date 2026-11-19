package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditPart

/**
 * 메인 재생과 별개로 음원을 들려주는 플레이어. 재생 큐를 건드리지 않는다.
 *
 * 앱에 하나뿐이라 [play] 가 앞서 걸린 재생을 끊는다 — 음원 편집 화면의 미리듣기와
 * 잠들기 타이머의 마무리 음성이 이것을 나눠 쓴다.
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
