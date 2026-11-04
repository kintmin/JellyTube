package com.kintmin.presentation.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * iOS 웹뷰는 `WKWebView` 를 `UIKitView` 로 올려야 한다.
 *
 * 스킴 라우팅(`intent://` 대응물 없음), 쿠키 정책, Referer 헤더까지 Android 와
 * 설계가 달라 iOS 화면 작업과 함께 다룬다. 그때까지 안내 문구만 보여준다 —
 * 빈 화면을 띄우면 "로딩이 안 되는 버그"로 오해한다.
 */
private class IosWebViewHandle : WebViewHandle {

    override val canGoBack: Boolean = false

    override val currentUrl: String? = null

    override val isCreationFailed: Boolean = false

    override fun goBack() = Unit

    override fun loadUrl(url: String, clearHistoryAfterLoad: Boolean) = Unit

    override fun retryCreation() = Unit
}

@Composable
actual fun rememberWebViewHandle(): WebViewHandle = remember { IosWebViewHandle() }

@Composable
actual fun PlatformWebView(
    handle: WebViewHandle,
    url: String,
    onUrlChanged: (String) -> Unit,
    onExternalSchemeFailed: (message: String) -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("이 기기에서는 아직 웹 검색을 쓸 수 없습니다.")
    }
}
