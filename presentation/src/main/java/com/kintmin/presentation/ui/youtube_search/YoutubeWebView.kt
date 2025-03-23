package com.kintmin.presentation.ui.youtube_search

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWebView(
    modifier: Modifier = Modifier,
    url: String,
    onChangeUrl: (String) -> Unit,
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var backEnabled by remember { mutableStateOf(false) }

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
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    CookieManager.getInstance().removeAllCookies(null)
                }

                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean
                    ) {
                        backEnabled = view?.canGoBack() ?: false
                        webView?.clearCache(true)
                        WebStorage.getInstance().deleteAllData()
                        url?.let {
                            onChangeUrl(it)
                        }
                        super.doUpdateVisitedHistory(view, url, isReload)
                    }
                }

                loadUrl(url)
                webView = this
            }
        },
        update = {
            webView?.clearCache(true)
            WebStorage.getInstance().deleteAllData()
        }
    )

    BackHandler(enabled = backEnabled) {
        webView?.goBack()
    }
}
