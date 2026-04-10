package com.kintmin.presentation.ui.main.youtube_search

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
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
) {
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
                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean
                    ) {
                        Log.d("webview", "doUpdateVisitedHistory: $url")
                        url?.let { sendIntent(YoutubeDownloadIntent.OnChangeUrl(it)) }
                        super.doUpdateVisitedHistory(view, url, isReload)
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
            webView?.goBack()
        }
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
        )
    }
}
