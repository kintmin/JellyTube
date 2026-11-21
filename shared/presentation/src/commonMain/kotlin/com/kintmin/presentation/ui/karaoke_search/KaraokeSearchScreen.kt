package com.kintmin.presentation.ui.karaoke_search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.common.SearchField
import com.kintmin.presentation.ui.karaoke_search.list_item.KaraokeSearchItemView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KaraokeSearchScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<KaraokeSearchViewModel>()
    val data by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                KaraokeSearchEvent.NavigateBack -> navigateToBack()
            }
        }
    }

    KaraokeSearchScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeSearchScreen(
    navigateToBack: () -> Unit,
    data: KaraokeSearchUiState,
    sendIntent: (KaraokeSearchIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = "TJ 노래방 번호 찾기",
                onClickBack = { navigateToBack() },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item(key = "search_bar") {
                SearchField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    value = data.query,
                    placeholder = "노래 제목으로 검색",
                    onValueChange = { sendIntent(KaraokeSearchIntent.OnChangeQuery(it)) },
                    onSearch = { sendIntent(KaraokeSearchIntent.OnClickSearch) },
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
                                text = "검색 중…",
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
                        KaraokeSearchItemView(
                            item = item,
                            onClick = { sendIntent(KaraokeSearchIntent.OnClickResult(item)) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun KaraokeSearchScreenPreview() {
    JellyTubeTheme {
        KaraokeSearchScreen(
            navigateToBack = {},
            data = KaraokeSearchUiState.getMock(),
            sendIntent = {},
        )
    }
}
