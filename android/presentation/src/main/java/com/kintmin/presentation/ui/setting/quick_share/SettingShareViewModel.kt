package com.kintmin.presentation.ui.setting.quick_share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingShareViewModel constructor(
    private val importSharedAudioMediaUseCase: ImportSharedAudioMediaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingShareUiState())
    val uiState: StateFlow<SettingShareUiState> = _uiState.asStateFlow()

    fun onFilesSelected(uriStrings: List<String>) {
        if (uriStrings.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, successCount = 0, errors = emptyList()) }
            var success = 0
            val errors = mutableListOf<String>()
            uriStrings.forEach { uriString ->
                importSharedAudioMediaUseCase(uriString)
                    .onSuccess { success++ }
                    .onFailure { e ->
                        when (e) {
                            is AlreadyDownloadedMedia -> errors.add("이미 저장된 파일입니다.")
                            else -> errors.add(e.message ?: "알 수 없는 오류")
                        }
                    }
            }
            _uiState.update {
                it.copy(isLoading = false, successCount = success, errors = errors)
            }
        }
    }
}

