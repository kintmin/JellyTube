package com.kintmin.presentation.ui.player_bar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.animation.GLOW_MAX_ALPHA
import com.kintmin.presentation.animation.PlaybackPollingEffect
import com.kintmin.presentation.animation.animatePlaybackSeconds
import com.kintmin.presentation.animation.animatePlayingGlow
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkAccentFallback
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.emptyTrack
import com.kintmin.presentation.ui.common.AudioArtwork
import com.kintmin.presentation.ui.common.rememberArtworkAccentColor

private val BAR_HEIGHT = 64.dp
private val ARTWORK_SIZE = 48.dp
private val PROGRESS_HEIGHT = 2.dp

// 번짐이 트랙 위로 뻗는 높이. 칸을 차지하지 않으므로 목록 글자와 겹친다 — 길게 두지 않는다.
private val PROGRESS_GLOW_HEIGHT = 10.dp

// 재생 머리의 번짐 반경.
private val PROGRESS_HEAD_GLOW_RADIUS = 10.dp

// 지나온 구간의 번짐은 머리보다 약하게 둔다. 폭 전체에 균일하게 깔리면 빛이 아니라 배경 톤으로 읽힌다.
private const val PROGRESS_TRAIL_GLOW_RATIO = 0.4f

// 영역은 최소 터치 타깃이라 줄이지 않는다. 아이콘을 키워 영역을 메운다.
private val PLAY_BUTTON_SIZE = 48.dp
private val PLAY_ICON_SIZE = 28.dp

@Composable
fun PlayerBar(
    data: PlayerBarUiState,
    sendIntent: (PlayerBarIntent) -> Unit,
    onClickBar: () -> Unit = {},
) {
    PlaybackPollingEffect(whileStarted = true) { sendIntent(PlayerBarIntent.OnRefreshMediaData) }

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

            AudioArtwork(
                imageFileFullPath = data.imageFileFullPath,
                modifier = artworkModifier,
                placeholderIconSize = 22.dp,
            )
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
                    color = contentMuted,
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

    val accent = rememberArtworkAccentColor(data.imageFileFullPath) ?: artworkAccentFallback
    val glow = animatePlayingGlow(data.isPlaying)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(PROGRESS_HEIGHT),
    ) {
        drawRect(color = emptyTrack)

        val playedWidth = size.width * playedRatio
        if (playedWidth <= 0f) return@Canvas

        val glowStrength = glow.value

        // 번짐은 트랙 높이 밖(위쪽)으로 그린다 — 칸을 넓히면 Scaffold 가 그만큼 목록을 밀어낸다.
        // 그린 자리는 이 요소의 경계 밖이라 터치도 받지 않는다.
        //
        // 가장 진한 끝을 트랙 상단(y=0)에 둔다. 트랙 안까지 그리면 그 구간을 아래의 불투명한
        // 진행바가 덮어 번짐의 피크가 보이지 않는다.
        if (glowStrength > 0f) {
            val glowHeight = PROGRESS_GLOW_HEIGHT.toPx()
            val trailAlpha = GLOW_MAX_ALPHA * PROGRESS_TRAIL_GLOW_RATIO * glowStrength
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = trailAlpha)),
                    startY = -glowHeight,
                    endY = 0f,
                ),
                topLeft = Offset(0f, -glowHeight),
                size = Size(playedWidth, glowHeight),
                blendMode = BlendMode.Plus,
            )
        }

        drawRect(color = accent, size = Size(playedWidth, size.height))

        // 머리 번짐은 바를 그린 뒤에 얹는다. 먼저 깔면 불투명한 바가 가장 진한 중심을 덮는다.
        if (glowStrength > 0f) {
            val radius = PROGRESS_HEAD_GLOW_RADIUS.toPx()
            val center = Offset(playedWidth, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = GLOW_MAX_ALPHA * glowStrength),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
                blendMode = BlendMode.Plus,
            )
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

@Preview
@Composable
private fun PlayerBarProgressPreview() {
    JellyTubeTheme {
        PlayerBarProgress(data = PlayerBarUiState.getMock())
    }
}
