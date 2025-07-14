package com.kintmin.presentation.ui.player_bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray20
import com.kintmin.presentation.theme.gray60
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBar(
    data: PlayerBarUiState,
    sendIntent: (PlayerBarIntent) -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(data.currentDuration.inWholeSeconds.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent),
    ) {
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .defaultMinSize(minHeight = 0.dp)
                .offset(y = 20.dp),
            valueRange = 0f..data.playbackDuration.inWholeSeconds.toFloat(),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                sendIntent(PlayerBarIntent.OnChangeTimeSlider(it))
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    enabled = true,
                    sliderState = sliderState,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                    colors = SliderDefaults.colors(
                        activeTrackColor = gray60,
                        inactiveTrackColor = gray20,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(0.dp)
                )
            },
            thumb = {
                Box(modifier = Modifier.size(0.dp))
            },
        )
        Row(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceDim)
        ) {
            if (data.imageFileFullPath == null) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface)
                        .align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(data.imageFileFullPath))
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(4))
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = data.title,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = data.timeString,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp),
                onClick = {
                    sendIntent(PlayerBarIntent.OnClickPlayOrPauseButton)
                }) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp),
                        imageVector = Icons.Rounded.PlayArrow, //if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerBarPreview() {
    JellyTubeTheme {
        PlayerBar(
            data = PlayerBarUiState.getMock(),
            sendIntent = {},
        )
    }
}