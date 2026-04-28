package com.kintmin.presentation.ui.main.youtube_search

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray80

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

    AndroidView(
        modifier = modifier,
        factory = { context ->
            webView ?: WebView(context).apply {
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
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        url: String?
                    ): Boolean {
                        if (url.isNullOrBlank()) return false
                        return handleIntentUrl(url, context)
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
        },
        update = { view ->
            if (currentUrl.isNotBlank() && view.url != currentUrl) {
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

private fun handleIntentUrl(url: String, context: android.content.Context): Boolean {
    if (!url.startsWith("intent://")) return false

    return try {
        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            .getStringExtra("browser_fallback_url")
        val convertedWebUrl = url.replaceFirst("intent://", "https://").substringBefore("#Intent")

        when {
            !fallbackUrl.isNullOrBlank() -> {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fallbackUrl)).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                )
                true
            }
            URLUtil.isNetworkUrl(convertedWebUrl) -> {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(convertedWebUrl)).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                )
                true
            }
            else -> true
        }
    } catch (_: Exception) {
        true
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
