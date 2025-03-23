package com.kintmin.presentation.ui.audio_play

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.kintmin.domain.model.AudioMediaData
import com.kintmin.presentation.service.PlaybackService

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
                    AudioItemView(it) { data ->
                        val intent = Intent(context, PlaybackService::class.java).apply {
                            putExtra(PlaybackService.EXTRA_MEDIA_DATA, data)
                        }
                        context.startService(intent)
                    }
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

@Composable
fun AudioItemView(
    data: AudioMediaData,
    onClick: (AudioMediaData) -> Unit = {},
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick(data) }
        .padding(16.dp)
    ) {
        Text(text = data.title)
    }
}