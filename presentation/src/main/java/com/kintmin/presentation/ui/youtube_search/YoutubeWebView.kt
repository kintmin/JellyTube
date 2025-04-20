package com.kintmin.presentation.ui.youtube_search

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.rememberNavController

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWebView(
    modifier: Modifier = Modifier,
    url: String,
    onChangeUrl: (String) -> Unit,
) {
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

                loadUrl(url)
            }
        }
    )

    BackHandler {
        val backEnabled = webView?.canGoBack() ?: false
        if (backEnabled) {
            webView?.goBack()
        } else {
            val navController = rememberNavController()
            navController.popBackStack()
        }
    }
}
