package com.kintmin.presentation.ui.playlist_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.usecase.FetchPlaylistFlowUseCase
import com.kintmin.platform.model.AudioPlayData
import com.kintmin.presentation.ui.playlist_detail.list_item.AudioPlayUiState
import com.kintmin.presentation.ui.playlist_detail.list_item.toParcelize
import com.kintmin.presentation.ui.playlist_detail.list_item.toUiModel
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.ui.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.playlist.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class AudioPlayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<AudioPlayEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId
    private var shouldClear = true
    private var isShuffled = false

    val playlistFlow: StateFlow<PlaylistItemUiState> = fetchPlaylistFlowUseCase(playlistId)
        .map { it.toUiModel() }
        .onEach { shouldClear = true }
        .catch {
            _eventFlow.emit(AudioPlayEvent.ShowToast("데이터 가져오기에 실패했습니다.\n다시 시도해주세요."))
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistItemUiState(
                id = playlistId,
                imageFileFullPath = "",
                name = "",
                description = "",
                playlistDuration = 0.seconds,
                audioMediaCount = 0,
            )
        )

    val audioListFlow: StateFlow<List<AudioPlayUiState>> = fetchAudioMediaListFlowUseCase(playlistId)
        .map { list -> list.map { it.toUiModel() } }
        .catch {
            _eventFlow.emit(AudioPlayEvent.ShowToast("데이터 가져오기에 실패했습니다.\n다시 시도해주세요."))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentPlayingAudioList: ArrayList<AudioPlayData> = arrayListOf()

    fun sendIntent(intent: AudioPlayIntent) {
        when (intent) {
            is AudioPlayIntent.OnClickAudioItem -> playAudio(intent.data)
            is AudioPlayIntent.OnClickDeleteAudioMedia -> deleteAudioMedia(intent.data.id)
            AudioPlayIntent.OnClickPlayAll -> setPlaylist(0)
            AudioPlayIntent.OnClickPlayShuffle -> setRandomPlaylist()
            AudioPlayIntent.OnClickAddAudioMediaInPlaylist -> {}
            AudioPlayIntent.OnClickEditPlaylist -> {}
            AudioPlayIntent.OnClickReorderAudioMediaList -> {}
            AudioPlayIntent.OnClickRepeatPlaylist -> {}
            AudioPlayIntent.OnClickNavigationBack -> {
                viewModelScope.launch {
                    _eventFlow.emit(AudioPlayEvent.NavigateToBack)
                }
            }
        }
    }

    private fun deleteAudioMedia(id: Int) {
        viewModelScope.launch {
            deleteAudioMediaUseCase(id).onFailure { exception ->
                _eventFlow.emit(AudioPlayEvent.ShowToast("삭제 실패: $exception"))
            }
        }
    }

    private fun setPlaylist(startIndex: Int) {
        viewModelScope.launch {
            if (isShuffled) {
                shouldClear = true
                isShuffled = false
            }

            if (shouldClear) {
                currentPlayingAudioList = ArrayList(audioListFlow.value.map { it.toParcelize() })
            }

            _eventFlow.emit(AudioPlayEvent.RegisterPlaylist(currentPlayingAudioList, startIndex, shouldClear))
            shouldClear = false
        }
    }

    private fun setRandomPlaylist() {
        viewModelScope.launch {
            currentPlayingAudioList = ArrayList(audioListFlow.value.map { it.toParcelize() }.shuffled())
            isShuffled = true

            _eventFlow.emit(AudioPlayEvent.RegisterPlaylist(currentPlayingAudioList, 0, true))
            shouldClear = false
        }
    }

    private fun playAudio(audioItem: AudioPlayUiState) {
        viewModelScope.launch {
            val targetIndex = if (currentPlayingAudioList.isEmpty()) {
                audioListFlow.value.indexOfFirst { it.id == audioItem.id }
            } else {
                currentPlayingAudioList.indexOfFirst { it.id == audioItem.id }
            }

            if (targetIndex == -1) {
                _eventFlow.emit(AudioPlayEvent.ShowToast("음원을 찾을 수 없습니다.\n새로고침해주세요."))
            } else {
                setPlaylist(targetIndex)
            }
        }
    }
}