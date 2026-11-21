package com.kintmin.presentation.ui.player_detail.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkBackdropFallback
import com.kintmin.presentation.theme.overlayContent
import com.kintmin.presentation.theme.overlayContentSecondary
import com.kintmin.presentation.theme.overlayScrimSoft
import com.kintmin.presentation.theme.overlayScrimStrong
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/** 줄마다 높이가 변하면 그라데이션이 커졌다 작아진다. 두 줄이 들어가는 높이로 고정한다. */
private val OVERLAY_HEIGHT = 56.dp

private const val FADE_MILLIS = 500

/**
 * 빈 줄이 이보다 짧게 스치면 막을 내리지 않는다. 간주처럼 가사가 잠깐 끊기는 구간마다
 * 깜빡이는 것을 막는다 — 다음 줄이 이 안에 오면 글자만 바뀐다.
 */
private val HIDE_DELAY = 1.seconds

/** 지금 가사 한 줄을 아트워크 위에 얹는다. 보여 줄 줄이 없으면 막까지 걷는다. */
@Composable
fun CurrentLyricOverlayView(
    line: String,
    translation: String?,
    modifier: Modifier = Modifier,
) {
    // 사라지는 것만 늦춘다. 나타나는 것은 늦추면 가사가 뒤늦게 뜬다.
    var isVisible by remember { mutableStateOf(line.isNotBlank()) }
    // 막이 내려가는 동안 글자가 먼저 비면 빈 막만 남는다. 마지막 줄을 들고 있는다.
    var shownLine by remember { mutableStateOf(line) }
    var shownTranslation by remember { mutableStateOf(translation) }

    LaunchedEffect(line, translation) {
        if (line.isNotBlank()) {
            shownLine = line
            shownTranslation = translation
            isVisible = true
        } else {
            delay(HIDE_DELAY)
            isVisible = false
        }
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(tween(FADE_MILLIS)),
        exit = fadeOut(tween(FADE_MILLIS)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(OVERLAY_HEIGHT)
                // 아트워크가 무엇이든 글자가 읽혀야 한다. 아래로 갈수록 짙어지는 막을 깐다.
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to overlayScrimSoft,
                        1f to overlayScrimStrong,
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 번역이 있으면 원문과 한 줄씩 나눠 높이를 지킨다
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = shownLine,
                    color = overlayContent,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (shownTranslation == null) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                shownTranslation?.let { translationText ->
                    Text(
                        text = translationText,
                        color = overlayContentSecondary,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CurrentLyricOverlayViewPreview() {
    JellyTubeTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(artworkBackdropFallback[0]),
        ) {
            CurrentLyricOverlayView(
                line = "今の歌詞の一行がここに表示されます",
                translation = "이 노래의 현재 가사 한 줄이 여기에 표시됩니다",
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
