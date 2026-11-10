package com.kintmin.presentation.ui.setting.file_share_receive

data class SettingFileShareReceiveUiState(
    val serverStatus: ServerStatus = ServerStatus.STOPPED,
)

enum class ServerStatus {
    STOPPED,
    RUNNING,
}
