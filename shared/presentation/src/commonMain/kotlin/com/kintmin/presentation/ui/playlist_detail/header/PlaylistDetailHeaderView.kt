package com.kintmin.presentation.ui.playlist_detail.header

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
                    PlaybackToggleButton(
                        modifier = Modifier
                            .wrapContentWidth(Alignment.End)
                            .weight(1f),
                        imageVector = Icons.Rounded.Shuffle,
                        label = "셔플",
                        isOn = headerData.isShuffling,
                        onClick = {
                            sendIntent(PlaylistDetailHeaderIntent.OnClickShuffle)
                        },
                    )
                    PlaybackToggleButton(
                        modifier = Modifier.wrapContentWidth(),
                        imageVector = Icons.Rounded.Repeat,
                        label = "반복",
                        isOn = headerData.isRepeating,
                        onClick = {
                            sendIntent(PlaylistDetailHeaderIntent.OnClickRepeat)
                        },
                    )
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
            // 36dp 로 누르면 터치 타깃이 깎이고 48dp 로 늘리면 12sp 글자에 비해 뚱뚱하다.
            // 40dp(M3 버튼 기본값)를 바닥으로 적되 고정하지는 않는다 — 고정하면 글꼴 배율을
            // 키웠을 때 글자 상하가 깎인다.
            Row(
                modifier = Modifier.heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isBasePlaylist) {
                    OutlinedButton(
                        onClick = {
                            sendIntent(PlaylistDetailHeaderIntent.OnClickAdd)
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.wrapContentWidth()
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
                    modifier = Modifier.wrapContentWidth()
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

private val TOGGLE_ICON_SIZE = 20.dp
private val TOGGLE_DOT_SIZE = 4.dp

/**
 * 켜짐을 아이콘 색과 **아래 점**으로 알린다. `Shuffle` 과 `Repeat` 는 Filled·Rounded 변종이
 * 20dp 에서 구분되지 않아, 점이 없으면 켜짐이 색으로만 전달된다.
 *
 * 점은 꺼짐일 때도 투명으로 자리를 지킨다 — 빼면 켤 때마다 아이콘이 위로 밀린다.
 */
@Composable
private fun PlaybackToggleButton(
    modifier: Modifier,
    imageVector: ImageVector,
    label: String,
    isOn: Boolean,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.primary else gray60,
        label = "playbackToggleColor",
    )

    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                modifier = Modifier.size(TOGGLE_ICON_SIZE),
                imageVector = imageVector,
                contentDescription = if (isOn) "$label 켜짐" else "$label 꺼짐",
                tint = color,
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(TOGGLE_DOT_SIZE)
                    .clip(CircleShape)
                    .background(if (isOn) color else Color.Transparent),
            )
        }
    }
}

@Preview
@Composable
private fun PlaybackToggleButtonPreview() {
    JellyTubeTheme {
        Row {
            PlaybackToggleButton(
                modifier = Modifier,
                imageVector = Icons.Rounded.Shuffle,
                label = "셔플",
                isOn = true,
                onClick = {},
            )
            PlaybackToggleButton(
                modifier = Modifier,
                imageVector = Icons.Rounded.Repeat,
                label = "반복",
                isOn = false,
                onClick = {},
            )
        }
    }
}

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