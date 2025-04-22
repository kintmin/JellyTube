package com.kintmin.presentation.ui.audio_play

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.platform.service.PlaybackService
import com.kintmin.presentation.theme.YTMusicBoxTheme
import com.kintmin.presentation.ui.audio_play.model.AudioPlayUiState
import com.kintmin.presentation.ui.audio_play.model.toParcelize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayView(
    modifier: Modifier,
    lazyPagingItems: Flow<PagingData<AudioPlayUiState>>,
    onRefresh: () -> Unit,
    isBasePlaylist: Boolean = true,
    modifyAudioMedia: (AudioPlayUiState) -> Unit = {},
    deleteAudioMediaFromPlaylist: (AudioPlayUiState) -> Unit = {},
    deleteAudioMedia: (AudioPlayUiState) -> Unit = {},
) {
    val context = LocalContext.current
    val items = lazyPagingItems.collectAsLazyPagingItems()
    val isRefreshing = items.loadState.refresh is LoadState.Loading

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(
            androidx.media3.session.R.drawable.media3_icon_artist
        )
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            onRefresh()
        },
    ) {
        LazyColumn(modifier = modifier) {
            item {
                Box(
                    modifier
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .padding(24.dp, 16.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(4))
                                .background(Color.Gray)
                        )
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp),
                            text = "플레이리스트",
                            fontSize = 16.sp,
                            lineHeight = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "음원 nnnn개",
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "플레이타임: hh:mm:ss",
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row {
                            ElevatedButton(
                                onClick = {

                                },
                                modifier = Modifier.wrapContentHeight().weight(1f),
                                contentPadding = PaddingValues(0.dp),

                            ) {
                                Text(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    text = "모두 재생",
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            ElevatedButton(
                                onClick = {

                                },
                                modifier = Modifier.wrapContentHeight().weight(1f),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    text = "셔플",
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                    }
                }
            }
            items(items.itemCount) { index ->
                val item = items[index]
                item?.let {
                    AudioItemView(
                        data = it,
                        onClickPlay = { data ->
                            val intent = Intent(context, PlaybackService::class.java).apply {
                                putExtra(PlaybackService.EXTRA_MEDIA_DATA, data.toParcelize())
                            }
                            context.startService(intent)
                        },
                        isBasePlaylist = isBasePlaylist,
                        modifyAudioMedia = modifyAudioMedia,
                        deleteAudioMediaFromPlaylist = deleteAudioMediaFromPlaylist,
                        deleteAudioMedia = deleteAudioMedia,
                    )
                }
            }

            items.apply {
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
        AudioPlayView(
            Modifier.fillMaxWidth(),
            flowOf(PagingData.from(AudioPlayUiState.getMockList())),
            {}
        )
    }
}