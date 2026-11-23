package com.kintmin.presentation.ui.setting.data_migration_share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SettingDataMigrationShareViewModel constructor(
    fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase,
    fetchSecretModeEnabledFlowUseCase: FetchSecretModeEnabledFlowUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingDataMigrationShareUiState> = combine(
        // 비밀모드가 꺼져 있어도 세어야 한다 — 빠진다는 사실을 알려야 할지 이 수로 판단한다.
        fetchPlaylistsByTypeUseCase(PlaylistType.SECRET),
        fetchSecretModeEnabledFlowUseCase(),
    ) { secretPlaylists, isSecretModeEnabled ->
        SettingDataMigrationShareUiState(
            isSecretModeEnabled = isSecretModeEnabled,
            secretPlaylistCount = secretPlaylists.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingDataMigrationShareUiState())
}
