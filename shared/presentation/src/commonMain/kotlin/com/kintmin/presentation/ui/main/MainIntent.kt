package com.kintmin.presentation.ui.main

sealed interface MainIntent {
    data class ChangeTab(val tab: MainTabItem) : MainIntent
    data class CheckAppUpdate(val currentVersionName: String) : MainIntent

    /** 알림 권한이 이미 있으면 묻지 않는다. 권한 판정은 화면이 하고 결과만 넘긴다. */
    data class CheckNotificationNotice(val isGranted: Boolean) : MainIntent

    /** 물어본 것으로 친다 — 허용했든 미뤘든 다시 띄우지 않는다. */
    data object CloseNotificationNotice : MainIntent
    data object DismissBackupNotice : MainIntent

    data object IgnoreAppUpdate : MainIntent
    data object DismissAppUpdate : MainIntent
}
