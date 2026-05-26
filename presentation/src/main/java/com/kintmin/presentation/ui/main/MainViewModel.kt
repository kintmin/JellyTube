package com.kintmin.presentation.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val importSharedAudioMediaUseCase: ImportSharedAudioMediaUseCase,
) : ViewModel() {
    private val _currentTabItem = MutableStateFlow(savedStateHandle.toRoute<MainScreenRoute>().tabItem)
    val tabItem = _currentTabItem.asStateFlow()

    private val eventChannel = Channel<MainScreenEvent>(capacity = 8)
    val eventFlow = eventChannel.receiveAsFlow()

    fun sendIntent(intent: MainScreenIntent) {
        when (intent) {
            is MainScreenIntent.ChangeTab -> updateTabItem(intent.tab)
            is MainScreenIntent.ImportMediaFiles -> importMediaFiles(intent.uriStrings)
        }
    }

    private fun updateTabItem(newTabItem: MainTabItem) {
        _currentTabItem.update { newTabItem }
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
                eventChannel.send(MainScreenEvent.ShowToast("${success}개 파일이 라이브러리에 추가되었습니다."))
            }
            errors.distinct().forEach { error ->
                eventChannel.send(MainScreenEvent.ShowToast(error))
            }
        }
    }
}

sealed interface MainScreenEvent {
    data class ShowToast(val message: String) : MainScreenEvent
}
