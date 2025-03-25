package com.kintmin.presentation.ui.audio_play

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.kintmin.platformruntime.service.PlaybackService
import com.kintmin.presentation.theme.YTMusicBoxTheme
import com.kintmin.presentation.ui.audio_play.model.toParcelize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayView(
    modifier: Modifier,
    viewModel: AudioPlayViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lazyPagingItems = viewModel.audioPagingFlow.collectAsLazyPagingItems()
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            viewModel.refreshList()
        },
    ) {
        LazyColumn(modifier = modifier) {
            items(lazyPagingItems.itemCount) { index ->
                val item = lazyPagingItems[index]
                item?.let {
                    AudioItemView(it, onClickPlay = { data ->
                        val intent = Intent(context, PlaybackService::class.java).apply {
                            putExtra(PlaybackService.EXTRA_MEDIA_DATA, data.toParcelize())
                        }
                        context.startService(intent)
                    })
                }
            }

            lazyPagingItems.apply {
                when {
                    loadState.refresh is LoadState.Loading -> {
                        item { Text("로딩 중...") }
                    }

                    loadState.append is LoadState.Loading -> {
                        item { Text("더 불러오는 중...") }
                    }

                    loadState.append is LoadState.Error -> {
                        item { Text("에러 발생: ${(loadState.append as LoadState.Error).error.message}") }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MusicControlsPreview() {
    YTMusicBoxTheme {
        AudioPlayView(Modifier.fillMaxWidth())
    }
}