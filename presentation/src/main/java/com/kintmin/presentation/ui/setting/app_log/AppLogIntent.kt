package com.kintmin.presentation.ui.setting.app_log

sealed interface AppLogIntent {
    data object OnInit : AppLogIntent
    data class OnClickLogDate(val date: String) : AppLogIntent
    data object OnRequestNextPage : AppLogIntent
}
