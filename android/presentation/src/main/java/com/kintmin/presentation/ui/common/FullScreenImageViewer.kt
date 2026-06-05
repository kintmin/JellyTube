package com.kintmin.presentation.ui.common

import android.graphics.Color as AndroidColor
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.custom_ui.zoomable.ZoomContentAlignment
import com.kintmin.presentation.ui.custom_ui.zoomable.ZoomLimitMode
import com.kintmin.presentation.ui.custom_ui.zoomable.ZoomableView
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun FullScreenImageViewer(
    imageFileFullPath: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageFileFullPath == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = LocalView.current
        var isShowCloseButton by remember { mutableStateOf(true) }

        val window = (view.parent as? DialogWindowProvider)?.window

        SideEffect {
            window ?: return@SideEffect
            window.setBackgroundDrawable(AndroidColor.TRANSPARENT.toDrawable())
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
        }

        DisposableEffect(window) {
            if (window == null) return@DisposableEffect onDispose {}

            val originalDecorFitsSystemWindows = true
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())

            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, originalDecorFitsSystemWindows)
            }
        }

        LaunchedEffect(isShowCloseButton) {
            if (!isShowCloseButton) return@LaunchedEffect
            delay(2_000)
            isShowCloseButton = false
        }

        val context = LocalContext.current
        val imageFile = remember(imageFileFullPath) { File(imageFileFullPath) }
        val imageRequest = remember(context, imageFile) {
            ImageRequest.Builder(context)
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
                    val drawable = (state as? AsyncImagePainter.State.Success)?.result?.drawable
                    if (drawable != null) {
                        imageSize = drawable.intrinsicWidth to drawable.intrinsicHeight
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

@Preview(showBackground = true)
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
