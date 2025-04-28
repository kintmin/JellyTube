package com.kintmin.presentation.ui.audio_play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.usecase.FetchPlaylistFlowUseCase
import com.kintmin.presentation.ui.audio_play.list_item.AudioPlayUiState
import com.kintmin.presentation.ui.audio_play.list_item.toTryParcelize
import com.kintmin.presentation.ui.audio_play.list_item.toUiModel
import com.kintmin.presentation.ui.audio_play.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.ui.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.playlist.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

    val playlistFlow: StateFlow<PlaylistItemUiState> = fetchPlaylistFlowUseCase(playlistId)
        .map { it.toUiModel() }
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
            val audioMediaList = ArrayList(audioListFlow.value.mapNotNull { it.toTryParcelize().getOrNull() })
            _eventFlow.emit(AudioPlayEvent.RegisterPlaylist(audioMediaList, startIndex, false))
        }
    }

    private fun setRandomPlaylist() {
        viewModelScope.launch {
            val randomAudioMediaList =
                ArrayList(audioListFlow.value.mapNotNull { it.toTryParcelize().getOrNull() }.shuffled())
            _eventFlow.emit(AudioPlayEvent.RegisterPlaylist(randomAudioMediaList, 0, true))
        }
    }

    private fun playAudio(audioItem: AudioPlayUiState) {
        viewModelScope.launch {
            audioListFlow.value.firstOrNull { it.id == audioItem.id }
            audioItem.toTryParcelize().onSuccess { data ->
                val targetIndex = audioListFlow.value.indexOfFirst { it.audioFileFullPath == data.audioFileFullPath }
                if (targetIndex == -1) {
                    _eventFlow.emit(AudioPlayEvent.ShowToast("음원을 찾을 수 없습니다.\n새로고침해주세요."))
                } else {
                    setPlaylist(targetIndex)
                }
            }.onFailure {
                _eventFlow.emit(AudioPlayEvent.ShowToast("음원을 찾을 수 없습니다.\n삭제 후 다시 다운해주세요."))
            }
        }
    }
}