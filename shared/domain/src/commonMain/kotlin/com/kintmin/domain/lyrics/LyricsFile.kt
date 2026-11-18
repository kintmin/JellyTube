package com.kintmin.domain.lyrics

/** 가사 파일의 확장자 규칙. 싱크 가사(lrc)인지는 확장자로만 가린다. */
object LyricsFile {
    const val SYNCED_EXT = "lrc"
    const val PLAIN_EXT = "txt"

    fun isSynced(fileNameOrPath: String): Boolean =
        fileNameOrPath.substringAfterLast('.', "").equals(SYNCED_EXT, ignoreCase = true)
}
