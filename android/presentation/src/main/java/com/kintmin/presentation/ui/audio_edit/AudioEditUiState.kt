package com.kintmin.presentation.ui.audio_edit

import com.kintmin.domain.audio_edit.model.AudioEditSegment

data class AudioEditUiState(
    val audioMediaId: Int,
    val audioMediaName: String,
    /** 편집은 항상 원본을 기준으로 한다. 편집본을 또 편집해 손실이 쌓이는 것을 막는다. */
    val originAudioFileFullPath: String,
    val originDurationMs: Long,
    /** 파형 진폭. 상대 크기만 의미가 있어 그릴 때 최대값으로 정규화한다. */
    val waveform: List<Int>,
    /** 잘라낼 범위를 고르는 선택 구간. */
    val selectionStartMs: Long,
    val selectionEndMs: Long,
    /** 사용자가 잘라낸 구간들. 이 여집합이 남길 구간이 된다. */
    val cutRanges: List<AudioEditSegment>,
    /** 남길 구간. cutRanges에서 계산된 결과이며 렌더에 그대로 넘어간다. */
    val keepSegments: List<AudioEditSegment>,
    val hasEditedAudio: Boolean,
    /** 싱크 가사가 있으면 편집으로 타임스탬프가 어긋난다고 알려준다. */
    val hasSyncedLyrics: Boolean,
    /** 미리듣기 커서 위치(원본 기준). 재생 중이 아니면 null. */
    val previewPositionMs: Long?,
    val isPreviewPlaying: Boolean,
    val isLoading: Boolean,
    val isSaving: Boolean,
) {

    val isDirty: Boolean get() = cutRanges.isNotEmpty()

    /** 편집 결과 길이. 실제 파일 길이는 렌더 후 측정값이라 수십 ms 차이가 날 수 있다. */
    val resultDurationMs: Long get() = keepSegments.sumOf { it.durationMs }

    /** 남는 구간이 없으면 저장할 수 없다. */
    val canSave: Boolean get() = isDirty && keepSegments.isNotEmpty() && !isSaving && !isLoading

    val hasSelection: Boolean get() = selectionEndMs > selectionStartMs

    companion object {

        fun getMock() = AudioEditUiState(
            audioMediaId = 1,
            audioMediaName = "음원 제목",
            originAudioFileFullPath = "/audio/origin.m4a",
            originDurationMs = 300_000,
            waveform = List(120) { index ->
                // 미리보기용으로 들쭉날쭉한 파형을 만든다
                (3000 + (index * 37 % 29_000))
            },
            selectionStartMs = 30_000,
            selectionEndMs = 90_000,
            cutRanges = listOf(AudioEditSegment(startMs = 180_000, endMs = 210_000)),
            keepSegments = listOf(
                AudioEditSegment(startMs = 0, endMs = 180_000),
                AudioEditSegment(startMs = 210_000, endMs = 300_000),
            ),
            hasEditedAudio = false,
            hasSyncedLyrics = true,
            previewPositionMs = 60_000,
            isPreviewPlaying = true,
            isLoading = false,
            isSaving = false,
        )

        fun getEmpty(audioMediaId: Int) = AudioEditUiState(
            audioMediaId = audioMediaId,
            audioMediaName = "",
            originAudioFileFullPath = "",
            originDurationMs = 0,
            waveform = emptyList(),
            selectionStartMs = 0,
            selectionEndMs = 0,
            cutRanges = emptyList(),
            keepSegments = emptyList(),
            hasEditedAudio = false,
            hasSyncedLyrics = false,
            previewPositionMs = null,
            isPreviewPlaying = false,
            isLoading = true,
            isSaving = false,
        )
    }
}
