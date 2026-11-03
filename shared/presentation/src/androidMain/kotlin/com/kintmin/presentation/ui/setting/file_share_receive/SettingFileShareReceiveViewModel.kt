package com.kintmin.presentation.ui.setting.file_share_receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.presentation.port.FileShareServerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingFileShareReceiveViewModel constructor(
    private val fileShareServerController: FileShareServerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingFileShareReceiveUiState())
    val uiState: StateFlow<SettingFileShareReceiveUiState> = _uiState.asStateFlow()

    init {
        observeServerStatus()
    }

    fun sendIntent(intent: SettingFileShareReceiveIntent) {
        when (intent) {
            SettingFileShareReceiveIntent.OnClickStartServer -> fileShareServerController.start()
            SettingFileShareReceiveIntent.OnClickStopServer -> fileShareServerController.stop()
        }
    }

    private fun observeServerStatus() {
        viewModelScope.launch {
            fileShareServerController.isRunning.collect { isRunning ->
                _uiState.update {
                    it.copy(serverStatus = if (isRunning) ServerStatus.RUNNING else ServerStatus.STOPPED)
                }
            }
        }
    }
}

