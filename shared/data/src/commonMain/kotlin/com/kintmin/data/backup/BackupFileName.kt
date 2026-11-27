package com.kintmin.data.backup

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * 백업 파일 이름. **날짜가 앞에 와** 폴더를 열면 최근 것이 위로 오고, **설치 값이 뒤에 붙어**
 * 다시 설치한 앱이 옛 파일을 덮지 않는다.
 */
internal object BackupFileName {

    private const val PREFIX = "JellyTube_"
    private const val EXTENSION = ".json.gz"
    private const val DATE_LENGTH = 8

    fun build(createdAtMillis: Long, timeZone: TimeZone, installId: String): String =
        "$PREFIX${dateOf(createdAtMillis, timeZone)}_$installId$EXTENSION"

    fun parse(fileName: String): Parsed? {
        if (!fileName.startsWith(PREFIX) || !fileName.endsWith(EXTENSION)) return null
        val middle = fileName.substring(PREFIX.length, fileName.length - EXTENSION.length)
        val date = middle.take(DATE_LENGTH)
        val installId = middle.drop(DATE_LENGTH + 1)
        if (middle.getOrNull(DATE_LENGTH) != '_') return null
        if (!date.all(Char::isDigit) || date.length != DATE_LENGTH || installId.isEmpty()) return null
        return Parsed(date = date, installId = installId)
    }

    /**
     * 옛 고정 이름이면 그 순번. 같은 이름이 이미 있으면 제공자가 ` (번호)` 를 붙이는데,
     * MIME 이 octet-stream 이면 확장자를 몰라 이름 맨 뒤에 붙는다(`…json.gz (1)`).
     * 확장자 앞에 붙는 제공자도 있어 둘 다 본다. 번호가 클수록 나중에 쓰인 것이다.
     */
    fun legacyOrderOf(fileName: String): Int? {
        if (fileName == BackupFiles.LEGACY_FILE_NAME) return 0
        val stem = BackupFiles.LEGACY_FILE_NAME.substringBefore('.')
        val extension = BackupFiles.LEGACY_FILE_NAME.substring(stem.length)
        return numberedSuffix(fileName, "${BackupFiles.LEGACY_FILE_NAME} (", ")")
            ?: numberedSuffix(fileName, "$stem (", ")$extension")
    }

    /** 되돌릴 후보를 최근 것부터. 날짜를 모르는 옛 이름은 뒤에 둔다. */
    fun candidatesNewestFirst(fileNames: List<String>): List<String> {
        // 날짜가 하루 단위라 같은 날 두 설치가 쓰면 겹친다. 가르지 않으면 어느 것을 되돌릴지가
        // 플랫폼이 돌려준 목록 순서에 달린다.
        val dated = fileNames.mapNotNull { name -> parse(name)?.let { name to it } }
            .sortedWith(compareByDescending<Pair<String, Parsed>> { it.second.date }.thenBy { it.second.installId })
        val legacy = fileNames.mapNotNull { name -> legacyOrderOf(name)?.let { name to it } }
            .sortedByDescending { (_, order) -> order }
        return (dated.map { (name, _) -> name }) + legacy.map { (name, _) -> name }
    }

    private fun numberedSuffix(fileName: String, prefix: String, suffix: String): Int? {
        if (!fileName.startsWith(prefix) || !fileName.endsWith(suffix)) return null
        if (fileName.length <= prefix.length + suffix.length) return null
        val number = fileName.substring(prefix.length, fileName.length - suffix.length)
        return if (number.all(Char::isDigit)) number.toIntOrNull() else null
    }

    // ISO 는 2026-11-27 이다. 줄을 빼면 날짜순이 문자열순과 같아진다.
    private fun dateOf(millis: Long, timeZone: TimeZone): String =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date.toString().replace("-", "")

    data class Parsed(val date: String, val installId: String)
}
