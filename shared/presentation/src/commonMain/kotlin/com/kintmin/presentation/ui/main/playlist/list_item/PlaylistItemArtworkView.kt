package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.animation.NowPlayingBars
import com.kintmin.presentation.animation.animatePlayingGlow
import com.kintmin.presentation.animation.playingHalo
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkAccentFallback
import com.kintmin.presentation.theme.overlayScrimStrong
import com.kintmin.presentation.ui.common.AudioArtwork

/** [isNowPlaying] 면 둘레가 [accentColor] 로 빛나고 모서리에 재생 중 막대가 선다. */
@Composable
fun PlaylistItemArtworkView(
    imageFileFullPath: String?,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
) {
    val shape = MaterialTheme.shapes.small
    val glow = animatePlayingGlow(isPlaying = isNowPlaying && isPlaying)

    AudioArtwork(
        imageFileFullPath = imageFileFullPath,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .playingHalo(glow, accentColor, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface),
        placeholderIconSize = 36.dp,
    ) {
        if (!isNowPlaying) return@AudioArtwork
        // 막대 색이 대비를 맞춘 기준 면이 이 scrim 이다. 플레이리스트 표지 위에서도 같은 바닥을 깐다.
        NowPlayingBars(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(overlayScrimStrong)
                // 기둥은 Canvas 라 스크린리더에 아무것도 남기지 않는다.
                .semantics { contentDescription = if (isPlaying) "재생 중" else "일시정지" },
            isPlaying = isPlaying,
            color = accentColor,
        )
    }
}

@Preview
@Composable
fun PlaylistItemArtworkNowPlayingPreview() {
    JellyTubeTheme {
        Box(modifier = Modifier.width(180.dp).padding(12.dp)) {
            PlaylistItemArtworkView(
                imageFileFullPath = null,
                isNowPlaying = true,
                isPlaying = true,
                accentColor = artworkAccentFallback,
            )
        }
    }
}
