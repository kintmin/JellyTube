package com.kintmin.presentation.ui.lyrics_search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.lyrics_search.list_item.LyricsSearchItemView
import org.koin.androidx.compose.koinViewModel

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
) {
    val viewModel = koinViewModel<LyricsSearchViewModel>()
    val data by viewModel.data.collectAsState()

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
                    Text(text = "가사 검색", fontSize = 16.sp, lineHeight = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "ArrowBackIosNew",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item(key = "search_bar") {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    value = data.query,
                    onValueChange = { sendIntent(LyricsSearchIntent.OnChangeQuery(it)) },
                    label = { Text("가사 검색 (제목·아티스트)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { sendIntent(LyricsSearchIntent.OnClickSearch) }),
                    trailingIcon = {
                        if (data.query.isNotEmpty()) {
                            IconButton(onClick = { sendIntent(LyricsSearchIntent.OnChangeQuery("")) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "검색어 전체 삭제",
                                )
                            }
                        }
                    },
                )
            }

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
                                text = "검색 중...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                data.isEmptyResult -> {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "검색 결과가 없습니다.",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
    }
}

@Preview(showBackground = true)
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
