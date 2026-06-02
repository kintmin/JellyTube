package com.kintmin.presentation.ui.player_bar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray20
import com.kintmin.presentation.theme.gray60
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBar(
    data: PlayerBarUiState,
    sendIntent: (PlayerBarIntent) -> Unit,
    onClickBar: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            lifecycleOwner.lifecycleScope.launch {
                while(isActive) {
                    delay(300)
                    sendIntent(PlayerBarIntent.OnRefreshMediaData)
                }
            }
        }
    }

    if (data.id == "") return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceDim)
                .height(16.dp)
        ) {
            Slider(
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..data.playbackDuration.inWholeSeconds.toFloat(),
                value = data.currentDuration.inWholeSeconds.toFloat(),
                onValueChange = {
                    sendIntent(PlayerBarIntent.OnChangeTimeSlider(it))
                },
                onValueChangeFinished = {
                    sendIntent(PlayerBarIntent.OnChangeFinishTimeSlider)
                },
                track = { sliderState ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(gray20)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(data.currentDuration.inWholeSeconds / data.playbackDuration.inWholeSeconds.toFloat())
                            .height(16.dp)
                            .background(gray60)
                    )
                },
                thumb = {},
            )
        }
        Row(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceDim)
                .clickable(
                    enabled = data.id.isNotBlank(),
                    onClick = onClickBar,
                )
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
                        .background(MaterialTheme.colorScheme.onSurface)
                        .align(Alignment.CenterVertically),
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
                        imageVector = if (data.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
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
