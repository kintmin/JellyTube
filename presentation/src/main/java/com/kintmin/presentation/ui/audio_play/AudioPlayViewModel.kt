package com.kintmin.presentation.ui.audio_play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.kintmin.domain.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.usecase.FetchPagingAudioMediaFlowUseCase
import com.kintmin.presentation.ui.audio_play.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioPlayViewModel @Inject constructor(
    private val fetchPagingAudioMediaFlowUseCase: FetchPagingAudioMediaFlowUseCase,
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<AudioPlayEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val audioPagingFlow = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest {
            fetchPagingAudioMediaFlowUseCase().map { pagingData ->
                pagingData.map { it.toUiModel() }
            }
        }
        .cachedIn(viewModelScope)

    fun refreshList() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun deleteAudioMedia(id: String) {
        viewModelScope.launch {
            deleteAudioMediaUseCase(id).onSuccess {
                refreshTrigger.emit(Unit)
            }.onFailure { exception ->
                _eventFlow.emit(AudioPlayEvent.ShowToast("삭제 실패: $exception"))
            }
        }
    }
}