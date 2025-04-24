package com.kintmin.presentation.ui.audio_play

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.kintmin.presentation.theme.YTMusicBoxTheme
import com.kintmin.presentation.ui.audio_play.header.AudioPlayHeaderView
import com.kintmin.presentation.ui.audio_play.list_item.AudioItemView
import com.kintmin.presentation.ui.audio_play.list_item.AudioPlayUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayView(
    modifier: Modifier,
    lazyPagingItems: Flow<PagingData<AudioPlayUiState>>,
    isBasePlaylist: Boolean,
    sendIntent: (AudioPlayIntent) -> Unit,
) {
    val audioMediaItems = lazyPagingItems.collectAsLazyPagingItems()

    when(audioMediaItems.loadState.refresh) {
        is LoadState.Error -> {}
        LoadState.Loading -> {}
        is LoadState.NotLoading -> {}
    }

    when(audioMediaItems.loadState.append) {
        is LoadState.Error -> {}
        LoadState.Loading -> {}
        is LoadState.NotLoading -> {}
    }

    PullToRefreshBox(
        isRefreshing = audioMediaItems.loadState.refresh is LoadState.Loading,
        onRefresh = {
            sendIntent(AudioPlayIntent.PullToRefreshAudioList)
        },
    ) {
        LazyColumn(modifier = modifier) {
            item {
                AudioPlayHeaderView(sendIntent)
            }
            items(
                count = audioMediaItems.itemCount,
                key = { index -> audioMediaItems[index]?.id ?: "" }
            ) { index ->
                val item = audioMediaItems[index]
                item?.let {
                    AudioItemView(
                        data = it,
                        isBasePlaylist = isBasePlaylist,
                        sendIntent = sendIntent,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicControlsPreview() {
    YTMusicBoxTheme {
        AudioPlayView(
            modifier = Modifier.fillMaxWidth(),
            lazyPagingItems = flowOf(PagingData.from(AudioPlayUiState.getMockList())),
            isBasePlaylist = true,
            sendIntent = {},
        )
    }
}