package com.kintmin.presentation.ui.audio_play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kintmin.domain.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.usecase.FetchAudioMediaListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioPlayViewModel @Inject constructor(
    private val fetchAudioMediaListUseCase: FetchAudioMediaListUseCase,
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<AudioPlayEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val audioPagingFlow = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest {
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    maxSize = 100,
                ),
                pagingSourceFactory = {
                    AudioPagingSource(fetchAudioMediaListUseCase)
                },
                initialKey = null,
            ).flow
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