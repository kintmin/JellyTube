package com.kintmin.domain.playlist

import com.kintmin.domain.extension.clampLength

object PlaylistTextLimit {
    const val MAX_TITLE_LENGTH = 40
    const val MAX_DESCRIPTION_LENGTH = 100

    /**
     * 개행을 공백으로 바꾸는 것까지가 상한이다 — 붙여넣기와 유튜브 원제로 섞여 들어오고,
     * 제목을 두 줄로 그리는 화면에서 개행 하나가 한 줄을 먹는다.
     */
    fun clampTitle(title: String): String =
        title.replace(NEWLINE, " ").trim().clampLength(MAX_TITLE_LENGTH).trim()

    fun clampDescription(description: String): String = description.clampLength(MAX_DESCRIPTION_LENGTH)

    private val NEWLINE = Regex("[\\r\\n\\u0085\\u2028\\u2029]")
}
