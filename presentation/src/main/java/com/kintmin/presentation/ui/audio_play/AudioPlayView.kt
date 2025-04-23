package com.kintmin.presentation.ui.audio_play

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.kintmin.presentation.ui.audio_play.model.toTryParcelize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayView(
    modifier: Modifier,
    lazyPagingItems: Flow<PagingData<AudioPlayUiState>>,
    onRefresh: () -> Unit,
    isBasePlaylist: Boolean = true,
    onStartSequentialPlayback: () -> Unit = {},
    modifyAudioMedia: (AudioPlayUiState) -> Unit = {},
    deleteAudioMediaFromPlaylist: (AudioPlayUiState) -> Unit = {},
    deleteAudioMedia: (AudioPlayUiState) -> Unit = {},
) {
    val context = LocalContext.current
    val audioMediaItems = lazyPagingItems.collectAsLazyPagingItems()
    val isRefreshing = audioMediaItems.loadState.refresh is LoadState.Loading

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(4))
                            .background(Color.Gray)
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 4.dp),
                        text = "플레이리스트 이름",
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = "플레이리스트 · 음원 123개 · 플레이타임 00:00:00",
                        fontSize = 12.sp,
                        lineHeight = 10.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.height(36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ElevatedButton(
                            onClick = {
                                onStartSequentialPlayback()
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 1.dp)
                                .padding(end = 8.dp)
                                .fillMaxHeight()
                                .weight(1f),
                            contentPadding = PaddingValues(0.dp),
                            ) {
                            Text(
                                text = "모두 재생",
                                fontSize = 12.sp,
                            )
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "PlayArrow"
                            )
                        }
                        ElevatedButton(
                            onClick = {

                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 1.dp)
                                .padding(end = 8.dp)
                                .fillMaxHeight()
                                .weight(1f),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = "셔플",
                                fontSize = 12.sp,
                            )
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle"
                            )
                        }
                        IconButton(
                            onClick = {
                                // 음원 전체 검색 & 다중선택 화면으로 전환
                            },
                            modifier = Modifier
                                .background(
                                    color = Color.White,
                                    shape = CircleShape,
                                )
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add"
                            )
                        }
                        Box(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                //
                            },
                            modifier = Modifier
                                .background(
                                    color = Color.White,
                                    shape = CircleShape,
                                )
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit"
                            )
                        }
                        Box(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                //
                            },
                            modifier = Modifier
                                .background(
                                    color = Color.White,
                                    shape = CircleShape,
                                )
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Rounded.Reorder,
                                contentDescription = "Reorder"
                            )
                        }
                    }
                }
            }

            items(
                count = audioMediaItems.itemCount,
                key = { index -> audioMediaItems[index]?.id ?: "" }
            ) { index ->
                val item = audioMediaItems[index]
                item?.let {
                    AudioItemView(
                        data = it,
                        onClickPlay = { data ->
                            data.toTryParcelize().onSuccess {
                                val intent = Intent(context, PlaybackService::class.java).apply {
                                    putExtra(PlaybackService.EXTRA_MEDIA_DATA, data.toTryParcelize())
                                }
                                context.startService(intent)
                            }.onFailure {
                                // 오디오 파일에 문제
                            }
                        },
                        isBasePlaylist = isBasePlaylist,
                        modifyAudioMedia = modifyAudioMedia,
                        deleteAudioMediaFromPlaylist = deleteAudioMediaFromPlaylist,
                        deleteAudioMedia = deleteAudioMedia,
                    )
                }
            }

            audioMediaItems.apply {
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