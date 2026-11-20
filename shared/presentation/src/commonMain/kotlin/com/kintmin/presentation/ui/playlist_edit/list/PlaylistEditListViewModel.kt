package com.kintmin.presentation.ui.playlist_edit.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.DeleteAudioTrackListUseCase
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaListUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.domain.audio_track.usecase.UpdateTrackSequenceUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistDescriptionUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistTitleUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageUseCase
import com.kintmin.domain.playlist.usecase.ResetPlaylistImageUseCase
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderUiState
import com.kintmin.presentation.ui.playlist_edit.header.toPlaylistEditHeaderUiState
import com.kintmin.presentation.ui.playlist_edit.navigation.PlaylistEditScreenRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaylistEditListViewModel constructor(
    savedStateHandle: SavedStateHandle,
    fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val updateTrackSequenceUseCase: UpdateTrackSequenceUseCase,
    private val updatePlaylistDescriptionUseCase: UpdatePlaylistDescriptionUseCase,
    private val updatePlaylistTitleUseCase: UpdatePlaylistTitleUseCase,
    private val deleteAudioMediaListUseCase: DeleteAudioMediaListUseCase,
    private val deleteAudioTrackListUseCase: DeleteAudioTrackListUseCase,
    private val updatePlaylistImageUseCase: UpdatePlaylistImageUseCase,
    private val resetPlaylistImageUseCase: ResetPlaylistImageUseCase,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistEditScreenRoute>().playlistId

    private val _eventChannel = Channel<PlaylistEditListEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PlaylistEditListEvent> = _eventChannel.receiveAsFlow()

    private val _checkedItemIdList = MutableStateFlow(listOf<Int>())

    val audioMediaListFlow: StateFlow<List<PlaylistEditListItemUiState>> =
        combine(
            fetchAudioMediaListFlowUseCase(playlistId),
            _checkedItemIdList
        ) { mediaList, checkedIds ->
            mediaList.map { data ->
                data.toPlaylistEditListItemUiState(
                    isChecked = data.audioMedia.id in checkedIds
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkedItemCountFlow = audioMediaListFlow.map { list -> list.count { it.isChecked } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val headerDataFlow = fetchPlaylistFlowUseCase(playlistId).map { it.toPlaylistEditHeaderUiState() }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistEditHeaderUiState(
                id = playlistId,
                imageFileFullPath = null,
                name = "",
                description = "",
                audioMediaCount = 0,
                playTimeDuration = "",
            )
        )

    fun sendIntent(intent: PlaylistEditListIntent) {
        when (intent) {
            is PlaylistEditListIntent.OnClickEditCheck -> checkItem(intent.data)
            is PlaylistEditListIntent.ReorderAudioItem -> updatePlaybackSequence(intent.reorderData, intent.targetData)
            is PlaylistEditListIntent.OnDragSelectItems -> _checkedItemIdList.update { intent.checkedIds.toList() }
            PlaylistEditListIntent.OnClickClearCheckedItemList -> clearCheckedItemList()
            PlaylistEditListIntent.OnClickDeleteAudioMediaListInPlaylist -> deleteAudioMediaListInPlaylist()
            PlaylistEditListIntent.OnClickFullDeleteAudioMediaList -> deleteFullAudioMediaList()
            is PlaylistEditListIntent.OnEditPlaylistTitle -> updatePlaylistTitle(intent.title)
            is PlaylistEditListIntent.OnEditPlaylistDescription -> updatePlaylistDescription(intent.description)
            is PlaylistEditListIntent.OnPickPlaylistImage -> updatePlaylistImage(intent.imageData)
            PlaylistEditListIntent.OnClickResetPlaylistImage -> resetPlaylistImage()
        }
    }

    private fun deleteAudioMediaListInPlaylist() {
        viewModelScope.launch {
            deleteAudioTrackListUseCase(playlistId, _checkedItemIdList.value)
            _checkedItemIdList.update { emptyList() }
        }
    }

    private fun deleteFullAudioMediaList() {
        viewModelScope.launch {
            val sourceList = audioMediaListFlow.value.filter {
                it.id in _checkedItemIdList.value
            }.map {
                it.source
            }
            deleteAudioMediaListUseCase(_checkedItemIdList.value, sourceList)
            _checkedItemIdList.update { emptyList() }
        }
    }

    private fun checkItem(data: PlaylistEditListItemUiState) {
        _checkedItemIdList.update {
            if (it.contains(data.id)) {
                it - data.id
            } else {
                it + data.id
            }
        }
    }

    private fun clearCheckedItemList() {
        _checkedItemIdList.update { emptyList() }
    }

    private fun updatePlaybackSequence(
        reorderData: PlaylistEditListItemUiState,
        targetData: PlaylistEditListItemUiState,
    ) {
        if (reorderData.id == targetData.id) return

        viewModelScope.launch {
            updateTrackSequenceUseCase(playlistId, reorderData.id, reorderData.sequence, targetData.sequence)
        }
    }

    // 디바운스하면 대기 중에 화면을 나갈 때 viewModelScope 와 함께 마지막 입력이 취소된다.
    // 대신 매 입력을 저장하되, 쓰기가 IO 에서 앞지르지 않게 입력 순서대로 줄 세운다(Mutex 는 FIFO).
    private val playlistInfoWriteMutex = Mutex()

    private fun updatePlaylistTitle(newTitle: String) {
        viewModelScope.launch {
            playlistInfoWriteMutex.withLock { updatePlaylistTitleUseCase(playlistId, newTitle) }
        }
    }

    private fun updatePlaylistDescription(newDescription: String) {
        viewModelScope.launch {
            playlistInfoWriteMutex.withLock { updatePlaylistDescriptionUseCase(playlistId, newDescription) }
        }
    }

    private fun updatePlaylistImage(imageData: ByteArray) {
        viewModelScope.launch {
            updatePlaylistImageUseCase(playlistId, imageData).onFailure {
                _eventChannel.send(PlaylistEditListEvent.ShowToast("사진을 불러오지 못했습니다. 다른 사진을 골라 주세요."))
            }
        }
    }

    private fun resetPlaylistImage() {
        viewModelScope.launch {
            resetPlaylistImageUseCase(playlistId).onFailure {
                _eventChannel.send(PlaylistEditListEvent.ShowToast("기본 썸네일로 되돌리지 못했습니다."))
            }
        }
    }
}
