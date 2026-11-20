package com.kintmin.presentation.ui.player_detail.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.animation.animatePlaybackSeconds
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.seaBlue80

private val TRACK_HEIGHT = 4.dp
private val PIN_WIDTH = 2.dp
private val PIN_HEIGHT = 14.dp

@Composable
fun PlaybackSliderView(
    playbackSeconds: Long,
    currentPositionSeconds: Float,
    isPlaying: Boolean,
    repeatRangeStartSeconds: Long?,
    repeatRangeEndSeconds: Long?,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderWidth by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    val smoothPositionSeconds = animatePlaybackSeconds(
        targetSeconds = currentPositionSeconds,
        snap = isDragging || !isPlaying,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { sliderWidth = it.width },
        contentAlignment = Alignment.CenterStart,
    ) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..playbackSeconds.toFloat(),
            value = smoothPositionSeconds,
            onValueChange = {
                isDragging = true
                onValueChange(it)
            },
            onValueChangeFinished = {
                isDragging = false
                onValueChangeFinished()
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
            ),
        )
        RepeatRangeBand(
            startSeconds = repeatRangeStartSeconds,
            endSeconds = repeatRangeEndSeconds,
            playbackSeconds = playbackSeconds,
            sliderWidth = sliderWidth,
        )
        RepeatRangePin(
            seconds = repeatRangeStartSeconds,
            playbackSeconds = playbackSeconds,
            sliderWidth = sliderWidth,
        )
        RepeatRangePin(
            seconds = repeatRangeEndSeconds,
            playbackSeconds = playbackSeconds,
            sliderWidth = sliderWidth,
        )
    }
}

/**
 * 구간을 트랙 위에 덮어 칠한다. 반투명이라 아래의 재생 진행이 그대로 읽힌다.
 *
 * 핀과 밴드의 가로 위치는 트랙 폭을 thumb 여백까지 셈하지 않는다 — 몇 px 어긋나도
 * 구간이 어디쯤인지 읽히면 된다.
 */
@Composable
private fun RepeatRangeBand(
    startSeconds: Long?,
    endSeconds: Long?,
    playbackSeconds: Long,
    sliderWidth: Int,
) {
    if (startSeconds == null || endSeconds == null || sliderWidth == 0) return

    val density = LocalDensity.current
    val startRatio = (startSeconds.toFloat() / playbackSeconds).coerceIn(0f, 1f)
    val endRatio = (endSeconds.toFloat() / playbackSeconds).coerceIn(0f, 1f)
    val startOffset = with(density) { (sliderWidth * startRatio).toDp() }
    val bandWidth = with(density) { (sliderWidth * (endRatio - startRatio)).toDp() }

    Box(
        modifier = Modifier
            .offset(x = startOffset)
            .width(bandWidth)
            .height(TRACK_HEIGHT)
            .clip(RoundedCornerShape(TRACK_HEIGHT / 2))
            .background(seaBlue80.copy(alpha = 0.55f)),
    )
}

@Composable
private fun RepeatRangePin(
    seconds: Long?,
    playbackSeconds: Long,
    sliderWidth: Int,
) {
    if (seconds == null || sliderWidth == 0) return

    val density = LocalDensity.current
    val ratio = (seconds.toFloat() / playbackSeconds).coerceIn(0f, 1f)
    val pinOffset = with(density) { (sliderWidth * ratio).toDp() }

    Box(
        modifier = Modifier
            .offset(x = pinOffset - PIN_WIDTH / 2)
            .width(PIN_WIDTH)
            .height(PIN_HEIGHT)
            .background(seaBlue80),
    )
}

@Preview
@Composable
private fun PlaybackSliderViewPreview() {
    JellyTubeTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1D1C1A)),
        ) {
            PlaybackSliderView(
                playbackSeconds = 300,
                currentPositionSeconds = 90f,
                isPlaying = false,
                repeatRangeStartSeconds = 60,
                repeatRangeEndSeconds = 240,
                onValueChange = {},
                onValueChangeFinished = {},
            )
        }
    }
}

@Preview
@Composable
private fun PlaybackSliderViewWithoutRangePreview() {
    JellyTubeTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1D1C1A)),
        ) {
            PlaybackSliderView(
                playbackSeconds = 300,
                currentPositionSeconds = 90f,
                isPlaying = false,
                repeatRangeStartSeconds = null,
                repeatRangeEndSeconds = null,
                onValueChange = {},
                onValueChangeFinished = {},
            )
        }
    }
}
