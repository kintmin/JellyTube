package com.kintmin.presentation.ui.playlist_detail.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.ui.common.FullScreenImageViewer
import okio.Path.Companion.toPath

@Composable
fun PlaylistDetailHeaderView(
    innerPaddingValues: PaddingValues,
    headerData: PlaylistDetailHeaderUiState,
    isBasePlaylist: Boolean,
    sendIntent: (PlaylistDetailHeaderIntent) -> Unit,
) {
    var isShowFullScreenImageViewer by remember { mutableStateOf(false) }

    FullScreenImageViewer(
        imageFileFullPath = if (isShowFullScreenImageViewer) headerData.imageFileFullPath else null,
        onDismiss = { isShowFullScreenImageViewer = false },
    )

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = innerPaddingValues.calculateTopPadding())
                .padding(16.dp)
        ) {
            if (headerData.imageFileFullPath == null) {
                Box(
                    modifier = Modifier
                        .height(220.dp)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(4))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(headerData.imageFileFullPath.toPath())
                        .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil3.request.CachePolicy.DISABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(4))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { isShowFullScreenImageViewer = true }
                )
            }

            Column {
                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.wrapContentHeight(),
                    ) {
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp),
                            text = headerData.name,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = headerData.playlistSubtitle,
                            fontSize = 12.sp,
                            lineHeight = 10.sp,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    // Filled 과 Rounded 변종은 20dp 에서 구분되지 않아 켜짐이 색으로만
                    // 전달됐다. 켜짐을 채운 배경으로 바꿔 모양으로도 읽히게 한다.
                    FilledIconToggleButton(
                        checked = headerData.isShuffling,
                        onCheckedChange = {
                            sendIntent(PlaylistDetailHeaderIntent.OnClickShuffle)
                        },
                        modifier = Modifier
                            .wrapContentWidth(Alignment.End)
                            .weight(1f),
                        colors = playbackToggleColors(),
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "셔플",
                        )
                    }
                    FilledIconToggleButton(
                        checked = headerData.isRepeating,
                        onCheckedChange = {
                            sendIntent(PlaylistDetailHeaderIntent.OnClickRepeat)
                        },
                        modifier = Modifier
                            .wrapContentWidth(),
                        colors = playbackToggleColors(),
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "반복",
                        )
                    }
                }
                if (headerData.description.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = headerData.description,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Row(
                // Material 최소 터치 타깃이다. 36dp 로 두면 세 버튼이 모두 미달이다.
                modifier = Modifier.height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isBasePlaylist) {
                    OutlinedButton(
                        onClick = {
                            sendIntent(PlaylistDetailHeaderIntent.OnClickAdd)
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth()
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(0.dp),
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                        )
                        Text(
                            modifier = Modifier.padding(0.dp),
                            text = "추가",
                            fontSize = 12.sp,
                        )
                    }
                    Box(modifier = Modifier.width(8.dp))
                }
                OutlinedButton(
                    onClick = {
                        sendIntent(PlaylistDetailHeaderIntent.OnClickEdit)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentWidth()
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                    )
                    Text(
                        text = "수정",
                        fontSize = 12.sp,
                    )
                }
                Button(
                    onClick = {
                        sendIntent(PlaylistDetailHeaderIntent.OnClickPlay)
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                    )
                    Text(
                        text = "모두 재생",
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

/** 꺼짐은 배경 없는 평범한 아이콘, 켜짐은 채운 원이다. 0.38 알파는 M3 에서 비활성을 뜻한다. */
@Composable
private fun playbackToggleColors() = IconButtonDefaults.filledIconToggleButtonColors(
    containerColor = Color.Transparent,
    contentColor = gray60,
    checkedContainerColor = MaterialTheme.colorScheme.primary,
    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
)

@Preview
@Composable
fun MusicControlsPreview() {
    JellyTubeTheme {
        PlaylistDetailHeaderView(
            innerPaddingValues = PaddingValues(top = 24.dp),
            sendIntent = {},
            isBasePlaylist = false,
            headerData = PlaylistDetailHeaderUiState.getMock()
                .copy(isShuffling = false, isRepeating = false),
        )
    }
}

@Preview
@Composable
fun MusicControlsPlaybackTogglesOnPreview() {
    JellyTubeTheme {
        PlaylistDetailHeaderView(
            innerPaddingValues = PaddingValues(top = 24.dp),
            sendIntent = {},
            isBasePlaylist = false,
            headerData = PlaylistDetailHeaderUiState.getMock()
                .copy(isShuffling = true, isRepeating = true),
        )
    }
}