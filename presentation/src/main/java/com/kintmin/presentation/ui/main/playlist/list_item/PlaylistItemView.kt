package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import java.io.File

@Composable
fun PlaylistItemView(
    modifier: Modifier,
    data: PlaylistItemUiState,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(
            data.imageFileFullPath?.let { File(it) } ?: androidx.media3.session.R.drawable.media3_icon_artist)
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    Column(
        modifier = modifier.padding(12.dp).clickable {
            sendIntent(PlaylistIntent.OnClickPlaylistItem(data))
        },
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(4))
                .background(Color.Gray)
        )
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text = data.name,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier,
            text = data.durationString,
            fontSize = 12.sp,
            lineHeight = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier,
            text = data.audioMediaCountString,
            fontSize = 12.sp,
            lineHeight = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistItemPreview() {
    JellyTubeTheme {
        PlaylistItemView(
            modifier = Modifier.width(180.dp),
            PlaylistItemUiState.getMock(),
            sendIntent = {},
        )
    }
}