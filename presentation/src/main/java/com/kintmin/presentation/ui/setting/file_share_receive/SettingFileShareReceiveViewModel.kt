package com.kintmin.presentation.ui.setting.file_share_receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.platform.service.FileShareForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingFileShareReceiveViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingFileShareReceiveUiState())
    val uiState: StateFlow<SettingFileShareReceiveUiState> = _uiState.asStateFlow()

    init {
        observeServerStatus()
    }

    private fun observeServerStatus() {
        viewModelScope.launch {
            FileShareForegroundService.isRunning.collect { isRunning ->
                _uiState.update {
                    it.copy(serverStatus = if (isRunning) ServerStatus.RUNNING else ServerStatus.STOPPED)
                }
            }
        }
    }
}
