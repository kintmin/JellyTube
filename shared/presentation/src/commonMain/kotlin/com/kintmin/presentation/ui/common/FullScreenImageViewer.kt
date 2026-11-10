package com.kintmin.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.AsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.kintmin.presentation.platform.PlatformFullScreenDialog
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.custom_ui.zoomable.ZoomContentAlignment
import com.kintmin.presentation.ui.custom_ui.zoomable.ZoomLimitMode
import com.kintmin.presentation.ui.custom_ui.zoomable.ZoomableView
import kotlinx.coroutines.delay
import okio.Path.Companion.toPath

@Composable
fun FullScreenImageViewer(
    imageFileFullPath: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageFileFullPath == null) return

    PlatformFullScreenDialog(onDismissRequest = onDismiss, hideSystemBars = true) {
        var isShowCloseButton by remember { mutableStateOf(true) }

        LaunchedEffect(isShowCloseButton) {
            if (!isShowCloseButton) return@LaunchedEffect
            delay(2_000)
            isShowCloseButton = false
        }

        val platformContext = LocalPlatformContext.current
        val imageFile = remember(imageFileFullPath) { imageFileFullPath.toPath() }
        val imageRequest = remember(platformContext, imageFile) {
            ImageRequest.Builder(platformContext)
                .data(imageFile)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }
        var imageSize by remember(imageFileFullPath) { mutableStateOf<Pair<Int, Int>?>(null) }

        FullScreenImageViewerContent(
            imageSize = imageSize,
            isShowCloseButton = isShowCloseButton,
            onClickClose = onDismiss,
            onTapImage = { isShowCloseButton = !isShowCloseButton },
            modifier = modifier,
        ) { imageModifier ->
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onState = { state ->
                    val image = (state as? AsyncImagePainter.State.Success)?.result?.image
                    if (image != null) {
                        imageSize = image.width to image.height
                    }
                },
                modifier = imageModifier,
            )
        }
    }
}

@Composable
private fun FullScreenImageViewerContent(
    imageSize: Pair<Int, Int>?,
    isShowCloseButton: Boolean,
    onClickClose: () -> Unit,
    onTapImage: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) boxWithConstraints@{
            val imageModifier = if (imageSize != null) {
                val imageRatio = imageSize.first.toFloat() / imageSize.second.toFloat()
                val screenRatio = this@boxWithConstraints.maxWidth.value / this@boxWithConstraints.maxHeight.value

                if (imageRatio >= screenRatio) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.fillMaxHeight()
                }
            } else {
                Modifier.fillMaxSize()
            }

            ZoomableView(
                modifier = Modifier.fillMaxSize(),
                zoomLimitMode = ZoomLimitMode.Auto,
                contentAlignment = ZoomContentAlignment.Middle,
                onTap = onTapImage,
            ) {
                image(imageModifier)
            }
        }

        if (isShowCloseButton) {
            IconButton(
                onClick = onClickClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(end = 16.dp, top = 16.dp)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
        }
    }
}

@Preview
@Composable
private fun FullScreenImageViewerPreview() {
    JellyTubeTheme {
        FullScreenImageViewerContent(
            imageSize = 1200 to 800,
            isShowCloseButton = true,
            onClickClose = {},
            onTapImage = {},
        ) { imageModifier ->
            Box(
                modifier = imageModifier.background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp),
                )
                Text(
                    text = "Full Screen Image Viewer",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                )
            }
        }
    }
}
