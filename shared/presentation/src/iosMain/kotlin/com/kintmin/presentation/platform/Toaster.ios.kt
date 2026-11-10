package com.kintmin.presentation.platform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Android `Toast.LENGTH_SHORT` / `LENGTH_LONG` 과 같은 길이로 맞춘다. */
internal const val SHORT_DURATION_MILLIS = 2_000L
internal const val LONG_DURATION_MILLIS = 3_500L

internal data class ToastMessage(
    val id: Long,
    val text: String,
    val durationMillis: Long,
)

/**
 * 띄울 메시지를 [PlatformToastHost] 로 흘린다.
 * 띄우는 쪽은 화면 어디에나 있고 그리는 쪽은 앱 루트 하나라 전역이다.
 */
internal object ToastHost {

    private var nextId = 0L
    private val _current = MutableStateFlow<ToastMessage?>(null)
    val current: StateFlow<ToastMessage?> = _current

    fun show(text: String, durationMillis: Long) {
        nextId += 1
        _current.value = ToastMessage(id = nextId, text = text, durationMillis = durationMillis)
    }

    fun clear(id: Long) {
        if (_current.value?.id == id) _current.value = null
    }
}

internal object IosToaster : Toaster {

    override fun show(message: String) = ToastHost.show(message, SHORT_DURATION_MILLIS)

    override fun showLong(message: String) = ToastHost.show(message, LONG_DURATION_MILLIS)
}

@Composable
actual fun rememberToaster(): Toaster = IosToaster

@Composable
actual fun PlatformToastHost() {
    val message by ToastHost.current.collectAsState()

    // 사라지는 애니메이션이 도는 동안에도 글자가 남아 있어야 해서 마지막 값을 붙잡아 둔다.
    var lastText by remember { mutableStateOf("") }

    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        lastText = current.text
        delay(current.durationMillis)
        ToastHost.clear(current.id)
    }

    Box(
        // 터치를 가로채지 않는다 — 포인터 핸들러가 없으면 아래 화면이 그대로 받는다.
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut(),
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = lastText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}
