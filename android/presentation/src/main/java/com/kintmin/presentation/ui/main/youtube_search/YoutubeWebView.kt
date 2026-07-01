package com.kintmin.presentation.ui.main.youtube_search

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray80
import androidx.core.net.toUri

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWebView(
    modifier: Modifier = Modifier,
    currentUrl: String,
    setWebView: (WebView) -> Unit,
    webView: WebView?,
    sendIntent: (YoutubeDownloadIntent) -> Unit,
    onNavigateToPlaylist: () -> Unit,
) {
    var shouldClearHistoryAfterHomeLoad by remember { mutableStateOf(false) }
    var creationFailed by remember { mutableStateOf(false) }
    val homeUrl = "https://m.youtube.com/"

    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .background(gray80),
            contentAlignment = Alignment.Center
        ) {
            Text("WebView Preview")
        }
        return
    }

    // 시스템 WebView(Android System WebView / Chrome) APK가 앱 실행 중 백그라운드 업데이트되거나
    // 손상/비활성화되면, WebView(context) 생성 시 그 APK 리소스를 앱 프로세스에 병합하는 과정에서
    // Resources$NotFoundException("failed to redirect ResourcesImpl")가 발생해 앱이 크래시한다.
    // (WebView.<init> -> ResourcesManager.redirectAllResourcesToNewImplLocked 경로)
    // 앱 코드로는 예방할 수 없는 시스템 측 문제라, 아래 factory의 runCatching으로 예외를 잡아
    // creationFailed를 세우고 여기서 크래시 대신 재시도 UI를 노출한다.
    // 시스템 WebView 업데이트가 끝나면 재시도로 정상 생성되는 경우가 많다.
    if (creationFailed) {
        Box(
            modifier = modifier.background(gray80),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "웹뷰에 문제가 생겼습니다.\n다시 시도해주세요.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { creationFailed = false }) {
                    Text("다시 시도")
                }
            }
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            webView ?: runCatching {
                WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val newWebView = this
                setWebView(newWebView)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    CookieManager.getInstance().setAcceptThirdPartyCookies(newWebView, true)
                    mediaPlaybackRequiresUserGesture = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        if (request == null) return false

                        when(request.url.scheme) {
                            "http", "https", "about", "data", "blob", "javascript" -> {
                                return false
                            }
                            "intent" -> {
                                handleIntentUrl(request.url, context) { message ->
                                    sendIntent(YoutubeDownloadIntent.OnShowToast(message))
                                }
                                return true
                            }
                            else -> {
                                handleDefaultScheme(request.url, context) { message ->
                                    sendIntent(YoutubeDownloadIntent.OnShowToast(message))
                                }
                                return true
                            }
                        }
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean
                    ) {
                        Log.d("webview", "doUpdateVisitedHistory: $url")
                        url?.let { sendIntent(YoutubeDownloadIntent.OnChangeUrl(it)) }
                        super.doUpdateVisitedHistory(view, url, isReload)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (shouldClearHistoryAfterHomeLoad) {
                            view?.clearHistory()
                            shouldClearHistoryAfterHomeLoad = false
                        }
                    }
                }

                val headers = mutableMapOf<String, String>().apply {
                    put("Referer", "https://${context.packageName.lowercase()}")
                }
                loadUrl(currentUrl, headers)
                }
            }.getOrElse { throwable ->
                creationFailed = true
                FrameLayout(context)
            }
        },
        update = { view ->
            if (view is WebView && currentUrl.isNotBlank() && view.url != currentUrl) {
                view.loadUrl(currentUrl)
            }
        }
    )

    BackHandler {
        val backEnabled = webView?.canGoBack() ?: false
        if (backEnabled) {
            webView.goBack()
        } else if (webView?.url != homeUrl) {
            shouldClearHistoryAfterHomeLoad = true
            webView?.loadUrl(homeUrl)
        } else {
            onNavigateToPlaylist()
        }
    }
}

private fun handleIntentUrl(uri: Uri, context: Context, onError: (String) -> Unit) {
    val url = uri.toString()
    runCatching {
        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure { exception ->
        if (exception is ActivityNotFoundException) {
            val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
            val convertedWebUrl = url.replaceFirst("intent://", "https://").substringBefore("#Intent")

            when {
                !fallbackUrl.isNullOrBlank() -> {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, fallbackUrl.toUri()).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    )
                }
                URLUtil.isNetworkUrl(convertedWebUrl) -> {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, convertedWebUrl.toUri()).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    )
                }
                else -> {
                    onError("처리할 수 없는 intent입니다.\n${url}")
                }
            }
        } else {
            onError("처리할 수 없는 intent입니다.\n${url}")
        }
    }
}

private fun handleDefaultScheme(uri: Uri, context: Context, onError: (String) -> Unit) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }.onFailure {
        onError("처리할 수 없는 스킴입니다.\n${uri}")
    }
}

@Preview(showBackground = true)
@Composable
fun YoutubeWebViewPreview() {
    JellyTubeTheme {
        YoutubeWebView(
            modifier = Modifier.fillMaxSize(),
            currentUrl = "",
            setWebView = {},
            webView = null,
            sendIntent = {},
            onNavigateToPlaylist = {},
        )
    }
}
