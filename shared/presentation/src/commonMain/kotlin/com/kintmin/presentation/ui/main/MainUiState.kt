package com.kintmin.presentation.ui.main

import com.kintmin.domain.app_update.model.AppRelease

data class MainUiState(
    val tabItem: MainTabItem,
    val availableUpdate: AppRelease? = null,
)
