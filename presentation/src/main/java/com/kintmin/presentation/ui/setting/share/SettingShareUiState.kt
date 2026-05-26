package com.kintmin.presentation.ui.setting.share

data class SettingShareUiState(
    val isLoading: Boolean = false,
    val successCount: Int = 0,
    val errors: List<String> = emptyList(),
)
