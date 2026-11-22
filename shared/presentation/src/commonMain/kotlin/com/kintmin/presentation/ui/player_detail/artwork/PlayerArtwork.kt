package com.kintmin.presentation.ui.player_detail.artwork

import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Scale
import com.kintmin.presentation.platform.extractDominantColor
import com.kintmin.presentation.theme.artworkBackdropFallback
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okio.Path.Companion.toPath

const val ARTWORK_TRANSITION_MILLIS = 500

/** 그림과 배경색을 한 값으로 묶는다 — 따로 도착하면 둘이 다른 프레임에 바뀐다. [painter] 가 null 이면 그림이 없는 곡이다. */
@Immutable
class PlayerArtwork(
    val imageFileFullPath: String?,
    val painter: Painter?,
    val gradientColors: List<Color>,
)

/** 첫 값, 그리고 같은 곡을 칸 크기만 바꿔 다시 읽은 값은 애니메이션 없이 바로 붙인다. */
fun Transition.Segment<PlayerArtwork?>.isInstant(): Boolean =
    initialState == null || initialState?.imageFileFullPath == targetState?.imageFileFullPath

/**
 * 곡이 바뀌어도 새 그림을 다 읽을 때까지 이전 값을 돌려준다. 첫 값이 오기 전에는 null 이다.
 * [size] 는 그릴 칸의 크기이고, 재기 전(0)에는 읽지 않는다.
 */
@Composable
fun rememberPlayerArtwork(imageFileFullPath: String?, size: IntSize): PlayerArtwork? {
    val context = LocalPlatformContext.current
    var artwork by remember { mutableStateOf<PlayerArtwork?>(null) }
    LaunchedEffect(imageFileFullPath, size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        artwork = loadPlayerArtwork(context, imageFileFullPath, size)
    }
    return artwork
}

private suspend fun loadPlayerArtwork(
    context: PlatformContext,
    imageFileFullPath: String?,
    size: IntSize,
): PlayerArtwork = coroutineScope {
    if (imageFileFullPath.isNullOrBlank()) {
        return@coroutineScope PlayerArtwork(imageFileFullPath, null, artworkBackdropFallback)
    }

    val image = async {
        // 가로 화면에서는 칸 높이가 0 이 되는데 Coil 은 0 크기 요청에서 예외를 던진다. 배경색은 그래도 뽑는다.
        if (size.width <= 0 || size.height <= 0) return@async null
        val request = ImageRequest.Builder(context)
            .data(imageFileFullPath.toPath())
            .size(size.width, size.height)
            .scale(Scale.FILL)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
        SingletonImageLoader.get(context).execute(request).image
    }
    val gradientColors = async { extractGradientFromArtwork(imageFileFullPath) }

    PlayerArtwork(
        imageFileFullPath = imageFileFullPath,
        painter = image.await()?.asPainter(context),
        gradientColors = gradientColors.await() ?: artworkBackdropFallback,
    )
}

private suspend fun extractGradientFromArtwork(path: String): List<Color>? {
    val seed = Color(extractDominantColor(path) ?: return null)

    return listOf(
        seed.lighten(0.2f),
        seed.darken(0.55f),
        seed.darken(0.82f),
    )
}

private fun Color.lighten(factor: Float): Color {
    val safe = factor.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * safe,
        green = green + (1f - green) * safe,
        blue = blue + (1f - blue) * safe,
        alpha = 1f,
    )
}

private fun Color.darken(factor: Float): Color {
    val safe = factor.coerceIn(0f, 1f)
    val mul = 1f - safe
    return Color(
        red = red * mul,
        green = green * mul,
        blue = blue * mul,
        alpha = 1f,
    )
}
