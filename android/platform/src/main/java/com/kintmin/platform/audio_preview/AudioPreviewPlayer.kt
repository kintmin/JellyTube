package com.kintmin.platform.audio_preview

import com.kintmin.domain.audio_edit.model.AudioEditPart

/**
 * 음원 편집 화면 전용 미리듣기 플레이어.
 *
 * 편집 결과를 렌더하지 않고도 들어볼 수 있게, 소리·무음 조각들을 이어서 재생한다.
 * 실제 재생(MediaControllerManager)과 별 인스턴스라 사용자가 듣던 음악을 가로채지 않는다.
 */
interface AudioPreviewPlayer {

    val isPlaying: Boolean

    /**
     * 편집 타임라인 기준 현재 재생 위치(ms). 재생 중이 아니면 null.
     *
     * 조각을 이어붙여 재생하므로 플레이어의 위치는 조각 안에서의 상대 위치다.
     * 파형에 미리듣기선을 그릴 수 있도록 타임라인 기준으로 환산해서 돌려준다.
     */
    val currentPositionMs: Long?

    /**
     * [parts]를 순서대로 이어 타임라인 [startAtMs] 위치부터 재생한다.
     * 이미 재생 중이면 새 조각으로 교체한다.
     */
    fun play(audioFileFullPath: String, parts: List<AudioEditPart>, startAtMs: Long)

    fun pause()

    /** 플레이어를 해제한다. 화면을 벗어날 때 호출한다. 이후 [play]를 부르면 다시 만들어진다. */
    fun release()
}
