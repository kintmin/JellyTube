package com.kintmin.presentation.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import com.kintmin.presentation.ui.main.navigation.mainScreenTypeMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val importSharedAudioMediaUseCase: ImportSharedAudioMediaUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MainUiState(tabItem = savedStateHandle.toRoute<MainScreenRoute>(mainScreenTypeMap).tabItem)
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<MainEvent>(capacity = 8)
    val eventFlow = eventChannel.receiveAsFlow()

    fun sendIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.ChangeTab -> updateTabItem(intent.tab)
            is MainIntent.ImportMediaFiles -> importMediaFiles(intent.uriStrings)
        }
    }

    private fun updateTabItem(newTabItem: MainTabItem) {
        _uiState.update { it.copy(tabItem = newTabItem) }
    }

    private fun importMediaFiles(uriStrings: List<String>) {
        if (uriStrings.isEmpty()) return
        viewModelScope.launch {
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

            if (success > 0) {
                eventChannel.send(MainEvent.ShowToast("${success}개 파일이 라이브러리에 추가되었습니다."))
            }
            errors.distinct().forEach { error ->
                eventChannel.send(MainEvent.ShowToast(error))
            }
        }
    }
}
