package com.kintmin.presentation.ui.player_detail.artwork

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkBackdropFallback
import com.kintmin.presentation.theme.overlayContentMuted
import com.kintmin.presentation.theme.overlayScrimSoft

/** 배경 그라데이션과 같은 [transition] 을 받아야 그림과 배경이 같은 프레임에 바뀐다. */
@Composable
fun PlayerArtworkView(
    transition: Transition<PlayerArtwork?>,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    initialState == null -> EnterTransition.None togetherWith ExitTransition.None
                    // 둘 다 반투명해지면 겹친 가운데에서 뒤 바닥이 비친다. 그림이 들어올 때는 나가는 쪽을 덮을 때까지 둔다.
                    targetState?.painter != null ->
                        fadeIn(tween(ARTWORK_TRANSITION_MILLIS)) togetherWith ExitTransition.KeepUntilTransitionsFinished
                    else -> fadeIn(tween(ARTWORK_TRANSITION_MILLIS)) togetherWith fadeOut(tween(ARTWORK_TRANSITION_MILLIS))
                }
            },
        ) { artwork ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val painter = artwork?.painter
                when {
                    artwork == null -> Unit
                    painter == null -> Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = overlayContentMuted,
                    )
                    else -> Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        overlay()
    }
}

@Preview
@Composable
private fun PlayerArtworkViewPreview() {
    JellyTubeTheme {
        PlayerArtworkView(
            transition = updateTransition(PlayerArtwork(null, artworkBackdropFallback)),
            modifier = Modifier
                .size(300.dp)
                .background(overlayScrimSoft),
        )
    }
}
