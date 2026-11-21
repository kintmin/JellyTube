package com.kintmin.presentation.ui.lyrics_viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.animation.FollowActiveLyricLine
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.destructiveContent
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.ui.lyrics_viewer.dialog.LyricsDeleteDialog
import com.kintmin.presentation.ui.lyrics_viewer.list_item.LyricsViewerLineView
import com.kintmin.presentation.ui.player_bar.PlayerBar
import com.kintmin.presentation.ui.player_bar.PlayerBarIntent
import com.kintmin.presentation.ui.player_bar.PlayerBarUiState
import com.kintmin.presentation.ui.player_bar.PlayerBarViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LyricsViewerScreen(
    navigateToBack: () -> Unit,
    navigateToLyricsEdit: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
) {
    val viewModel = koinViewModel<LyricsViewerViewModel>()
    val playerBarViewModel = koinViewModel<PlayerBarViewModel>()
    val data by viewModel.uiState.collectAsState()
    val playerBar by playerBarViewModel.uiState.collectAsState()
    val toaster = rememberToaster()

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is LyricsViewerEvent.ShowToast ->
                    toaster.show(event.message)

                LyricsViewerEvent.NavigateToBack -> navigateToBack()
            }
        }
    }

    LyricsViewerScreen(
        navigateToBack = navigateToBack,
        navigateToLyricsEdit = navigateToLyricsEdit,
        navigateToPlayerDetail = navigateToPlayerDetail,
        data = data,
        playerBar = playerBar,
        sendIntent = viewModel::sendIntent,
        sendPlayerBarIntent = playerBarViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsViewerScreen(
    navigateToBack: () -> Unit,
    navigateToLyricsEdit: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    data: LyricsViewerUiState,
    playerBar: PlayerBarUiState,
    sendIntent: (LyricsViewerIntent) -> Unit,
    sendPlayerBarIntent: (PlayerBarIntent) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    LyricsDeleteDialog(
        isShow = showDeleteDialog,
        audioMediaName = data.title,
        onDismiss = { showDeleteDialog = false },
        onConfirmDelete = { sendIntent(LyricsViewerIntent.OnClickDeleteLyrics) },
    )

    // 재생 위치는 Flow가 없으므로 300ms 폴링으로 현재 가사 줄을 갱신한다.
    // RESUMED 동안만 돈다. repeatOnLifecycle 안에서 lifecycleScope.launch 로 빠져나가면
    // 화면을 벗어나도 루프가 취소되지 않아 백그라운드에서 계속 돌았다.
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                delay(300)
                sendIntent(LyricsViewerIntent.OnRefreshPosition)
            }
        }
        onPauseOrDispose { job.cancel() }
    }

    val listState = rememberLazyListState()
    FollowActiveLyricLine(listState = listState, activeIndex = data.activeIndex)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    Text(
                        text = data.title.ifBlank { "가사" },
                        fontSize = topAppBarDetailTitleFontSize,
                        lineHeight = topAppBarDetailTitleLineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = "뒤로",
                        )
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = "더보기",
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (data.showTranslateMenu) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when {
                                            data.isCreatingTranslation -> "번역 중…"
                                            data.translationLines != null -> "번역 다시 만들기"
                                            else -> "번역 만들기"
                                        }
                                    )
                                },
                                enabled = !data.isCreatingTranslation,
                                onClick = {
                                    menuExpanded = false
                                    sendIntent(LyricsViewerIntent.OnClickCreateTranslation)
                                },
                            )
                        }
                        if (data.showTransliterateMenu) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when {
                                            data.isCreatingTransliteration -> "음차 만드는 중…"
                                            data.transliterationLines != null -> "음차 다시 만들기"
                                            else -> "음차 만들기"
                                        }
                                    )
                                },
                                enabled = !data.isCreatingTransliteration,
                                onClick = {
                                    menuExpanded = false
                                    sendIntent(LyricsViewerIntent.OnClickCreateTransliteration)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("편집하기") },
                            onClick = {
                                menuExpanded = false
                                navigateToLyricsEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("삭제하기") },
                            colors = MenuDefaults.itemColors(textColor = destructiveContent),
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            },
                        )
                    }
                },
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
        when {
            data.isLoading -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            data.lines.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "표시할 가사가 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                ) {
                    itemsIndexed(data.lines) { index, line ->
                        LyricsViewerLineView(
                            line = line,
                            transliteration = data.transliterationLines?.getOrNull(index),
                            translation = data.translationLines?.getOrNull(index),
                            isActive = data.isSynced && index == data.activeIndex,
                            onClick = if (data.canSeek) {
                                { sendIntent(LyricsViewerIntent.OnClickLine(index)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LyricsViewerScreenPreview() {
    JellyTubeTheme {
        LyricsViewerScreen(
            navigateToBack = {},
            navigateToLyricsEdit = {},
            navigateToPlayerDetail = {},
            data = LyricsViewerUiState.getMock(),
            playerBar = PlayerBarUiState.getMock(),
            sendIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}
