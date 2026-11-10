package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGRect
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
import platform.WebKit.WKAudiovisualMediaTypeNone
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/** 웹뷰가 그대로 열어야 하는 스킴. 나머지는 바깥 앱으로 넘긴다. */
private val PASS_THROUGH_SCHEMES = setOf("http", "https", "about", "data", "blob", "javascript")

/** SPA 의 주소 변경을 네이티브로 올리는 창구 이름. */
private const val URL_CHANGE_HANDLER_NAME = "jellyTubeUrlChanged"

/**
 * `WKNavigationDelegate` 는 same-document 이동(`pushState`/`replaceState`/`popstate`/
 * `hashchange`)을 알려주지 않는다. m.youtube.com 은 SPA 라 영상 사이 이동이 전부 여기로
 * 떨어지는데, 이것이 없으면 **다운로드 대상 주소가 첫 화면에서 멈춘다.**
 * Android `WebViewClient.doUpdateVisitedHistory` 와 짝이다.
 */
private val URL_CHANGE_SCRIPT = """
(function() {
    var last = location.href;
    var post = function() {
        var current = location.href;
        if (current === last) return;
        last = current;
        try { window.webkit.messageHandlers.$URL_CHANGE_HANDLER_NAME.postMessage(current); } catch (e) {}
    };
    var wrap = function(name) {
        var original = history[name];
        if (!original) return;
        history[name] = function() {
            var result = original.apply(this, arguments);
            post();
            return result;
        };
    };
    wrap('pushState');
    wrap('replaceState');
    window.addEventListener('popstate', post);
    window.addEventListener('hashchange', post);
})();
""".trimIndent()

@OptIn(ExperimentalForeignApi::class)
private class IosWebViewHandle : WebViewHandle {

    var webView: WKWebView? by mutableStateOf(null)
        private set

    /**
     * iOS 에서 이 상태가 서는 경우는 **렌더러(WebContent) 프로세스가 죽었을 때**다.
     * Android 의 "시스템 WebView APK 생성 실패"와 원인은 다르지만, 사용자에게는 똑같이
     * "웹뷰를 쓸 수 없다" 이고 재시도로 풀린다. 비워 두면 흰 화면으로 남는다.
     */
    override var isCreationFailed: Boolean by mutableStateOf(false)
        private set

    /** 렌더러가 죽은 뒤 돌아갈 자리. `reload()` 는 그때 열려 있던 주소를 보장하지 않는다. */
    private var resumeUrl: String? = null

    override val canGoBack: Boolean get() = webView?.canGoBack == true

    override val currentUrl: String? get() = webView?.URL?.absoluteString

    override fun goBack() {
        webView?.goBack()
    }

    override fun loadUrl(url: String, clearHistoryAfterLoad: Boolean) {
        val target = webView ?: return

        // `WKWebView.backForwardList` 는 읽기 전용이라 Android 처럼 히스토리를 비울 수 없다.
        // 대신 새 항목을 **쌓지 않는** location.replace 로 간다. 양쪽이 같은 답을 내려면
        // 비우는 쪽이 아니라 덮어쓰는 쪽이어야 한다.
        if (clearHistoryAfterLoad && target.URL != null) {
            target.evaluateJavaScript("location.replace('${url.escapeForJsString()}')", null)
            return
        }

        val nsUrl = NSURL.URLWithString(url) ?: return
        // Android 는 Referer 에 패키지명을 싣는다. **여기서는 싣지 않는다** — 그 헤더가 무엇을
        // 위한 것인지 Android 쪽에 기록이 없어, 근거 없이 따라 하면 두 플랫폼이 다른 이유로
        // 같은 값을 보내게 된다. 유튜브가 다르게 굴면 그때 근거와 함께 더한다.
        target.loadRequest(NSURLRequest.requestWithURL(nsUrl))
    }

    override fun retryCreation() {
        isCreationFailed = false
        val target = webView ?: return
        val resume = resumeUrl
        resumeUrl = null
        if (resume != null) loadUrl(resume) else target.reload()
    }

    fun attach(webView: WKWebView) {
        this.webView = webView
    }

    fun markRendererGone(lastUrl: String?) {
        resumeUrl = lastUrl
        isCreationFailed = true
    }

    fun release() {
        webView?.apply {
            stopLoading()
            navigationDelegate = null
            configuration.userContentController
                .removeScriptMessageHandlerForName(URL_CHANGE_HANDLER_NAME)
        }
        webView = null
    }
}

@Composable
actual fun rememberWebViewHandle(): WebViewHandle {
    val handle = remember { IosWebViewHandle() }
    DisposableEffect(handle) {
        onDispose { handle.release() }
    }
    return handle
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(
    handle: WebViewHandle,
    url: String,
    onUrlChanged: (String) -> Unit,
    onExternalSchemeFailed: (message: String) -> Unit,
    modifier: Modifier,
) {
    val iosHandle = handle as IosWebViewHandle
    val currentOnUrlChanged by rememberUpdatedState(onUrlChanged)
    val currentOnExternalSchemeFailed by rememberUpdatedState(onExternalSchemeFailed)

    val urlChangeHandler = remember {
        UrlChangeMessageHandler { changedUrl -> currentOnUrlChanged(changedUrl) }
    }

    val navigationDelegate = remember {
        WebViewNavigationDelegate(
            onUrlChanged = { changedUrl -> currentOnUrlChanged(changedUrl) },
            onExternalSchemeFailed = { message -> currentOnExternalSchemeFailed(message) },
            onRendererGone = { lastUrl -> iosHandle.markRendererGone(lastUrl) },
        )
    }

    val webView = remember {
        WKWebView(
            frame = cValue<CGRect>(),
            configuration = createConfiguration(urlChangeHandler),
        ).apply {
            this.navigationDelegate = navigationDelegate
            allowsBackForwardNavigationGestures = true
            // 화면 전체를 웹이 쓰므로 자동 인셋 보정은 첫 프레임을 밀기만 한다.
            scrollView.contentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentNever
            iosHandle.attach(this)
        }
    }

    // 로드는 url 이 "바뀔 때 한 번"만 한다. 웹뷰도 스스로 이동하는 주체라, 매 리컴포지션마다
    // 단언하면 낡은 주소를 되로드해 뒤로가기가 되돌아간다(Android 쪽에 같은 기록이 있다).
    LaunchedEffect(webView, url) {
        if (url.isNotBlank() && webView.URL?.absoluteString != url) {
            iosHandle.loadUrl(url)
        }
    }

    UIKitView(
        factory = { webView },
        modifier = modifier,
        // **기본값(NonCooperative)이면 웹뷰 위에 겹친 Compose 가 터치를 못 받는다.**
        // interop 뷰는 네이티브 계층에서 Compose 캔버스 위에 놓이므로, 그려지는 순서와
        // 무관하게 네이티브가 먼저 집는다 — 다운로드 FAB 이 눌리지 않는다.
        // Cooperative 는 Compose 에 먼저 기회를 주고, 아무도 집지 않으면 웹뷰로 넘긴다.
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.Cooperative(),
        ),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun createConfiguration(
    urlChangeHandler: WKScriptMessageHandlerProtocol,
): WKWebViewConfiguration = WKWebViewConfiguration().apply {
    allowsInlineMediaPlayback = true
    // Android 의 mediaPlaybackRequiresUserGesture = false 와 짝이다.
    mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeNone
    defaultWebpagePreferences.allowsContentJavaScript = true

    userContentController.apply {
        addScriptMessageHandler(urlChangeHandler, URL_CHANGE_HANDLER_NAME)
        addUserScript(
            WKUserScript(
                source = URL_CHANGE_SCRIPT,
                injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                // iframe 까지 후킹하면 광고 프레임의 주소가 현재 주소로 올라온다.
                forMainFrameOnly = true,
            ),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class WebViewNavigationDelegate(
    private val onUrlChanged: (String) -> Unit,
    private val onExternalSchemeFailed: (String) -> Unit,
    private val onRendererGone: (String?) -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val requestUrl = decidePolicyForNavigationAction.request.URL
        val scheme = requestUrl?.scheme?.lowercase()

        if (requestUrl == null || scheme == null || scheme in PASS_THROUGH_SCHEMES) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            return
        }

        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)

        val application = UIApplication.sharedApplication
        if (application.canOpenURL(requestUrl)) {
            application.openURL(requestUrl, emptyMap<Any?, Any?>(), null)
        } else {
            onExternalSchemeFailed("처리할 수 없는 스킴입니다.\n${requestUrl.absoluteString}")
        }
    }

    // 두 콜백은 ObjC 셀렉터가 다른데 Kotlin 시그니처가 같아진다. 충돌 허용을 명시해야 한다.
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
        webView.URL?.absoluteString?.let(onUrlChanged)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        webView.URL?.absoluteString?.let(onUrlChanged)
    }

    /**
     * 렌더러가 죽었다. **비워 두면 화면이 빈 흰색으로 남고** 사용자에게는 앱이 멈춘 것과
     * 구분되지 않는다. `WKWebView` 는 스스로 되살아나지 않는다.
     */
    override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
        onRendererGone(webView.URL?.absoluteString)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class UrlChangeMessageHandler(
    private val onUrlChanged: (String) -> Unit,
) : NSObject(), WKScriptMessageHandlerProtocol {

    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        (didReceiveScriptMessage.body as? String)?.let(onUrlChanged)
    }
}

/** `location.replace('...')` 에 넣기 전에 따옴표와 역슬래시를 막는다. */
private fun String.escapeForJsString(): String =
    replace("\\", "\\\\").replace("'", "\\'")
