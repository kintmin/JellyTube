package com.kintmin.presentation.ui.setting.data_migration_share_playlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.EmptyStateView
import com.kintmin.presentation.ui.common.SelectionTopAppBar
import com.kintmin.presentation.ui.setting.data_migration_share_playlist.list.SettingDataMigrationSharePlaylistListItemView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingDataMigrationSharePlaylistScreen(
    navigateToBack: () -> Unit,
    navigateToSend: (playlistIdList: List<Int>) -> Unit,
) {
    val viewModel = koinViewModel<SettingDataMigrationSharePlaylistViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    SettingDataMigrationSharePlaylistScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        onClickSend = { navigateToSend(uiState.checkedPlaylistIdList) },
        sendIntent = viewModel::sendIntent,
    )
}

@Composable
fun SettingDataMigrationSharePlaylistScreen(
    uiState: SettingDataMigrationSharePlaylistUiState,
    navigateToBack: () -> Unit,
    onClickSend: () -> Unit,
    sendIntent: (SettingDataMigrationSharePlaylistIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SelectionTopAppBar(
                title = "보낼 플레이리스트 선택",
                selectedCount = uiState.checkedCount,
                onClickBack = navigateToBack,
                onClearSelection = {
                    sendIntent(SettingDataMigrationSharePlaylistIntent.OnClickClearCheckedList)
                },
                selectedUnit = "개",
                actions = {
                    IconButton(
                        onClick = { sendIntent(SettingDataMigrationSharePlaylistIntent.OnClickSelectAll) },
                    ) {
                        Icon(
                            imageVector = if (uiState.isAllChecked) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                            contentDescription = if (uiState.isAllChecked) "전체 선택 해제" else "전체 선택",
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isSendEnabled) return@Scaffold
            BottomActionBar {
                BottomActionButton(
                    text = "${uiState.checkedCount}개 플레이리스트 보내기",
                    onClick = onClickSend,
                )
            }
        },
    ) { innerPadding ->
        if (uiState.playlistList.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                icon = Icons.Rounded.LibraryMusic,
                title = "보낼 플레이리스트가 없습니다",
                description = "플레이리스트를 만든 뒤에 다시 들어오세요",
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
        ) {
            items(
                count = uiState.playlistList.size,
                key = { index -> uiState.playlistList[index].id },
            ) { index ->
                val item = uiState.playlistList[index]
                SettingDataMigrationSharePlaylistListItemView(
                    modifier = Modifier.height(56.dp),
                    data = item,
                    onClick = {
                        sendIntent(SettingDataMigrationSharePlaylistIntent.OnClickPlaylistItem(item.id))
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingDataMigrationSharePlaylistScreenPreview() {
    JellyTubeTheme {
        SettingDataMigrationSharePlaylistScreen(
            uiState = SettingDataMigrationSharePlaylistUiState.getMock(),
            navigateToBack = {},
            onClickSend = {},
            sendIntent = {},
        )
    }
}
