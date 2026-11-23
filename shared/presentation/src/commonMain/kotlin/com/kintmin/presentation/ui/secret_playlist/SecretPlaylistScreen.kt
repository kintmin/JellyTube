package com.kintmin.presentation.ui.secret_playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.common.SectionHeader
import com.kintmin.presentation.ui.common.wheel_picker.NumberWheelPicker
import com.kintmin.presentation.ui.main.bottom_sheet.PlaylistReorderBottomSheet
import com.kintmin.presentation.ui.main.playlist.PlaylistEmptyView
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.main.playlist.dialog.PlaylistAddDialog
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistAddButtonView
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistItemView
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistLibraryBlockView
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistReorderActionView
import com.kintmin.presentation.ui.player_bar.PlayerBar
import com.kintmin.presentation.ui.player_bar.PlayerBarIntent
import com.kintmin.presentation.ui.player_bar.PlayerBarUiState
import com.kintmin.presentation.ui.player_bar.PlayerBarViewModel
import com.kintmin.presentation.ui.player_detail.dialog.PlaybackOptionChipRow
import com.kintmin.presentation.ui.setting.section.SettingItemView
import com.kintmin.presentation.ui.setting.section.SettingSwitchView
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes

private val SecretModeMinuteOptions = listOf(15, 30, 45, 60, 90)

private val SecretModeMinuteRange = (1..120).toList()

@Composable
fun SecretPlaylistScreen(
    navigateToBack: () -> Unit,
    navigateToPlaylistDetail: (playlistId: Int, secretOnly: Boolean) -> Unit,
    navigateToPlaylistEdit: (playlistId: Int) -> Unit,
    navigateToPlaylistAdd: (playlistId: Int, playlistName: String) -> Unit,
    navigateToPlayerDetail: () -> Unit,
) {
    val toaster = rememberToaster()
    val viewModel = koinViewModel<SecretPlaylistViewModel>()
    val playerBarViewModel = koinViewModel<PlayerBarViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val playerBar by playerBarViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SecretPlaylistEvent.NavigateToPlaylistDetailScreen ->
                    navigateToPlaylistDetail(event.playlistId, event.secretOnly)
                is SecretPlaylistEvent.NavigateToPlaylistEditScreen -> navigateToPlaylistEdit(event.playlistId)
                is SecretPlaylistEvent.NavigateToPlaylistAddScreen -> navigateToPlaylistAdd(event.playlistId, event.playlistName)
                is SecretPlaylistEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    SecretPlaylistScreen(
        uiState = uiState,
        playerBar = playerBar,
        navigateToBack = navigateToBack,
        navigateToPlayerDetail = navigateToPlayerDetail,
        sendIntent = viewModel::sendIntent,
        sendPlayerBarIntent = playerBarViewModel::sendIntent,
    )
}

@Composable
fun SecretPlaylistScreen(
    uiState: SecretPlaylistUiState,
    playerBar: PlayerBarUiState,
    navigateToBack: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    sendIntent: (SecretPlaylistIntent) -> Unit,
    sendPlayerBarIntent: (PlayerBarIntent) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    PlaylistAddDialog(
        showDialog = showAddDialog,
        onDismiss = { showAddDialog = false },
        existingNames = uiState.playlists.map { it.name },
        makePlaylist = { title ->
            sendIntent(SecretPlaylistIntent.OnPlaylistIntent(PlaylistIntent.MakeNewPlaylist(title)))
        },
    )

    if (uiState.isReorderBottomSheetVisible) {
        PlaylistReorderBottomSheet(
            playlists = uiState.playlists,
            title = "비밀 플레이리스트 순서",
            onReorder = { orderedIds ->
                sendIntent(SecretPlaylistIntent.OnPlaylistIntent(PlaylistIntent.OnReorderPlaylist(orderedIds)))
            },
            onDismissRequest = {
                sendIntent(SecretPlaylistIntent.OnPlaylistIntent(PlaylistIntent.OnDismissReorderBottomSheet))
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = "비밀 플레이리스트",
                onClickBack = navigateToBack,
            )
        },
        bottomBar = {
            Column {
                PlayerBar(
                    data = playerBar,
                    sendIntent = sendPlayerBarIntent,
                    onClickBar = navigateToPlayerDetail,
                )
                Box(
                    modifier = Modifier
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item(key = "secret-mode", span = { GridItemSpan(maxLineSpan) }) {
                SettingItemView(
                    title = "비밀모드",
                    icon = Icons.Rounded.Lock,
                    description = uiState.remaining?.let { "${it.to_hh_colon_mm_colon_ss()} 뒤에 꺼집니다" }
                        ?: "켜면 ${uiState.minutes}분 동안 비밀 플레이리스트가 보입니다",
                    onClick = { sendIntent(SecretPlaylistIntent.OnToggleSecretMode(!uiState.isEnabled)) },
                    trailing = {
                        SettingSwitchView(
                            checked = uiState.isEnabled,
                            onCheckedChange = { sendIntent(SecretPlaylistIntent.OnToggleSecretMode(it)) },
                        )
                    },
                )
            }

            if (!uiState.isEnabled) {
                item(key = "minutes", span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PlaybackOptionChipRow(
                            options = SecretModeMinuteOptions,
                            selected = uiState.minutes,
                            label = { option -> "${option}분" },
                            onSelect = { option -> sendIntent(SecretPlaylistIntent.OnSelectMinutes(option)) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NumberWheelPicker(
                            values = SecretModeMinuteRange,
                            selectedValue = uiState.minutes,
                            onValueChange = { sendIntent(SecretPlaylistIntent.OnSelectMinutes(it)) },
                            label = "분",
                            formatter = { it.toString() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                return@LazyVerticalGrid
            }

            uiState.secretFavorite?.let { secretFavorite ->
                item(key = "secret-favorite", span = { GridItemSpan(maxLineSpan) }) {
                    PlaylistLibraryBlockView(
                        modifier = Modifier.fillMaxWidth(),
                        data = listOf(secretFavorite),
                        sendIntent = { sendIntent(SecretPlaylistIntent.OnPlaylistIntent(it)) },
                    )
                }
            }

            item(key = "secret-header", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    text = "비밀 플레이리스트",
                    showDivider = true,
                    action = {
                        // 빈 상태가 제 CTA 를 갖는다. 같은 명령으로 가는 버튼을 둘 보이지 않게 한다.
                        if (uiState.playlists.isEmpty()) return@SectionHeader
                        // 바꿀 것이 없으면 빈 시트가 올라온다. 누를 수 없게 막는다.
                        PlaylistReorderActionView(
                            enabled = uiState.playlists.size > 1,
                            onClick = {
                                sendIntent(
                                    SecretPlaylistIntent.OnPlaylistIntent(PlaylistIntent.OnClickShowReorderBottomSheet)
                                )
                            },
                        )
                        PlaylistAddButtonView(text = "새로 만들기", onClick = { showAddDialog = true })
                    },
                )
            }

            if (uiState.playlists.isEmpty()) {
                item(key = "secret-empty", span = { GridItemSpan(maxLineSpan) }) {
                    PlaylistEmptyView(modifier = Modifier, onClickAdd = { showAddDialog = true })
                }
            } else {
                items(
                    count = uiState.playlists.size,
                    key = { index -> uiState.playlists[index].id },
                ) { index ->
                    PlaylistItemView(
                        modifier = Modifier,
                        data = uiState.playlists[index],
                        sendIntent = { sendIntent(SecretPlaylistIntent.OnPlaylistIntent(it)) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SecretPlaylistScreenOffPreview() {
    JellyTubeTheme {
        SecretPlaylistScreen(
            uiState = SecretPlaylistUiState(isEnabled = false, minutes = 30),
            playerBar = PlayerBarUiState.getMock(),
            navigateToBack = {},
            navigateToPlayerDetail = {},
            sendIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}

@Preview
@Composable
fun SecretPlaylistScreenOnPreview() {
    JellyTubeTheme {
        SecretPlaylistScreen(
            uiState = SecretPlaylistUiState(
                isEnabled = true,
                minutes = 30,
                remaining = 12.minutes,
                playlists = listOf(PlaylistItemUiState.getMock(id = 10).copy(name = "나만 듣는 곡")),
            ),
            playerBar = PlayerBarUiState.getMock(),
            navigateToBack = {},
            navigateToPlayerDetail = {},
            sendIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}
