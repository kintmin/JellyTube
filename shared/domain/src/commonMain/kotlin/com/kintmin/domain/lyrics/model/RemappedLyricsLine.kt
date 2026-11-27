package com.kintmin.domain.lyrics.model

/**
 * 편집본 시간축으로 옮긴 가사 줄 한 개.
 *
 * 텍스트를 담지 않고 [sourceIndex] 로 원본 줄을 가리킨다 — 원문·뜻·읽는 법이 같은 줄 번호로
 * 짝지어져 있어서, 한 번 옮긴 결과를 세 겹에 그대로 쓸 수 있다.
 */
data class RemappedLyricsLine(
    val sourceIndex: Int,
    val timeMs: Long,
)
