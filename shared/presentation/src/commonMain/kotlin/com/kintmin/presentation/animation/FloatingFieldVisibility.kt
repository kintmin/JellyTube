package com.kintmin.presentation.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme

private const val FLOATING_FIELD_ANIMATION_MILLIS = 300

@Composable
fun FloatingFieldVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val spec = tween<Float>(FLOATING_FIELD_ANIMATION_MILLIS, easing = FastOutSlowInEasing)
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = expandVertically(tween(FLOATING_FIELD_ANIMATION_MILLIS, easing = FastOutSlowInEasing)) + fadeIn(spec),
        exit = shrinkVertically(tween(FLOATING_FIELD_ANIMATION_MILLIS, easing = FastOutSlowInEasing)) + fadeOut(spec),
        content = content,
    )
}

@Preview
@Composable
private fun FloatingFieldVisibilityPreview() {
    JellyTubeTheme {
        FloatingFieldVisibility(visible = true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}
