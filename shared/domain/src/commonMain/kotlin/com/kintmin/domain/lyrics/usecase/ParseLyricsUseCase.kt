package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsLine

/**
 * 가사 원문을 줄 단위로 파싱한다.
 * LRC 타임태그([mm:ss.xx])가 하나라도 있으면 싱크 가사로 보고 타임스탬프를 추출하며,
 * 없으면 일반 가사로 보고 모든 줄을 timeMs=null 로 반환한다.
 */
class ParseLyricsUseCase {

    private val timeTagRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    operator fun invoke(rawLyrics: String): List<LyricsLine> {
        val lines = rawLyrics.split("\n")
        val synced = lines.any { timeTagRegex.containsMatchIn(it) }

        if (!synced) {
            return lines.map { LyricsLine(timeMs = null, text = it.trim()) }
        }

        val result = mutableListOf<LyricsLine>()
        for (line in lines) {
            val matches = timeTagRegex.findAll(line).toList()
            if (matches.isEmpty()) continue // 메타태그([ar:], [ti:] 등)나 빈 줄은 건너뛴다.
            val text = line.replace(timeTagRegex, "").trim()
            for (match in matches) {
                result.add(LyricsLine(timeMs = match.toTimeMs(), text = text))
            }
        }
        return result.sortedBy { it.timeMs }
    }

    private fun MatchResult.toTimeMs(): Long {
        val minutes = groupValues[1].toLong()
        val seconds = groupValues[2].toLong()
        val fraction = groupValues[3]
        val fractionMs = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.take(3).toLong()
        }
        return (minutes * 60 + seconds) * 1000 + fractionMs
    }
}

/**
 * 현재 재생 위치(ms)에 해당하는 활성 가사 줄 인덱스를 계산한다.
 * timeMs 가 positionMs 이하인 마지막 줄을 반환하며, 해당 줄이 없거나 비싱크면 -1.
 * (lines 는 timeMs 오름차순 정렬 가정)
 */
fun activeLyricIndex(lines: List<LyricsLine>, positionMs: Long): Int {
    var active = -1
    for (index in lines.indices) {
        val timeMs = lines[index].timeMs ?: continue
        if (timeMs <= positionMs) active = index else break
    }
    return active
}
