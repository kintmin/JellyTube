package com.kintmin.presentation.ui.audio_edit

sealed interface AudioEditIntent {

    /** 파형에서 잘라낼 범위를 고르거나 미세조정했다. */
    data class OnSelectionChanged(val startMs: Long, val endMs: Long) : AudioEditIntent

    /** 선택한 범위를 잘라낸다. */
    data object OnClickCutSelection : AudioEditIntent

    /** 잘라낸 구간 하나를 되살린다. */
    data class OnClickRestoreCut(val index: Int) : AudioEditIntent

    /** 잘라낸 구간을 모두 되살린다. */
    data object OnClickResetCuts : AudioEditIntent

    /** 편집 결과를 들어본다. 재생 중이면 멈춘다. */
    data object OnClickTogglePreview : AudioEditIntent

    /** 편집본을 만들어 이 음원에 적용한다. */
    data object OnConfirmOverwrite : AudioEditIntent

    /** 편집본을 원본과 별개인 새 음원으로 저장한다. */
    data class OnConfirmSaveAsNew(val name: String) : AudioEditIntent
}
