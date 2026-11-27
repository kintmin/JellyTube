package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.RemappedLyricsLine

/**
 * 원본 시간축의 싱크 가사를 편집본 시간축으로 옮긴다.
 *
 * 조각이 떼어 온 구간에 든 줄만 남고, 잘려 나간 구간의 줄은 사라진다. 같은 구간을 두 번 놓으면
 * 그 줄도 두 번 나온다. 무음은 자리만 밀 뿐 줄을 만들지 않는다.
 *
 * 조각이 줄 중간에서 시작하면 **그 순간 울리고 있던 줄을 조각 머리에 붙인다** — 2절부터 잘라
 * 들어도 지금 들리는 가사가 바로 보여야 한다.
 *
 * [lines] 는 timeMs 오름차순이고 싱크가 있어야 한다. 비싱크 가사는 옮길 근거가 없어 호출자가 가린다.
 */
class RemapLyricsToAudioEditUseCase {

    operator fun invoke(lines: List<LyricsLine>, parts: List<AudioEditPart>): List<RemappedLyricsLine> {
        val remapped = mutableListOf<RemappedLyricsLine>()
        var cursorMs = 0L
        for (part in parts) {
            if (part is AudioEditPart.Audio) {
                carryActiveLine(lines, part, cursorMs, remapped)
                lines.forEachIndexed { index, line ->
                    val timeMs = line.timeMs ?: return@forEachIndexed
                    if (timeMs >= part.startMs && timeMs < part.endMs) {
                        remapped += RemappedLyricsLine(sourceIndex = index, timeMs = cursorMs + (timeMs - part.startMs))
                    }
                }
            }
            cursorMs += part.durationMs
        }
        return remapped
    }

    private fun carryActiveLine(
        lines: List<LyricsLine>,
        part: AudioEditPart.Audio,
        cursorMs: Long,
        remapped: MutableList<RemappedLyricsLine>,
    ) {
        val activeIndex = activeLyricIndex(lines, part.startMs).takeIf { it >= 0 } ?: return
        // 조각 안에서 다시 나올 줄이거나 앞 조각 끝에 이미 놓인 줄이면 당기지 않는다 — 같은 줄이 연달아 보인다.
        if (lines[activeIndex].timeMs == part.startMs) return
        if (remapped.lastOrNull()?.sourceIndex == activeIndex) return
        remapped += RemappedLyricsLine(sourceIndex = activeIndex, timeMs = cursorMs)
    }
}
