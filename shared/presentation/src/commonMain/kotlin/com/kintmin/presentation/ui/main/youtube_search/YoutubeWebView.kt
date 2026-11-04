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
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kintmin.presentation.platform.PlatformWebView
import com.kintmin.presentation.platform.WebViewHandle
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray80
import androidx.compose.ui.tooling.preview.Preview

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
            modifier = modifier.background(gray80),
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

/**
 * 플랫폼 웹뷰를 만들지 못했을 때의 재시도 화면.
 *
 * Android 는 시스템 WebView APK 가 실행 중 업데이트되면 생성이 실패할 수 있고
 * 앱 코드로 예방할 수 없다. 크래시 대신 여기로 떨어뜨리고, 업데이트가 끝난 뒤
 * 재시도하면 정상 생성되는 경우가 많다.
 */
@Composable
private fun WebViewCreationFailedView(
    onClickRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(gray80),
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
