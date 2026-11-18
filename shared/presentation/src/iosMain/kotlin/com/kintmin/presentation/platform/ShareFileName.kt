package com.kintmin.presentation.platform

private val INVALID_CHARS = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
private val WHITESPACE = Regex("\\s+")

// 한글은 글자당 3바이트라 80자면 240바이트로, 파일명 한도(255바이트)에 확장자가 들어갈 자리가 남는다.
private const val MAX_BASE_LENGTH = 80
private const val FALLBACK_BASE = "JellyTube"

internal fun shareFileName(title: String, artist: String, ext: String): String {
    val base = listOf(artist, title)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" - ")
        .replace(INVALID_CHARS, "_")
        .replace(WHITESPACE, " ")
        .trim()
        .take(MAX_BASE_LENGTH)
        .let { if (it.lastOrNull()?.isHighSurrogate() == true) it.dropLast(1) else it }
        .trim()
        .trimEnd('.')
        .ifBlank { FALLBACK_BASE }
    return "$base.$ext"
}
