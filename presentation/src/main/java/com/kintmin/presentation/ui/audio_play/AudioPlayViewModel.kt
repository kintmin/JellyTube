package com.kintmin.presentation.ui.audio_play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.usecase.FetchAudioMediaListUseCase
import com.kintmin.domain.usecase.FetchPagingAudioMediaFlowUseCase
import com.kintmin.platform.model.AudioPlayData
import com.kintmin.presentation.ui.audio_play.list_item.AudioPlayUiState
import com.kintmin.presentation.ui.audio_play.list_item.toTryParcelize
import com.kintmin.presentation.ui.audio_play.list_item.toUiModel
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
    private val fetchAudioMediaListUseCase: FetchAudioMediaListUseCase,
    private val fetchPagingAudioMediaFlowUseCase: FetchPagingAudioMediaFlowUseCase,
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<AudioPlayEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val isBasePlaylist = true

    @OptIn(ExperimentalCoroutinesApi::class)
    val audioPagingFlow = _refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest {
            fetchPagingAudioMediaFlowUseCase().map { pagingData ->
                pagingData.map { it.toUiModel() }
            }
        }
        .cachedIn(viewModelScope)

    private var cachedAudioDataList = ArrayList<AudioPlayData>()

    fun sendIntent(intent: AudioPlayIntent) {
        when(intent) {
            is AudioPlayIntent.OnClickAudioItem -> playAudio(intent.data)
            is AudioPlayIntent.OnClickDeleteAudioMedia -> deleteAudioMedia(intent.data.id)
            AudioPlayIntent.PullToRefreshAudioList -> refreshList()
            AudioPlayIntent.OnClickPlayAll -> setPlaylist()
            AudioPlayIntent.OnClickAddAudioMediaInPlaylist -> {}
            AudioPlayIntent.OnClickEditPlaylist -> {}
            AudioPlayIntent.OnClickPlayShuffle -> {}
            AudioPlayIntent.OnClickReorderAudioMediaList -> {}
        }
    }

    private fun refreshList() {
        viewModelScope.launch {
            _refreshTrigger.emit(Unit)
        }
    }

    private fun deleteAudioMedia(id: String) {
        viewModelScope.launch {
            deleteAudioMediaUseCase(id).onSuccess {
                _refreshTrigger.emit(Unit)
            }.onFailure { exception ->
                _eventFlow.emit(AudioPlayEvent.ShowToast("삭제 실패: $exception"))
            }
        }
    }

    private fun setPlaylist() {
        viewModelScope.launch {
            val dataList = fetchAudioMediaList().getOrNull() ?: return@launch
            if (dataList.size != cachedAudioDataList.size) {
                val diffCount = dataList.size - cachedAudioDataList.size
                _eventFlow.emit(AudioPlayEvent.ShowToast("${diffCount}개의 재생목록 등록을 실패했습니다."))
            }
            _eventFlow.emit(AudioPlayEvent.RegisterPlaylist(cachedAudioDataList, 0))
        }
    }

    private fun playAudio(audioItem: AudioPlayUiState) {
        viewModelScope.launch {
            audioItem.toTryParcelize().onSuccess { data ->
                var targetIndex = cachedAudioDataList.indexOfFirst { it.audioFileFullPath == data.audioFileFullPath }

                if (targetIndex == -1) {
                    fetchAudioMediaList().getOrElse { return@launch }
                    targetIndex = cachedAudioDataList.indexOfFirst { it.audioFileFullPath == data.audioFileFullPath }
                }

                if (targetIndex == -1) {
                    _eventFlow.emit(AudioPlayEvent.RegisterPlaylist(cachedAudioDataList, 0))
                    _eventFlow.emit(AudioPlayEvent.ShowToast("음원을 찾을 수 없습니다."))
                } else {
                    _eventFlow.emit(AudioPlayEvent.RegisterPlaylist(cachedAudioDataList, targetIndex))
                }
            }.onFailure {
                _eventFlow.emit(AudioPlayEvent.ShowToast("음원을 찾을 수 없습니다.\n삭제 후 다시 다운해주세요."))
            }
        }
    }

    private suspend fun fetchAudioMediaList(): Result<List<AudioMedia>> {
        return fetchAudioMediaListUseCase().onSuccess { dataList ->
            cachedAudioDataList = ArrayList(dataList.mapNotNull { it.toTryParcelize().getOrNull() })
        }.onFailure {
            _eventFlow.emit(AudioPlayEvent.ShowToast("재생목록 등록에 실패했습니다.\n다시 시도해주세요."))
        }
    }
}