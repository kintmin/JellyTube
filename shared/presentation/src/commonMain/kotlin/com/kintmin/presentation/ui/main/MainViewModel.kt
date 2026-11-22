package com.kintmin.presentation.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.app_update.usecase.CheckAppUpdateOncePerDayUseCase
import com.kintmin.domain.app_update.usecase.IgnoreAppUpdateUseCase
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import com.kintmin.presentation.ui.main.navigation.mainScreenTypeMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class MainViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val checkAppUpdateOncePerDayUseCase: CheckAppUpdateOncePerDayUseCase,
    private val ignoreAppUpdateUseCase: IgnoreAppUpdateUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MainUiState(tabItem = savedStateHandle.toRoute<MainScreenRoute>(mainScreenTypeMap).tabItem)
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun sendIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.ChangeTab -> updateTabItem(intent.tab)
            is MainIntent.CheckAppUpdate -> checkAppUpdate(intent.currentVersionName)
            MainIntent.IgnoreAppUpdate -> ignoreAppUpdate()
            MainIntent.DismissAppUpdate -> _uiState.update { it.copy(availableUpdate = null) }
        }
    }

    private fun updateTabItem(newTabItem: MainTabItem) {
        _uiState.update { it.copy(tabItem = newTabItem) }
    }

    private fun checkAppUpdate(currentVersionName: String) {
        viewModelScope.launch {
            val release = checkAppUpdateOncePerDayUseCase(
                currentVersionName = currentVersionName,
                nowMillis = Clock.System.now().toEpochMilliseconds(),
            ).getOrNull() ?: return@launch
            _uiState.update { it.copy(availableUpdate = release) }
        }
    }

    private fun ignoreAppUpdate() {
        val release = _uiState.value.availableUpdate ?: return
        _uiState.update { it.copy(availableUpdate = null) }
        viewModelScope.launch { ignoreAppUpdateUseCase(release.versionName) }
    }
}
