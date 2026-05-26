package com.kintmin.presentation.ui.setting.file_share_receive

import android.content.Context
import androidx.lifecycle.ViewModel
import com.kintmin.platform.service.FileShareForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingFileShareReceiveViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingFileShareReceiveUiState())
    val uiState: StateFlow<SettingFileShareReceiveUiState> = _uiState.asStateFlow()

    fun onStartServer() {
        FileShareForegroundService.startService(appContext)
        _uiState.update { it.copy(serverStatus = ServerStatus.RUNNING) }
    }

    fun onStopServer() {
        FileShareForegroundService.stopService(appContext)
        _uiState.update { it.copy(serverStatus = ServerStatus.IDLE) }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
