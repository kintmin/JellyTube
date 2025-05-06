package com.kintmin.presentation.ui.playlist_edit.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.usecase.DeleteAudioMediaListFromPlaylistUseCase
import com.kintmin.domain.usecase.DeleteAudioMediaListUseCase
import com.kintmin.domain.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.usecase.FetchPlaylistFlowUseCase
import com.kintmin.domain.usecase.UpdatePlaybackSequenceUseCase
import com.kintmin.domain.usecase.UpdatePlaylistDescriptionUseCase
import com.kintmin.domain.usecase.UpdatePlaylistTitleUseCase
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderUiState
import com.kintmin.presentation.ui.playlist_edit.header.toPlaylistEditHeaderUiState
import com.kintmin.presentation.util.Debounce
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistEditListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val updatePlaybackSequenceUseCase: UpdatePlaybackSequenceUseCase,
    private val updatePlaylistDescriptionUseCase: UpdatePlaylistDescriptionUseCase,
    private val updatePlaylistTitleUseCase: UpdatePlaylistTitleUseCase,
    private val deleteAudioMediaListUseCase: DeleteAudioMediaListUseCase,
    private val deleteAudioMediaListFromPlaylistUseCase: DeleteAudioMediaListFromPlaylistUseCase,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId
    val isBasePlaylist = playlistId == Playlist.TOTAL || playlistId == Playlist.UNCATEGORIZED

    private val _checkedItemIdList = MutableStateFlow(listOf<Int>())

    val audioMediaListFlow: StateFlow<List<PlaylistEditListItemUiState>> =
        combine(
            fetchAudioMediaListFlowUseCase(playlistId),
            _checkedItemIdList
        ) { mediaList, checkedIds ->
            mediaList.map { audioMedia ->
                audioMedia.toPlaylistEditListItemUiState(
                    isChecked = audioMedia.id in checkedIds
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkedItemCountFlow = audioMediaListFlow.map { list -> list.count { it.isChecked } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val headerDataFlow = fetchPlaylistFlowUseCase(playlistId).map { it.toPlaylistEditHeaderUiState() }
        .distinctUntilChangedBy { it.id }
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

    private val updatePlaylistTitleDebounce = Debounce(500L)
    private val updatePlaylistDescriptionDebounce = Debounce(500L)

    fun sendIntent(intent: PlaylistEditListIntent) {
        when (intent) {
            is PlaylistEditListIntent.OnClickEditCheck -> checkItem(intent.data)
            is PlaylistEditListIntent.ReorderAudioItem -> updatePlaybackSequence(intent.reorderData, intent.targetData)
            PlaylistEditListIntent.OnClickClearCheckedItemList -> clearCheckedItemList()
            PlaylistEditListIntent.OnClickDeleteAudioMediaListInPlaylist -> deleteAudioMediaListInPlaylist()
            PlaylistEditListIntent.OnClickFullDeleteAudioMediaList -> deleteFullAudioMediaList()
            is PlaylistEditListIntent.OnEditPlaylistTitle -> updatePlaylistTitle(intent.title)
            is PlaylistEditListIntent.OnEditPlaylistDescription -> updatePlaylistDescription(intent.description)
        }
    }

    private fun deleteAudioMediaListInPlaylist() {
        viewModelScope.launch {
            deleteAudioMediaListFromPlaylistUseCase(playlistId, _checkedItemIdList.value)
            _checkedItemIdList.update { emptyList() }
        }
    }

    private fun deleteFullAudioMediaList() {
        viewModelScope.launch {
            deleteAudioMediaListUseCase(playlistId, _checkedItemIdList.value)
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

        val isForward = reorderData.sequence > targetData.sequence
        val newSequence = if (isForward) {
            targetData.sequence
        } else {
            targetData.sequence + 1
        }

        viewModelScope.launch {
            updatePlaybackSequenceUseCase(playlistId, reorderData.id, newSequence)
        }
    }

    private fun updatePlaylistTitle(newTitle: String) {
        viewModelScope.launch {
            updatePlaylistTitleDebounce(viewModelScope) {
                updatePlaylistTitleUseCase(playlistId, newTitle)
            }
        }
    }

    private fun updatePlaylistDescription(newDescription: String) {
        viewModelScope.launch {
            updatePlaylistDescriptionDebounce(viewModelScope) {
                updatePlaylistDescriptionUseCase(playlistId, newDescription)
            }
        }
    }
}

