package com.kintmin.presentation.ui.playlist_add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.AddAudioMediaListToPlaylistUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListToAddTrackFlowUseCase
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.presentation.extension.matchKorean
import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemUiState
import com.kintmin.presentation.ui.playlist_add.list.toPlaylistAddListItemUiState
import com.kintmin.presentation.ui.playlist_add.navigation.PlaylistAddScreenRoute
import com.kintmin.presentation.util.Debounce
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaylistAddViewModel constructor(
    savedStateHandle: SavedStateHandle,
    fetchAudioMediaListFlowUseCase: FetchAudioMediaListToAddTrackFlowUseCase,
    fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    private val addAudioMediaListToPlaylistUseCase: AddAudioMediaListToPlaylistUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlaylistAddScreenRoute>()
    private val playlistId = route.playlistId

    private val _eventChannel = Channel<PlaylistAddEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PlaylistAddEvent> = _eventChannel.receiveAsFlow()

    private var isAdding = false

    private val _checkedItemIdList = MutableStateFlow(listOf<Int>())

    private val changeSearchTextDebounce = Debounce(200L)
    private val _searchText = MutableStateFlow("")

    val uiState: StateFlow<PlaylistAddUiState> = combine(
        fetchAudioMediaListFlowUseCase(playlistId),
        _checkedItemIdList,
        _searchText,
        fetchPlaylistFlowUseCase(playlistId).map { it.name }.onStart { emit(route.playlistNameSeed) },
    ) { mediaList, checkedIds, searchText, playlistName ->
        PlaylistAddUiState(
            audioList = mediaList.filter { it.audioMedia.name.matchKorean(searchText) }.map { data ->
                data.toPlaylistAddListItemUiState(
                    isChecked = data.audioMedia.id in checkedIds
                )
            },
            checkedItemCount = checkedIds.size,
            playlistName = playlistName,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlaylistAddUiState(playlistName = route.playlistNameSeed),
    )

    fun sendIntent(intent: PlaylistAddIntent) {
        when (intent) {
            is PlaylistAddIntent.OnChangeSearchText -> changeSearchText(intent.searchText)
            PlaylistAddIntent.OnClickAdd -> addAudioMediaListToPlaylist()
            is PlaylistAddIntent.OnClickAudioItem -> checkItem(intent.data)
            is PlaylistAddIntent.OnDragSelectItems -> _checkedItemIdList.update { intent.checkedIds.toList() }
            PlaylistAddIntent.OnClickClearCheckedItemList -> _checkedItemIdList.value = emptyList()
        }
    }

    private fun changeSearchText(newSearchText: String) {
        viewModelScope.launch {
            changeSearchTextDebounce {
                _searchText.update { newSearchText }
            }
        }
    }

    private fun addAudioMediaListToPlaylist() {
        val checkedIdList = _checkedItemIdList.value
        if (isAdding || checkedIdList.isEmpty()) return
        isAdding = true

        viewModelScope.launch {
            addAudioMediaListToPlaylistUseCase(playlistId, checkedIdList)
                .onSuccess {
                    _eventChannel.send(PlaylistAddEvent.ShowToast("${checkedIdList.size}곡을 추가했습니다."))
                    _eventChannel.send(PlaylistAddEvent.NavigateToBack)
                }
                .onFailure {
                    isAdding = false
                    _eventChannel.send(PlaylistAddEvent.ShowToast("곡을 추가하지 못했습니다. 다시 시도해 주세요."))
                }
        }
    }

    private fun checkItem(data: PlaylistAddListItemUiState) {
        _checkedItemIdList.update {
            if (it.contains(data.id)) {
                it - data.id
            } else {
                it + data.id
            }
        }
    }
}
