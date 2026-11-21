package com.kintmin.presentation.ui.player_bar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
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
import com.kintmin.presentation.theme.gray10
import com.kintmin.presentation.theme.gray60
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

private val BAR_HEIGHT = 64.dp
private val ARTWORK_SIZE = 48.dp
private val PROGRESS_HEIGHT = 2.dp

// 영역은 최소 터치 타깃이라 줄이지 않는다. 아이콘을 M3 비율(약 60%)로 키워 영역을 메운다.
private val PLAY_BUTTON_SIZE = 48.dp
private val PLAY_ICON_SIZE = 28.dp

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
    ) {
        PlayerBarProgress(data = data)
        Row(
            modifier = Modifier
                .height(BAR_HEIGHT)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(
                    enabled = data.id.isNotBlank(),
                    onClick = onClickBar,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artworkModifier = Modifier
                .padding(start = 8.dp)
                .size(ARTWORK_SIZE)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface)

            if (data.imageFileFullPath == null) {
                Box(
                    modifier = artworkModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = gray60,
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
                    modifier = artworkModifier,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
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
                    // 폴링마다 다시 그려지는 숫자라 비례폭이면 초마다 글자가 밀린다.
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    color = gray60,
                )
            }
            IconButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(PLAY_BUTTON_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                onClick = {
                    sendIntent(PlayerBarIntent.OnClickPlayOrPauseButton)
                },
            ) {
                Icon(
                    modifier = Modifier.size(PLAY_ICON_SIZE),
                    imageVector = if (data.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (data.isPlaying) "일시정지" else "재생",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

/** 프레임마다 바뀌는 재생 위치를 여기서만 읽는다. 바깥에서 읽으면 재생바 전체가 매 프레임 다시 그려진다. */
@Composable
private fun PlayerBarProgress(data: PlayerBarUiState) {
    val playbackSeconds = data.playbackDuration.inWholeMilliseconds / 1000f
    val currentSeconds = animatePlaybackSeconds(
        targetSeconds = data.currentDuration.inWholeMilliseconds / 1000f,
        snap = !data.isPlaying,
    )
    val playedRatio =
        if (playbackSeconds > 0f) (currentSeconds / playbackSeconds).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PROGRESS_HEIGHT)
            .background(gray10),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(playedRatio)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.tertiary),
        )
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

@Preview
@Composable
private fun PlayerBarProgressPreview() {
    JellyTubeTheme {
        PlayerBarProgress(data = PlayerBarUiState.getMock())
    }
}
