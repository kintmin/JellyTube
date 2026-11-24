package com.kintmin.presentation.ui.setting.data_migration_share_playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.data_migration.usecase.FetchShareablePlaylistListFlowUseCase
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.presentation.ui.setting.data_migration_share_playlist.list.toSettingDataMigrationSharePlaylistListItemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SettingDataMigrationSharePlaylistViewModel constructor(
    fetchShareablePlaylistListFlowUseCase: FetchShareablePlaylistListFlowUseCase,
) : ViewModel() {

    private val checkedPlaylistIdList = MutableStateFlow(listOf<Int>())

    private val playlistListFlow: StateFlow<List<Playlist>> = fetchShareablePlaylistListFlowUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SettingDataMigrationSharePlaylistUiState> = combine(
        playlistListFlow,
        checkedPlaylistIdList,
    ) { playlists, checkedIds ->
        SettingDataMigrationSharePlaylistUiState(
            playlistList = playlists.map {
                it.toSettingDataMigrationSharePlaylistListItemUiState(isChecked = it.id in checkedIds)
            },
            // 목록에서 사라진 플레이리스트가 선택에 남아 있으면 보내는 수가 실제와 어긋난다.
            checkedPlaylistIdList = checkedIds.filter { id -> playlists.any { it.id == id } },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingDataMigrationSharePlaylistUiState(),
    )

    fun sendIntent(intent: SettingDataMigrationSharePlaylistIntent) {
        when (intent) {
            is SettingDataMigrationSharePlaylistIntent.OnClickPlaylistItem -> checkItem(intent.playlistId)
            SettingDataMigrationSharePlaylistIntent.OnClickSelectAll -> selectAll()
            SettingDataMigrationSharePlaylistIntent.OnClickClearCheckedList ->
                checkedPlaylistIdList.update { emptyList() }
        }
    }

    private fun checkItem(playlistId: Int) {
        checkedPlaylistIdList.update { if (playlistId in it) it - playlistId else it + playlistId }
    }

    private fun selectAll() {
        val allIds = playlistListFlow.value.map { it.id }
        checkedPlaylistIdList.update { if (it.size == allIds.size) emptyList() else allIds }
    }
}
