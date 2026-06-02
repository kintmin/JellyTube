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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.FullScreenImageViewer
import java.io.File

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
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(headerData.imageFileFullPath))
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
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
                    IconButton(
                        onClick = {
                            sendIntent(PlaylistDetailHeaderIntent.OnClickShuffle)
                        },
                        modifier = Modifier
                            .wrapContentWidth(Alignment.End)
                            .weight(1f)
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = if (headerData.isShuffling) Icons.Filled.Shuffle else Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (headerData.isShuffling) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(
                        onClick = {
                            sendIntent(PlaylistDetailHeaderIntent.OnClickRepeat)
                        },
                        modifier = Modifier
                            .wrapContentWidth()
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = if (headerData.isRepeating) Icons.Filled.Repeat else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (headerData.isRepeating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
                modifier = Modifier.height(36.dp),
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
                            contentDescription = "Add"
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
                        contentDescription = "Edit"
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicControlsPreview() {
    JellyTubeTheme {
        PlaylistDetailHeaderView(
            innerPaddingValues = PaddingValues(top = 24.dp),
            sendIntent = {},
            isBasePlaylist = false,
            headerData = PlaylistDetailHeaderUiState.getMock(),
        )
    }
}