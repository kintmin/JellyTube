package com.kintmin.presentation.ui.setting.file_share_receive

sealed interface SettingFileShareReceiveIntent {
    data object OnClickStartServer : SettingFileShareReceiveIntent
    data object OnClickStopServer : SettingFileShareReceiveIntent
}
