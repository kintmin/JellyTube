package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * 웹뷰 조작 핸들. 플랫폼 뷰 객체를 노출하지 않고 생명주기도 핸들이 소유한다.
 */
@Stable
interface WebViewHandle {

    val canGoBack: Boolean

    val currentUrl: String?

    /** 웹뷰를 쓸 수 없는 상태. 앱 코드로 예방할 수 없어 크래시 대신 재시도 UI 를 띄운다. */
    val isCreationFailed: Boolean

    fun goBack()

    /** [clearHistoryAfterLoad] 가 true 면 로드 완료 후 뒤로가기 히스토리를 비운다. */
    fun loadUrl(url: String, clearHistoryAfterLoad: Boolean = false)

    /** 생성 실패 상태를 풀고 다시 만들게 한다. */
    fun retryCreation()
}

@Composable
expect fun rememberWebViewHandle(): WebViewHandle

/**
 * 플랫폼 웹뷰를 그린다.
 *
 * @param onUrlChanged 방문 이력이 바뀔 때마다 현재 URL. 다운로드 대상 영상을 추적한다.
 * @param onExternalSchemeFailed `intent://` 등 외부 앱 스킴을 열지 못했을 때의 안내 문구.
 */
@Composable
expect fun PlatformWebView(
    handle: WebViewHandle,
    url: String,
    onUrlChanged: (String) -> Unit,
    onExternalSchemeFailed: (message: String) -> Unit,
    modifier: Modifier = Modifier,
)
