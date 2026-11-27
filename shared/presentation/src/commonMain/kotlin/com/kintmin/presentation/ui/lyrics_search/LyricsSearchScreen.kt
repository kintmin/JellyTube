package com.kintmin.presentation.ui.lyrics_search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.ui.common.BackNavigationIconButton
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.EmptyStateView
import com.kintmin.presentation.ui.common.FloatingSearchFieldHeight
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog
import com.kintmin.presentation.ui.common.SearchField
import com.kintmin.presentation.ui.common.floatingField
import com.kintmin.presentation.ui.lyrics_search.list_item.LyricsSearchItemView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LyricsSearchScreen(
    navigateToBack: () -> Unit,
    navigateToLyricsDetail: (
        audioMediaId: Int,
        trackName: String,
        artistName: String,
        plainLyrics: String,
        syncedLyrics: String,
    ) -> Unit,
    navigateToLyricsEdit: (audioMediaId: Int) -> Unit,
) {
    val viewModel = koinViewModel<LyricsSearchViewModel>()
    val data by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is LyricsSearchEvent.NavigateToLyricsDetail -> navigateToLyricsDetail(
                    event.audioMediaId,
                    event.trackName,
                    event.artistName,
                    event.plainLyrics,
                    event.syncedLyrics,
                )

                is LyricsSearchEvent.NavigateToLyricsEdit -> navigateToLyricsEdit(event.audioMediaId)
            }
        }
    }

    LyricsSearchScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSearchScreen(
    navigateToBack: () -> Unit,
    data: LyricsSearchUiState,
    sendIntent: (LyricsSearchIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = listOf("가사 검색", data.playbackDurationText)
                                .filter { it.isNotEmpty() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = data.audioMediaName,
                            fontSize = topAppBarDetailTitleFontSize,
                            lineHeight = topAppBarDetailTitleLineHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                expandedHeight = topAppBarHeight,
                navigationIcon = {
                    BackNavigationIconButton(onClick = navigateToBack)
                },
            )
        },
        bottomBar = {
            BottomActionBar {
                BottomActionButton(
                    text = "가사 직접 추가하기",
                    onClick = { sendIntent(LyricsSearchIntent.OnClickAddLyricsManually) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = FloatingSearchFieldHeight,
                    bottom = innerPadding.calculateBottomPadding(),
                ),
            ) {
                when {
                    data.isLoading -> {
                        item(key = "loading") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    modifier = Modifier.padding(top = 12.dp),
                                    text = "검색 중…",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    data.isEmptyResult -> {
                        item(key = "empty") {
                            EmptyStateView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                icon = Icons.Rounded.SearchOff,
                                title = "검색 결과가 없습니다",
                            )
                        }
                    }

                    else -> {
                        items(
                            items = data.results,
                            key = { item -> item.id },
                        ) { item ->
                            LyricsSearchItemView(
                                item = item,
                                onClick = { sendIntent(LyricsSearchIntent.OnClickResult(item)) },
                            )
                        }
                    }
                }
            }
            SearchField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .floatingField(),
                value = data.query,
                placeholder = "가사 검색 (제목·아티스트)",
                onValueChange = { sendIntent(LyricsSearchIntent.OnChangeQuery(it)) },
                onSearch = { sendIntent(LyricsSearchIntent.OnClickSearch) },
            )
        }
    }

    JellyTubeConfirmDialog(
        showDialog = data.isConfirmingExternalSearch,
        title = "비밀 곡의 가사를 찾을까요?",
        message = "가사를 찾으려면 이 곡의 제목이 가사 서비스로 전달됩니다. " +
            "찾지 않으면 가사를 직접 넣을 수 있습니다.",
        confirmText = "검색",
        onConfirm = { sendIntent(LyricsSearchIntent.OnConfirmExternalSearch) },
        onDismiss = { sendIntent(LyricsSearchIntent.OnDismissExternalSearch) },
        dismissText = "찾지 않기",
    )
}

@Preview
@Composable
private fun LyricsSearchScreenPreview() {
    JellyTubeTheme {
        LyricsSearchScreen(
            navigateToBack = {},
            data = LyricsSearchUiState.getMock(),
            sendIntent = {},
        )
    }
}
