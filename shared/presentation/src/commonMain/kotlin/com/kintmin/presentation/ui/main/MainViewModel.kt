package com.kintmin.presentation.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.app_setting.usecase.FetchHomeNoticeShownUseCase
import com.kintmin.domain.app_setting.usecase.MarkBackupNoticeShownUseCase
import com.kintmin.domain.app_setting.usecase.MarkNotificationNoticeShownUseCase
import com.kintmin.domain.app_update.usecase.CheckAppUpdateOncePerDayUseCase
import com.kintmin.domain.app_update.usecase.IgnoreAppUpdateUseCase
import com.kintmin.domain.backup.usecase.FetchBackupStatusFlowUseCase
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import com.kintmin.presentation.ui.main.navigation.mainScreenTypeMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class MainViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val checkAppUpdateOncePerDayUseCase: CheckAppUpdateOncePerDayUseCase,
    private val ignoreAppUpdateUseCase: IgnoreAppUpdateUseCase,
    private val fetchHomeNoticeShownUseCase: FetchHomeNoticeShownUseCase,
    private val markNotificationNoticeShownUseCase: MarkNotificationNoticeShownUseCase,
    private val markBackupNoticeShownUseCase: MarkBackupNoticeShownUseCase,
    fetchBackupStatusFlowUseCase: FetchBackupStatusFlowUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MainUiState(tabItem = savedStateHandle.toRoute<MainScreenRoute>(mainScreenTypeMap).tabItem)
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(fetchHomeNoticeShownUseCase(), fetchBackupStatusFlowUseCase()) { shown, backup ->
                !shown.backup && backup.folder == null
            }.collect { visible ->
                _uiState.update { it.copy(isBackupNoticeVisible = visible) }
            }
        }
    }

    fun sendIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.ChangeTab -> updateTabItem(intent.tab)
            is MainIntent.CheckAppUpdate -> checkAppUpdate(intent.currentVersionName)
            is MainIntent.CheckNotificationNotice -> checkNotificationNotice(intent.isGranted)
            MainIntent.CloseNotificationNotice -> closeNotificationNotice()
            MainIntent.DismissBackupNotice -> dismissBackupNotice()
            MainIntent.IgnoreAppUpdate -> ignoreAppUpdate()
            MainIntent.DismissAppUpdate -> _uiState.update { it.copy(availableUpdate = null) }
        }
    }

    private fun updateTabItem(newTabItem: MainTabItem) {
        _uiState.update { it.copy(tabItem = newTabItem) }
    }

    /**
     * 이미 허용돼 있으면 묻지 않고 물어본 것으로 적는다 — 그래야 사용자가 나중에 알림을 꺼도
     * 다시 묻지 않는다. 끈 것은 사용자의 선택이고, 되돌리는 길은 설정에 둔다.
     */
    private fun checkNotificationNotice(isGranted: Boolean) {
        viewModelScope.launch {
            if (fetchHomeNoticeShownUseCase().first().notification) return@launch
            if (isGranted) {
                markNotificationNoticeShownUseCase()
            } else {
                _uiState.update { it.copy(isNotificationNoticeVisible = true) }
            }
        }
    }

    private fun closeNotificationNotice() {
        _uiState.update { it.copy(isNotificationNoticeVisible = false) }
        viewModelScope.launch { markNotificationNoticeShownUseCase() }
    }

    private fun dismissBackupNotice() {
        _uiState.update { it.copy(isBackupNoticeVisible = false) }
        viewModelScope.launch { markBackupNoticeShownUseCase() }
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
