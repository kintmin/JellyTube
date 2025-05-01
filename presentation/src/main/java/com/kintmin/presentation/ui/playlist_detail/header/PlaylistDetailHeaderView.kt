package com.kintmin.presentation.ui.playlist_detail.header

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun PlaylistDetailHeaderView(
    headerData: PlaylistDetailHeaderUiState,
    sendIntent: (PlaylistDetailHeaderIntent) -> Unit,
) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(
            androidx.media3.session.R.drawable.media3_icon_artist
        )
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

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
                )
                Text(
                    text = headerData.playlistSubtitle,
                    fontSize = 12.sp,
                    lineHeight = 10.sp,
                    overflow = TextOverflow.Ellipsis,
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
                    tint = if (headerData.isShuffling) Color(0xFF1DB954) else Color.Gray
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
                    tint = if (headerData.isRepeating) Color(0xFF1DB954) else Color.Gray
                )
            }
        }
        Row(
            modifier = Modifier.height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    sendIntent(PlaylistDetailHeaderIntent.OnClickAddAudioMediaInPlaylist)
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
                    sendIntent(PlaylistDetailHeaderIntent.OnClickEditPlaylist)
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
            ElevatedButton(
                onClick = {
                    sendIntent(PlaylistDetailHeaderIntent.OnClickPlay)
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicControlsPreview() {
    JellyTubeTheme {
        PlaylistDetailHeaderView(
            sendIntent = {},
            headerData = PlaylistDetailHeaderUiState.getMock(),
        )
    }
}