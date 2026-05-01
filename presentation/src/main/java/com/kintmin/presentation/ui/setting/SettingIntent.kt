package com.kintmin.presentation.ui.setting

sealed interface SettingIntent {
    data object OnInit : SettingIntent
    data object OnClickShouldInsertAtTopOnDownloadTile : SettingIntent
    data class OnToggleShouldInsertAtTopOnDownload(val value: Boolean) : SettingIntent
    data object OnClickPlaylistIdOnDownloadTile : SettingIntent
    data object OnDismissPlaylistIdOnDownloadBottomSheet : SettingIntent
    data class OnSelectPlaylistIdOnDownload(val playlistId: Int) : SettingIntent
    data object OnClickStepTile : SettingIntent
    data object OnClickAppLogTile : SettingIntent
}
