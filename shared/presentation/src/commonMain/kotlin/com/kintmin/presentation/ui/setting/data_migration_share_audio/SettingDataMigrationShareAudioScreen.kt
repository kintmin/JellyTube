package com.kintmin.presentation.ui.setting.data_migration_share_audio

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.CollapsibleSearchField
import com.kintmin.presentation.ui.common.FloatingSearchFieldHeight
import com.kintmin.presentation.ui.common.SearchToggleButton
import com.kintmin.presentation.ui.common.SelectionTopAppBar
import com.kintmin.presentation.ui.setting.data_migration_share_audio.list.SettingDataMigrationShareAudioListItemView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingDataMigrationShareAudioScreen(
    navigateToBack: () -> Unit,
    navigateToSend: (audioMediaIdList: List<Int>) -> Unit,
) {
    val viewModel = koinViewModel<SettingDataMigrationShareAudioViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    SettingDataMigrationShareAudioScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        onClickSend = { navigateToSend(uiState.checkedAudioMediaIdList) },
        sendIntent = viewModel::sendIntent,
    )
}

@Composable
fun SettingDataMigrationShareAudioScreen(
    uiState: SettingDataMigrationShareAudioUiState,
    navigateToBack: () -> Unit,
    onClickSend: () -> Unit,
    sendIntent: (SettingDataMigrationShareAudioIntent) -> Unit,
) {
    var isSearchFieldVisible by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    fun changeSearchText(text: String) {
        searchText = text
        sendIntent(SettingDataMigrationShareAudioIntent.OnChangeSearchText(text))
    }
    fun closeSearchField() {
        isSearchFieldVisible = false
        changeSearchText("")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SelectionTopAppBar(
                title = "보낼 곡 선택",
                selectedCount = uiState.checkedCount,
                onClickBack = navigateToBack,
                onClearSelection = { sendIntent(SettingDataMigrationShareAudioIntent.OnClickClearCheckedList) },
                actions = {
                    IconButton(onClick = { sendIntent(SettingDataMigrationShareAudioIntent.OnClickSelectAll) }) {
                        Icon(
                            imageVector = if (uiState.isAllChecked) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                            contentDescription = if (uiState.isAllChecked) "전체 선택 해제" else "전체 선택",
                        )
                    }
                    SearchToggleButton(
                        isSearchFieldVisible = isSearchFieldVisible,
                        contentDescription = "음원 검색",
                        onClick = { if (isSearchFieldVisible) closeSearchField() else isSearchFieldVisible = true },
                    )
                },
            )
        },
        bottomBar = {
            if (!uiState.isSendEnabled) return@Scaffold
            BottomActionBar(modifier = Modifier.imePadding()) {
                BottomActionButton(
                    text = "${uiState.checkedCount}곡 보내기",
                    onClick = onClickSend,
                )
            }
        },
    ) { innerPadding ->
        // 필드가 목록 위에 뜨므로 그만큼 목록 머리를 비워 첫 곡이 가려지지 않게 한다.
        val searchFieldInset by animateDpAsState(
            targetValue = if (isSearchFieldVisible) FloatingSearchFieldHeight else 0.dp,
            label = "searchFieldInset",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = searchFieldInset,
                    bottom = innerPadding.calculateBottomPadding(),
                ),
            ) {
                items(
                    count = uiState.audioMediaList.size,
                    key = { index -> uiState.audioMediaList[index].id },
                ) { index ->
                    val item = uiState.audioMediaList[index]
                    SettingDataMigrationShareAudioListItemView(
                        modifier = Modifier.height(56.dp),
                        data = item,
                        onClick = {
                            sendIntent(SettingDataMigrationShareAudioIntent.OnClickAudioItem(item.id))
                        },
                    )
                }
            }
            CollapsibleSearchField(
                visible = isSearchFieldVisible,
                value = searchText,
                placeholder = "음원 제목 검색",
                onValueChange = ::changeSearchText,
                onClose = ::closeSearchField,
            )
        }
    }
}

@Preview
@Composable
private fun SettingDataMigrationShareAudioScreenPreview() {
    JellyTubeTheme {
        SettingDataMigrationShareAudioScreen(
            uiState = SettingDataMigrationShareAudioUiState.getMock(),
            navigateToBack = {},
            onClickSend = {},
            sendIntent = {},
        )
    }
}
