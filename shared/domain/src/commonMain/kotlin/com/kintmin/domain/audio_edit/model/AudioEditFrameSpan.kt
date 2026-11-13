package com.kintmin.domain.audio_edit.model

import kotlin.math.roundToLong

/** [AudioEditPart]를 샘플 프레임 단위로 옮긴 것. */
sealed interface AudioEditFrameSpan {

    val frameCount: Long

    data class Audio(
        val sourceStartFrame: Long,
        override val frameCount: Long,
    ) : AudioEditFrameSpan

    data class Silence(
        override val frameCount: Long,
    ) : AudioEditFrameSpan
}

fun List<AudioEditPart>.toFrameSpans(
    sampleRate: Double,
    sourceFrameCount: Long,
    startAtMs: Long = 0L,
): List<AudioEditFrameSpan> {
    fun frameOf(ms: Long): Long = (ms * sampleRate / 1000).roundToLong()

    val spans = mutableListOf<AudioEditFrameSpan>()
    var partStartMs = 0L
    for (part in this) {
        val partEndMs = partStartMs + part.durationMs
        val partStartFrame = frameOf(partStartMs)
        val startFrame = frameOf(maxOf(partStartMs, startAtMs))
        val frameCount = frameOf(partEndMs) - startFrame

        if (frameCount > 0) {
            when (part) {
                is AudioEditPart.Audio -> {
                    val sourceStartFrame = frameOf(part.startMs) + (startFrame - partStartFrame)
                    val audioFrameCount = frameCount.coerceAtMost(sourceFrameCount - sourceStartFrame).coerceAtLeast(0)
                    if (audioFrameCount > 0) spans += AudioEditFrameSpan.Audio(sourceStartFrame, audioFrameCount)
                    if (frameCount > audioFrameCount) spans += AudioEditFrameSpan.Silence(frameCount - audioFrameCount)
                }

                is AudioEditPart.Silence -> spans += AudioEditFrameSpan.Silence(frameCount)
            }
        }
        partStartMs = partEndMs
    }
    return spans
}
