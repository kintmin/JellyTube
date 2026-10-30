package com.kintmin.presentation.ui.audio_edit

import com.kintmin.domain.audio_edit.model.AudioEditTimeline

sealed interface AudioEditIntent {

    /** 눈금 줄을 누르거나 끌어 기준선을 옮겼다. */
    data class OnBaselineChanged(val ms: Long) : AudioEditIntent

    /** 끝 표시를 끌었다. 뒤로 끌면 무음이 붙고 앞으로 당기면 소리가 잘린다. */
    data class OnTimelineEndChanged(val ms: Long) : AudioEditIntent

    /** 파형에서 조각을 눌렀다. 빈칸을 누르면 null로 선택이 풀린다. */
    data class OnClipSelected(val index: Int?) : AudioEditIntent

    /** 조각 경계를 꾹 눌러 끌었다. */
    data class OnClipEdgeDragged(
        val index: Int,
        val edge: AudioEditTimeline.Edge,
        val ms: Long,
    ) : AudioEditIntent

    /** 조각 안쪽을 꾹 눌러 끌었다. 조각을 [positionMs]로 옮긴다. */
    data class OnClipMoved(val index: Int, val positionMs: Long) : AudioEditIntent

    /** 기준선 위치에서 조각을 나눈다. */
    data object OnClickSplit : AudioEditIntent

    /** 선택한 조각을 지워 빈칸으로 만든다. */
    data object OnClickDeleteClip : AudioEditIntent

    /** 기준선부터 편집 결과를 들어본다. 재생 중이면 멈춘다. */
    data object OnClickTogglePreview : AudioEditIntent

    /** 편집본을 만들어 이 음원에 적용한다. */
    data object OnConfirmOverwrite : AudioEditIntent

    /** 편집본을 원본과 별개인 새 음원으로 저장한다. */
    data class OnConfirmSaveAsNew(val name: String) : AudioEditIntent
}
