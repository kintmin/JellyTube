package com.kintmin.presentation.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.app_setting.usecase.UpdateShouldInsertAtTopOnDownloadUseCase
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
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
class SettingViewModel @Inject constructor(
    fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
    private val updateShouldInsertAtTopOnDownloadUseCase: UpdateShouldInsertAtTopOnDownloadUseCase,
    private val updatePlaylistIdOnDownloadUseCase: UpdatePlaylistIdOnDownloadUseCase,
) : ViewModel() {

    private val isPlaylistIdOnDownloadBottomSheetVisible = MutableStateFlow(false)
    private val _eventFlow = MutableSharedFlow<SettingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val uiState: StateFlow<SettingUiState> = combine(
        fetchShouldInsertAtTopOnDownloadFlowUseCase(),
        fetchPlaylistIdOnDownloadFlowUseCase(),
        fetchAllPlaylistFlowUseCase(),
        isPlaylistIdOnDownloadBottomSheetVisible,
    ) { shouldInsertAtTopOnDownload, playlistIdOnDownload, playlistList, isBottomSheetVisible ->
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

        SettingUiState(
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
            playlistIdOnDownload = playlistIdOnDownload,
            playlistIdOnDownloadName = selectablePlaylistList.firstOrNull { it.id == playlistIdOnDownload }?.name ?: "기본",
            selectablePlaylistList = selectablePlaylistList,
            isPlaylistIdOnDownloadBottomSheetVisible = isBottomSheetVisible,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingUiState(),
    )

    fun sendIntent(intent: SettingIntent) {
        when (intent) {
            SettingIntent.OnInit -> Unit
            SettingIntent.OnClickShouldInsertAtTopOnDownloadTile -> {
                updateShouldInsertAtTopOnDownload(!uiState.value.shouldInsertAtTopOnDownload)
            }
            is SettingIntent.OnToggleShouldInsertAtTopOnDownload -> {
                updateShouldInsertAtTopOnDownload(intent.value)
            }
            SettingIntent.OnClickPlaylistIdOnDownloadTile -> {
                isPlaylistIdOnDownloadBottomSheetVisible.value = true
            }
            SettingIntent.OnDismissPlaylistIdOnDownloadBottomSheet -> {
                isPlaylistIdOnDownloadBottomSheetVisible.value = false
            }
            is SettingIntent.OnSelectPlaylistIdOnDownload -> {
                updatePlaylistIdOnDownload(intent.playlistId)
            }
            SettingIntent.OnClickStepTile -> {
                viewModelScope.launch {
                    _eventFlow.emit(SettingEvent.NavigateToStepScreen)
                }
            }
            SettingIntent.OnClickAppLogTile -> {
                viewModelScope.launch {
                    _eventFlow.emit(SettingEvent.NavigateToAppLogScreen)
                }
            }
        }
    }

    private fun updateShouldInsertAtTopOnDownload(value: Boolean) {
        viewModelScope.launch {
            updateShouldInsertAtTopOnDownloadUseCase(value)
        }
    }

    private fun updatePlaylistIdOnDownload(playlistId: Int) {
        viewModelScope.launch {
            updatePlaylistIdOnDownloadUseCase(playlistId)
            isPlaylistIdOnDownloadBottomSheetVisible.value = false
        }
    }

    private fun toDisplayPlaylistName(playlistId: Int, playlistName: String): String {
        return if (playlistId == Playlist.UNCATEGORIZED) "기본" else playlistName
    }
}
