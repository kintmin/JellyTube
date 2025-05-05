package com.kintmin.presentation.ui.playlist_edit.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.usecase.FetchPlaylistFlowUseCase
import com.kintmin.domain.usecase.UpdatePlaybackSequenceUseCase
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderUiState
import com.kintmin.presentation.ui.playlist_edit.header.toPlaylistEditHeaderUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistEditListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val updatePlaybackSequenceUseCase: UpdatePlaybackSequenceUseCase,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId

    private val _eventFlow = MutableSharedFlow<PlaylistEditListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val audioMediaListFlow =
        fetchAudioMediaListFlowUseCase(playlistId).map { list -> list.map { it.toPlaylistEditListItemUiState() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkedItemCountFlow = audioMediaListFlow.map { list -> list.count { it.isChecked } }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val headerDataFlow = fetchPlaylistFlowUseCase(playlistId).map { it.toPlaylistEditHeaderUiState() }
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
            is PlaylistEditListIntent.OnClickEditCheck -> TODO()
            is PlaylistEditListIntent.ReorderAudioItem -> updatePlaybackSequence(intent.reorderData, intent.targetData)
        }
    }

    private fun deleteAudioMediaInPlaylist(id: Int) {}

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

    private fun triggerEvent(newEvent: PlaylistEditListEvent) {
        viewModelScope.launch {
            _eventFlow.emit(newEvent)
        }
    }
}

