package com.kintmin.presentation.ui.main.youtube_search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kintmin.presentation.platform.PlatformWebView
import com.kintmin.presentation.platform.WebViewHandle
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentSubtle

private const val HOME_URL = "https://m.youtube.com/"

@Composable
fun YoutubeWebView(
    currentUrl: String,
    webViewHandle: WebViewHandle,
    sendIntent: (YoutubeDownloadIntent) -> Unit,
    onNavigateToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(contentSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Text("WebView Preview")
        }
        return
    }

    if (webViewHandle.isCreationFailed) {
        WebViewCreationFailedView(
            onClickRetry = webViewHandle::retryCreation,
            modifier = modifier,
        )
        return
    }

    PlatformWebView(
        handle = webViewHandle,
        url = currentUrl,
        onUrlChanged = { url -> sendIntent(YoutubeDownloadIntent.OnChangeUrl(url)) },
        onExternalSchemeFailed = { message ->
            sendIntent(YoutubeDownloadIntent.OnShowToast(message))
        },
        modifier = modifier,
    )

    // 웹 히스토리 -> 홈 -> 탭 전환 순으로 뒤로가기를 소비한다.
    NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) {
        when {
            webViewHandle.canGoBack -> webViewHandle.goBack()
            webViewHandle.currentUrl != HOME_URL ->
                webViewHandle.loadUrl(HOME_URL, clearHistoryAfterLoad = true)

            else -> onNavigateToPlaylist()
        }
    }
}

@Composable
private fun WebViewCreationFailedView(
    onClickRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(contentSubtle),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "웹뷰에 문제가 생겼습니다.\n다시 시도해주세요.",
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClickRetry) {
                Text("다시 시도")
            }
        }
    }
}

@Preview
@Composable
fun WebViewCreationFailedViewPreview() {
    JellyTubeTheme {
        WebViewCreationFailedView(onClickRetry = {})
    }
}
