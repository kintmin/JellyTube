package com.kintmin.presentation.ui.setting.app_log

sealed interface AppLogEvent {
    data class ShareLogFiles(val fileFullPaths: List<String>) : AppLogEvent
}
