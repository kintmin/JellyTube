package com.kintmin.presentation.ui.youtube_search

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.kintmin.presentation.theme.YTMusicBoxTheme

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWebView(
    modifier: Modifier = Modifier,
    currentUrl: String,
    onChangeUrl: (String) -> Unit,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("WebView Preview")
        }
        return
    }

    var webView: WebView? by remember { mutableStateOf(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                webView = this

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
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
                        url?.let { onChangeUrl(it) }
                        super.doUpdateVisitedHistory(view, url, isReload)
                    }
                }

                loadUrl(currentUrl)
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
    YTMusicBoxTheme {
        YoutubeWebView(
            modifier = Modifier.fillMaxSize(),
            currentUrl = "",
            onChangeUrl = {},
        )
    }
}