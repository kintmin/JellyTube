package com.kintmin.presentation.ui.setting.data_migration_share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.setting.section.SettingItemView
import com.kintmin.presentation.ui.setting.section.SettingNavigateIcon
import com.kintmin.presentation.ui.setting.section.SettingNoticeCardView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingDataMigrationShareScreen(
    navigateToBack: () -> Unit,
    navigateToSync: () -> Unit,
    navigateToPlaylistSelect: () -> Unit,
    navigateToAudioMediaSelect: () -> Unit,
) {
    val viewModel = koinViewModel<SettingDataMigrationShareViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    SettingDataMigrationShareScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        navigateToSync = navigateToSync,
        navigateToPlaylistSelect = navigateToPlaylistSelect,
        navigateToAudioMediaSelect = navigateToAudioMediaSelect,
    )
}

@Composable
fun SettingDataMigrationShareScreen(
    uiState: SettingDataMigrationShareUiState,
    navigateToBack: () -> Unit,
    navigateToSync: () -> Unit,
    navigateToPlaylistSelect: () -> Unit,
    navigateToAudioMediaSelect: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { DetailTopAppBar(title = "다른 기기로 데이터 보내기", onClickBack = navigateToBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingItemView(
                title = "동기화",
                icon = Icons.Rounded.Sync,
                description = "이 기기의 곡·플레이리스트·즐겨찾기·설정·걸음 기록을 그대로 옮깁니다.",
                onClick = navigateToSync,
                trailing = { SettingNavigateIcon(contentDescription = "동기화 시작 화면으로 이동") },
            )
            SettingItemView(
                title = "플레이리스트 보내기",
                icon = Icons.Rounded.LibraryMusic,
                description = "고른 플레이리스트와 그 안의 곡만 보냅니다.",
                onClick = navigateToPlaylistSelect,
                trailing = { SettingNavigateIcon(contentDescription = "플레이리스트 선택 화면으로 이동") },
            )
            SettingItemView(
                title = "곡 보내기",
                icon = Icons.Rounded.MusicNote,
                description = "고른 곡만 보냅니다. 받는 기기가 담을 플레이리스트를 정합니다.",
                onClick = navigateToAudioMediaSelect,
                trailing = { SettingNavigateIcon(contentDescription = "곡 선택 화면으로 이동") },
            )

            if (!uiState.hasSecretPlaylist) return@Column
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (uiState.isSecretModeEnabled) {
                    SettingNoticeCardView(
                        icon = Icons.Rounded.Lock,
                        title = "비밀 플레이리스트도 함께 갑니다",
                        text = "비밀모드가 켜져 있어 비밀 플레이리스트 ${uiState.secretPlaylistCount}개가 보낼 것에 들어갑니다. " +
                            "받는 기기에서도 비밀모드를 켜야 보입니다.",
                    )
                } else {
                    SettingNoticeCardView(
                        icon = Icons.Rounded.Lock,
                        title = "비밀 플레이리스트는 보내지 않습니다",
                        text = "기기를 옮기는 중이라면 비밀모드를 켠 뒤에 다시 들어오세요. " +
                            "켜지 않고 보내면 비밀 플레이리스트는 이 기기에만 남습니다.",
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingDataMigrationShareScreenPreview() {
    JellyTubeTheme {
        SettingDataMigrationShareScreen(
            uiState = SettingDataMigrationShareUiState.getMock(),
            navigateToBack = {},
            navigateToSync = {},
            navigateToPlaylistSelect = {},
            navigateToAudioMediaSelect = {},
        )
    }
}

@Preview
@Composable
private fun SettingDataMigrationShareScreenSecretOffPreview() {
    JellyTubeTheme {
        SettingDataMigrationShareScreen(
            uiState = SettingDataMigrationShareUiState(isSecretModeEnabled = false, secretPlaylistCount = 1),
            navigateToBack = {},
            navigateToSync = {},
            navigateToPlaylistSelect = {},
            navigateToAudioMediaSelect = {},
        )
    }
}
