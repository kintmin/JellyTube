package com.kintmin.presentation.platform

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

private class AndroidWebViewHandle : WebViewHandle {

    var webView: WebView? by mutableStateOf(null)
        private set

    override var isCreationFailed: Boolean by mutableStateOf(false)
        private set

    /** 다음 로드가 끝난 뒤 히스토리를 비울지. 홈으로 되돌릴 때 쓴다. */
    var shouldClearHistoryAfterLoad: Boolean = false

    override val canGoBack: Boolean get() = webView?.canGoBack() == true

    override val currentUrl: String? get() = webView?.url

    override fun goBack() {
        webView?.goBack()
    }

    override fun loadUrl(url: String, clearHistoryAfterLoad: Boolean) {
        shouldClearHistoryAfterLoad = clearHistoryAfterLoad
        val target = webView ?: return
        target.loadUrl(url, mapOf("Referer" to "https://${target.context.packageName.lowercase()}"))
    }

    override fun retryCreation() {
        isCreationFailed = false
    }

    fun attach(webView: WebView) {
        this.webView = webView
    }

    fun markCreationFailed() {
        isCreationFailed = true
    }

    fun release() {
        webView?.apply {
            stopLoading()
            (parent as? ViewGroup)?.removeView(this)
            removeAllViews()
            destroy()
        }
        webView = null
    }
}

@Composable
actual fun rememberWebViewHandle(): WebViewHandle {
    val handle = remember { AndroidWebViewHandle() }
    DisposableEffect(handle) {
        onDispose { handle.release() }
    }
    return handle
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(
    handle: WebViewHandle,
    url: String,
    onUrlChanged: (String) -> Unit,
    onExternalSchemeFailed: (message: String) -> Unit,
    modifier: Modifier,
) {
    val androidHandle = handle as AndroidWebViewHandle

    // 로드는 url 이 "바뀔 때 한 번"만 한다.
    //
    // AndroidView 의 update 로 `view.url != url 이면 로드`를 계속 단언하면 안 된다.
    // 웹뷰도 스스로 이동하는 주체이고, view.url 은 로드가 커밋될 때 바로 바뀌는데
    // ViewModel 은 doUpdateVisitedHistory 를 거쳐야 갱신된다. 그 사이에 리컴포지션이
    // 일어나면 낡은 url 을 다시 로드해 뒤로가기가 되돌아가 버린다
    // (PlayerBar 가 300ms 폴링이라 그 창이 거의 항상 열린다).
    //
    // currentUrl 비교는 웹뷰가 스스로 이동해 올라온 값을 되로드하지 않기 위한 것이다.
    // webView 도 키에 넣는다. 이 효과는 AndroidView 의 factory 보다 먼저 돌 수 있어서
    // 인스턴스가 붙은 뒤 다시 실행되어야 첫 로드가 유실되지 않는다.
    LaunchedEffect(androidHandle.webView, url) {
        val view = androidHandle.webView ?: return@LaunchedEffect
        if (url.isNotBlank() && view.url != url) {
            androidHandle.loadUrl(url)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            androidHandle.webView ?: runCatching {
                createWebView(
                    context = context,
                    handle = androidHandle,
                    onUrlChanged = onUrlChanged,
                    onExternalSchemeFailed = onExternalSchemeFailed,
                )
            }.getOrElse {
                // 시스템 WebView(Android System WebView / Chrome) APK 가 앱 실행 중
                // 백그라운드 업데이트되거나 손상·비활성화되면, WebView(context) 생성 시 그 APK
                // 리소스를 앱 프로세스에 병합하는 과정에서
                // Resources$NotFoundException("failed to redirect ResourcesImpl")로 크래시한다.
                // (WebView.<init> -> ResourcesManager.redirectAllResourcesToNewImplLocked 경로)
                // 앱 코드로는 예방할 수 없는 시스템 측 문제라 여기서 잡고 재시도 UI 로 넘긴다.
                // 업데이트가 끝나면 재시도로 정상 생성되는 경우가 많다.
                androidHandle.markCreationFailed()
                FrameLayout(context)
            }
        },
    )
}

private fun createWebView(
    context: Context,
    handle: AndroidWebViewHandle,
    onUrlChanged: (String) -> Unit,
    onExternalSchemeFailed: (String) -> Unit,
): WebView = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )

    val createdWebView = this
    handle.attach(createdWebView)

    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        CookieManager.getInstance().setAcceptThirdPartyCookies(createdWebView, true)
        mediaPlaybackRequiresUserGesture = false
        cacheMode = WebSettings.LOAD_DEFAULT
    }

    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            if (request == null) return false

            return when (request.url.scheme) {
                "http", "https", "about", "data", "blob", "javascript" -> false

                "intent" -> {
                    handleIntentUrl(request.url, context, onExternalSchemeFailed)
                    true
                }

                else -> {
                    handleDefaultScheme(request.url, context, onExternalSchemeFailed)
                    true
                }
            }
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            url?.let(onUrlChanged)
            super.doUpdateVisitedHistory(view, url, isReload)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (handle.shouldClearHistoryAfterLoad) {
                view?.clearHistory()
                handle.shouldClearHistoryAfterLoad = false
            }
        }
    }

    // 로드는 호출자의 LaunchedEffect(url) 가 한다. 여기서 하면 로더가 둘이 된다.
}

private fun handleIntentUrl(uri: Uri, context: Context, onError: (String) -> Unit) {
    val url = uri.toString()
    runCatching {
        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure { exception ->
        if (exception !is ActivityNotFoundException) {
            onError("처리할 수 없는 intent입니다.\n$url")
            return@onFailure
        }

        val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            .getStringExtra("browser_fallback_url")
        val convertedWebUrl = url.replaceFirst("intent://", "https://").substringBefore("#Intent")

        when {
            !fallbackUrl.isNullOrBlank() -> context.startActivity(
                Intent(Intent.ACTION_VIEW, fallbackUrl.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )

            URLUtil.isNetworkUrl(convertedWebUrl) -> context.startActivity(
                Intent(Intent.ACTION_VIEW, convertedWebUrl.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )

            else -> onError("처리할 수 없는 intent입니다.\n$url")
        }
    }
}

private fun handleDefaultScheme(uri: Uri, context: Context, onError: (String) -> Unit) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }.onFailure {
        onError("처리할 수 없는 스킴입니다.\n$uri")
    }
}
