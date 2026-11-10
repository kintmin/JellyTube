package com.kintmin.presentation.ui.setting.app_log

data class AppLogUiState(
    val logDateList: List<String> = emptyList(),
    val selectedLogDate: String? = null,
    val logLineList: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val hasNextPage: Boolean = false,
)
