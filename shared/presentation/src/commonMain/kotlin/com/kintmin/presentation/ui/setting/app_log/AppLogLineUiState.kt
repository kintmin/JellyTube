package com.kintmin.presentation.ui.setting.app_log

/** 날짜는 화면에서 이미 골랐으므로 담지 않는다. */
data class AppLogLineUiState(
    val time: String?,
    val type: String?,
    val message: String,
)

private val APP_LOG_LINE_REGEX = Regex("""^\[\d{4}-\d{2}-\d{2} (\d{2}:\d{2}:\d{2}\.\d{3})] (?:\[([A-Z_]+)] )?(.*)$""")

fun parseAppLogLine(line: String): AppLogLineUiState {
    val match = APP_LOG_LINE_REGEX.matchEntire(line)
        ?: return AppLogLineUiState(time = null, type = null, message = line)
    val (time, type, message) = match.destructured
    return AppLogLineUiState(time = time, type = type.ifEmpty { null }, message = message)
}
