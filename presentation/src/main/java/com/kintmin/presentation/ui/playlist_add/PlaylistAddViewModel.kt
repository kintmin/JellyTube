package com.kintmin.presentation.ui.playlist_add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.AddAudioMediaListToPlaylistUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListToAddTrackFlowUseCase
import com.kintmin.presentation.extension.matchKorean
import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemUiState
import com.kintmin.presentation.ui.playlist_add.list.toPlaylistAddListItemUiState
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.util.Debounce
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistAddViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    fetchAudioMediaListFlowUseCase: FetchAudioMediaListToAddTrackFlowUseCase,
    private val addAudioMediaListToPlaylistUseCase: AddAudioMediaListToPlaylistUseCase,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId

    private val _eventFlow = MutableSharedFlow<PlaylistAddEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _checkedItemIdList = MutableStateFlow(listOf<Int>())

    private val changeSearchTextDebounce = Debounce(200L)
    private val _searchText = MutableStateFlow("")

    val audioListFlow: StateFlow<List<PlaylistAddListItemUiState>> = combine(
        fetchAudioMediaListFlowUseCase(playlistId),
        _checkedItemIdList,
        _searchText,
    ) { mediaList, checkedIds, searchText ->
        mediaList.filter { it.audioMedia.name.matchKorean(searchText) }.map { data ->
            data.toPlaylistAddListItemUiState(
                isChecked = data.audioMedia.id in checkedIds
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkedItemCountFlow = _checkedItemIdList.map { list -> list.size }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun sendIntent(intent: PlaylistAddIntent) {
        when (intent) {
            is PlaylistAddIntent.OnChangeSearchText -> changeSearchText(intent.searchText)
            PlaylistAddIntent.OnClickAdd -> addAudioMediaListToPlaylist()
            is PlaylistAddIntent.OnClickAudioItem -> checkItem(intent.data)
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
        viewModelScope.launch {
            addAudioMediaListToPlaylistUseCase(playlistId, _checkedItemIdList.value)
            _checkedItemIdList.update { emptyList() }
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