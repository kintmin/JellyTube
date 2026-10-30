package com.kintmin.presentation.ui.audio_edit

import com.kintmin.domain.audio_edit.model.AudioEditClip
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.AudioWaveform

data class AudioEditUiState(
    val audioMediaId: Int,
    val audioMediaName: String,
    /** 편집은 항상 원본을 기준으로 한다. 편집본을 또 편집해 손실이 쌓이는 것을 막는다. */
    val originAudioFileFullPath: String,
    /** 파형 진폭. 긴 음원은 앞에서부터 읽힌 만큼씩 채워진다. */
    val waveform: AudioWaveform,
    /** 남길 조각들과 결과물 끝 위치. */
    val timeline: AudioEditTimeline,
    /** 사용자가 눈금 줄에서 정한 기준선. 자르기와 미리듣기 시작 위치가 된다. */
    val baselineMs: Long,
    val selectedClipIndex: Int?,
    val hasEditedAudio: Boolean,
    /** 싱크 가사가 있으면 편집으로 타임스탬프가 어긋난다고 알려준다. */
    val hasSyncedLyrics: Boolean,
    /** 미리듣기선 위치(타임라인 기준). 재생 중이 아니면 null. 기준선과는 별개다. */
    val previewPositionMs: Long?,
    val isPreviewPlaying: Boolean,
    val isLoading: Boolean,
    val isSaving: Boolean,
) {

    val originDurationMs: Long get() = timeline.originDurationMs

    val isDirty: Boolean get() = timeline.isDirty

    /** 편집 결과 길이. 실제 파일 길이는 렌더 후 측정값이라 수십 ms 차이가 날 수 있다. */
    val resultDurationMs: Long get() = timeline.endMs

    val canSave: Boolean get() = isDirty && timeline.hasAudio && !isSaving && !isLoading

    val canSplit: Boolean get() = timeline.canSplit(baselineMs)

    /** 기준선이 맞닿은 두 조각 사이에 있으면 자르기 버튼이 병합 버튼으로 바뀐다. */
    val canMerge: Boolean get() = timeline.canMerge(baselineMs)

    val canDelete: Boolean get() = selectedClipIndex != null

    /** 파형을 읽는 중이면 진행률(0~99), 다 읽었거나 아직 시작 전이면 null. */
    val waveformLoadingPercent: Int?
        get() = waveform
            .takeIf { it.peaks.isNotEmpty() && !it.isComplete }
            ?.let { (it.loadedBucketCount * 100 / it.peaks.size).coerceIn(0, 99) }

    companion object {

        fun getMock() = AudioEditUiState(
            audioMediaId = 1,
            audioMediaName = "음원 제목",
            originAudioFileFullPath = "/audio/origin.m4a",
            waveform = AudioWaveform(
                peaks = List(3_000) { index ->
                    // 미리보기용으로 들쭉날쭉한 파형을 만든다
                    (3000 + (index * 37 % 29_000))
                },
                // 뒤쪽은 아직 읽는 중인 상태를 보여준다
                loadedBucketCount = 2_400,
            ),
            timeline = AudioEditTimeline(
                originDurationMs = 300_000,
                clips = listOf(
                    AudioEditClip(sourceStartMs = 0, sourceEndMs = 60_000, positionMs = 0),
                    AudioEditClip(sourceStartMs = 60_000, sourceEndMs = 120_000, positionMs = 60_000),
                    AudioEditClip(sourceStartMs = 180_000, sourceEndMs = 300_000, positionMs = 200_000),
                ),
                endMs = 330_000,
            ),
            baselineMs = 60_000,
            selectedClipIndex = 1,
            hasEditedAudio = false,
            hasSyncedLyrics = true,
            previewPositionMs = 90_000,
            isPreviewPlaying = true,
            isLoading = false,
            isSaving = false,
        )

        fun getEmpty(audioMediaId: Int) = AudioEditUiState(
            audioMediaId = audioMediaId,
            audioMediaName = "",
            originAudioFileFullPath = "",
            waveform = AudioWaveform.EMPTY,
            timeline = AudioEditTimeline.initial(originDurationMs = 0),
            baselineMs = 0,
            selectedClipIndex = null,
            hasEditedAudio = false,
            hasSyncedLyrics = false,
            previewPositionMs = null,
            isPreviewPlaying = false,
            isLoading = true,
            isSaving = false,
        )
    }
}
