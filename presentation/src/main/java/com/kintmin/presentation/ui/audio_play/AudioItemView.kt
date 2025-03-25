package com.kintmin.presentation.ui.audio_play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.kintmin.presentation.theme.YTMusicBoxTheme
import com.kintmin.presentation.ui.audio_play.model.AudioPlayUiState
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Composable
fun AudioItemView(
    data: AudioPlayUiState,
    onClickPlay: (AudioPlayUiState) -> Unit = {},
    onClickOpenEdit: (AudioPlayUiState) -> Unit = {},
) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(
            data.imageFileFullPath?.let { File(it) }
                ?: androidx.media3.session.R.drawable.media3_icon_artist
        )
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(modifier = Modifier
            .weight(1f)
            .clickable { onClickPlay(data) }
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16))
                    .background(Color.Gray)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = data.mediaName,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = data.extraString,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterVertically),
            onClick = { onClickOpenEdit(data) }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AudioItemPreview() {
    YTMusicBoxTheme {
        AudioItemView(
            AudioPlayUiState(
                id = "",
                mediaName = "Audio Title",
                description = "Audio Description",
                artist = "Artist",
                audioDuration = 1234.seconds,
                audioFileFullPath = "",
                imageFileFullPath = "",
            )
        )
    }
}