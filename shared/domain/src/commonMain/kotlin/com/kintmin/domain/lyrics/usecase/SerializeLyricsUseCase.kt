package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsLine

/**
 * 가사 줄 목록을 LRC(SYNC) 문자열로 직렬화한다. (ParseLyricsUseCase 의 역방향)
 *
 * 각 줄은 `[M:SS.cc]텍스트` 형식으로 만든다.
 * - M  = 총 분(시는 분으로 환산, 자릿수 제한 없음. 최소 2자리 zero-pad)
 * - SS = 2자리 초
 * - cc = 2자리 센티초(밀리초를 10으로 나눈 값)
 *
 * timeMs 가 null 이면 0 으로 본다.
 * 한 줄의 text 에 개행이 남아 있으면 같은 timeMs 로 여러 LRC 라인으로 전개한다.
 */
class SerializeLyricsUseCase {

    operator fun invoke(lines: List<LyricsLine>): String {
        return lines.joinToString("\n") { line ->
            val tag = formatTag(line.timeMs ?: 0L)
            line.text.split("\n").joinToString("\n") { part -> "$tag$part" }
        }
    }

    private fun formatTag(timeMs: Long): String {
        val safeMs = if (timeMs < 0L) 0L else timeMs
        val minutes = safeMs / 60_000L
        val seconds = (safeMs % 60_000L) / 1_000L
        val centiseconds = (safeMs % 1_000L) / 10L
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')
        val cc = centiseconds.toString().padStart(2, '0')
        return "[$mm:$ss.$cc]"
    }
}
