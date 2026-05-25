package com.kintmin.presentation.ui.main.floating_action

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainFloatingActionViewModel @Inject constructor(
    fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
    private val updatePlaylistIdOnDownloadUseCase: UpdatePlaylistIdOnDownloadUseCase,
) : ViewModel() {

    private val isPlaylistBottomSheetVisible = MutableStateFlow(false)

    private val _eventFlow = MutableSharedFlow<MainFloatingActionEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val uiState: StateFlow<MainFloatingActionUiState> = combine(
        fetchPlaylistIdOnDownloadFlowUseCase(),
        fetchAllPlaylistFlowUseCase(),
        isPlaylistBottomSheetVisible,
    ) { playlistIdOnDownload, playlistList, isBottomSheetVisible ->
        val selectablePlaylistList = playlistList
            .filterNot { it.id == Playlist.TOTAL }
            .map { it.id to it.name }
            .toMutableList()
            .apply {
                if (none { it.first == Playlist.UNCATEGORIZED }) {
                    add(0, Playlist.UNCATEGORIZED to "미분류")
                }
            }
            .distinctBy { it.first }
            .map { (id, name) ->
                DownloadPlaylistUiState(
                    id = id,
                    name = toDisplayPlaylistName(id, name),
                    isSelected = id == playlistIdOnDownload,
                )
            }

        MainFloatingActionUiState(
            playlistIdOnDownload = playlistIdOnDownload,
            playlistIdOnDownloadName = selectablePlaylistList.firstOrNull { it.id == playlistIdOnDownload }?.name ?: "기본",
            selectablePlaylistList = selectablePlaylistList,
            isPlaylistBottomSheetVisible = isBottomSheetVisible,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainFloatingActionUiState(),
    )

    fun sendIntent(intent: MainFloatingActionIntent) {
        when (intent) {
            is MainFloatingActionIntent.OnClickDownload -> {
                handleClickDownload(intent.url)
            }
            MainFloatingActionIntent.OnClickPlaylistButton -> {
                isPlaylistBottomSheetVisible.value = true
            }
            MainFloatingActionIntent.OnDismissPlaylistBottomSheet -> {
                isPlaylistBottomSheetVisible.value = false
            }
            is MainFloatingActionIntent.OnSelectPlaylist -> {
                updatePlaylistIdOnDownload(intent.playlistId)
            }
        }
    }

    private fun updatePlaylistIdOnDownload(playlistId: Int) {
        viewModelScope.launch {
            updatePlaylistIdOnDownloadUseCase(playlistId)
            isPlaylistBottomSheetVisible.value = false
        }
    }

    private fun handleClickDownload(url: String) {
        viewModelScope.launch {
            if (url.isDownloadableYoutubeUrl()) {
                _eventFlow.emit(MainFloatingActionEvent.Download(url))
            } else {
                _eventFlow.emit(MainFloatingActionEvent.ShowToast("다운받을 영상을 골라주세요."))
            }
        }
    }

    private fun String.isDownloadableYoutubeUrl(): Boolean {
        val uri = runCatching { toUri() }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        val firstPathSegment = uri.pathSegments.firstOrNull() ?: return false

        return host in DOWNLOADABLE_YOUTUBE_HOSTS && firstPathSegment in DOWNLOADABLE_YOUTUBE_PATHS
    }

    private fun toDisplayPlaylistName(playlistId: Int, playlistName: String): String {
        return if (playlistId == Playlist.UNCATEGORIZED) "기본" else playlistName
    }

    private companion object {
        val DOWNLOADABLE_YOUTUBE_HOSTS = setOf(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "youtu.be",
        )
        val DOWNLOADABLE_YOUTUBE_PATHS = setOf("watch", "shorts")
    }
}
