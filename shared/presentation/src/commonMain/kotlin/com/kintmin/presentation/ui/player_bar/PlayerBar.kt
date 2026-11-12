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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.kintmin.presentation.animation.PLAYBACK_POLLING_INTERVAL_MILLIS
import com.kintmin.presentation.animation.animatePlaybackSeconds
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray20
import com.kintmin.presentation.theme.gray60
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBar(
    data: PlayerBarUiState,
    sendIntent: (PlayerBarIntent) -> Unit,
    onClickBar: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    // 보이는(STARTED) 동안만 폴링한다. RESUMED 는 화면 전환 애니메이션이 끝나야 와서 바가 늦게 뜬다.
    // repeatOnLifecycle 안에서 lifecycleScope.launch 로 빠져나가면 화면을 벗어나도 루프가 취소되지 않는다.
    LifecycleStartEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                sendIntent(PlayerBarIntent.OnRefreshMediaData)
                delay(PLAYBACK_POLLING_INTERVAL_MILLIS.toLong())
            }
        }
        onStopOrDispose { job.cancel() }
    }

    if (data.id == "") return

    var isDragging by remember { mutableStateOf(false) }
    val playbackSeconds = data.playbackDuration.inWholeMilliseconds / 1000f
    val currentSeconds = animatePlaybackSeconds(
        targetSeconds = data.currentDuration.inWholeMilliseconds / 1000f,
        snap = isDragging || !data.isPlaying,
    )

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
                valueRange = 0f..playbackSeconds,
                value = currentSeconds,
                onValueChange = {
                    isDragging = true
                    sendIntent(PlayerBarIntent.OnChangeTimeSlider(it))
                },
                onValueChangeFinished = {
                    isDragging = false
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
                            .fillMaxWidth(if (playbackSeconds > 0f) (currentSeconds / playbackSeconds).coerceIn(0f, 1f) else 0f)
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
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(data.imageFileFullPath.toPath())
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

@Preview
@Composable
fun PlayerBarPreview() {
    JellyTubeTheme {
        PlayerBar(
            data = PlayerBarUiState.getMock(),
            sendIntent = {},
        )
    }
}
