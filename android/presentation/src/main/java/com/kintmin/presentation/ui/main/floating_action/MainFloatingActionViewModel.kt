package com.kintmin.presentation.ui.main.floating_action

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainFloatingActionViewModel constructor(
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
        // 다운로드 대상은 USER 플레이리스트 + 미분류(기본)만. 전체/즐겨찾기는 제외.
        val uncategorizedId = playlistList.firstOrNull { it.type == PlaylistType.UNCATEGORIZED }?.id
        val effectiveTargetId =
            playlistIdOnDownload?.takeIf { id -> playlistList.any { it.id == id } } ?: uncategorizedId

        val selectablePlaylistList = playlistList
            .filter { it.type == PlaylistType.USER || it.type == PlaylistType.UNCATEGORIZED }
            .map { playlist ->
                DownloadPlaylistUiState(
                    id = playlist.id,
                    name = if (playlist.type == PlaylistType.UNCATEGORIZED) "기본" else playlist.name,
                    isSelected = playlist.id == effectiveTargetId,
                )
            }

        MainFloatingActionUiState(
            playlistIdOnDownload = effectiveTargetId,
            playlistIdOnDownloadName = selectablePlaylistList.firstOrNull { it.isSelected }?.name ?: "기본",
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

