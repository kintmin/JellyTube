package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsLine

/**
 * 개행(줄넘김)이 포함된 가사 줄을 여러 줄로 쪼갠다.
 *
 * 위에서 아래로 순차 처리하며, 개행이 있는 줄은
 * [해당 줄 시작 시간 ~ 다음 줄 시작 시간(없으면 음원 종료 시각 audioEndMs)] 구간을
 * 쪼갠 조각 수(n)로 1/n 균등 배분해 각 조각에 시작 시간을 부여한다.
 *
 * 이미 SYNC 로 쪼개진 가사든 plain 가사든 동일하게 적용된다.
 */
class SplitLyricsByNewlineUseCase {

    operator fun invoke(lines: List<LyricsLine>, audioEndMs: Long): List<LyricsLine> {
        val result = mutableListOf<LyricsLine>()
        for (i in lines.indices) {
            val line = lines[i]
            val parts = line.text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

            if (parts.size <= 1) {
                result.add(line.copy(text = parts.firstOrNull() ?: line.text.trim()))
                continue
            }

            val startMs = line.timeMs ?: 0L
            val nextStartMs = (lines.getOrNull(i + 1)?.timeMs ?: audioEndMs).coerceAtLeast(startMs)
            val span = nextStartMs - startMs
            for (j in parts.indices) {
                val timeMs = startMs + span * j / parts.size
                result.add(LyricsLine(timeMs = timeMs, text = parts[j]))
            }
        }
        return result
    }
}
