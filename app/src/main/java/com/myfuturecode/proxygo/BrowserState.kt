package com.myfuturecode.proxygo

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.ByteArrayInputStream

private const val DESKTOP_UA =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

class Tab(val webView: WebView) {
    var url by mutableStateOf("")       // "" = home page (quick sites)
    var title by mutableStateOf("")
    var progress by mutableIntStateOf(100)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    val isHome get() = url.isEmpty()
}

/** Saare tabs aur unke WebView. Activity ke saath zinda rehte hain (configChanges manifest me handle). */
class BrowserState(private val ctx: Context) {
    val tabs = mutableStateListOf<Tab>()
    /** Website ki video full-screen ho to yahan aati hai */
    var customView by mutableStateOf<android.view.View?>(null)
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    /** File upload (input type=file) ke liye Activity launcher */
    var fileChooser: ((android.webkit.ValueCallback<Array<Uri>>) -> Unit)? = null

    fun exitFullscreen() { customCallback?.onCustomViewHidden(); customView = null; customCallback = null }
    var currentIndex by mutableIntStateOf(0)
    val current: Tab get() = tabs[currentIndex]

    init { newTab() }

    fun newTab(url: String? = null) {
        tabs.add(createTab())
        currentIndex = tabs.lastIndex
        url?.let { open(it) }
    }

    fun closeTab(i: Int) {
        val t = tabs.removeAt(i)
        t.webView.destroy()
        if (tabs.isEmpty()) newTab()
        currentIndex = currentIndex.coerceAtMost(tabs.lastIndex)
    }

    /** Address bar ka text: URL hai to kholo, warna search. */
    fun open(input: String) {
        val text = input.trim()
        if (text.isEmpty()) return
        val url = when {
            URLUtil.isNetworkUrl(text) -> text
            !text.contains(' ') && text.contains('.') -> "https://$text"
            else -> Prefs.searchUrl(text)
        }
        current.url = url
        current.webView.loadUrl(url)
    }

    fun goHome() {
        current.webView.stopLoading()
        current.url = ""
        current.title = ""
    }

    fun back(): Boolean {
        val t = current
        if (t.isHome) return false
        if (t.webView.canGoBack()) t.webView.goBack() else goHome()
        return true
    }

    fun applySettings() = tabs.forEach { configure(it.webView.settings) }

    fun reloadAll() = tabs.forEach { if (!it.isHome) it.webView.reload() }

    fun clearCache() = tabs.forEach { it.webView.clearCache(true); it.webView.clearHistory() }

    fun destroy() = tabs.forEach { it.webView.destroy() }

    private fun configure(s: WebSettings) {
        s.javaScriptEnabled = Prefs.javascript.value
        s.domStorageEnabled = true
        s.loadsImagesAutomatically = !Prefs.blockImages.value
        s.blockNetworkImage = Prefs.blockImages.value
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.useWideViewPort = Prefs.wideViewport.value || Prefs.desktopMode.value
        s.loadWithOverviewMode = Prefs.overview.value || Prefs.desktopMode.value
        s.textZoom = Prefs.textZoom.intValue
        CookieManager.getInstance().setAcceptCookie(Prefs.acceptCookies.value)
        s.userAgentString = if (Prefs.desktopMode.value) DESKTOP_UA else null
        s.mediaPlaybackRequiresUserGesture = true
        s.setSupportMultipleWindows(false)
    }

    private fun isAd(host: String?): Boolean {
        if (!Prefs.adBlock.value || host == null) return false
        return RemoteConfig.current.adblockHosts.any { host == it || host.endsWith(".$it") }
    }

    /** Proxy wala page fail hua (timeout wagairah) to MainActivity agla server try karta hai. */
    var onProxyError: ((Tab) -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    private fun createTab(): Tab {
        val wv = WebView(ctx)
        configure(wv.settings)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        lateinit var tab: Tab
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                if (isAd(request.url.host)) WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))) else null

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme ?: return false
                if (scheme == "http" || scheme == "https") return false
                // intent:, market:, tel: wagairah bahar wale app me
                runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, request.url).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                // Free proxy IP pe Google captcha deta hai: wahi search Bing pe khol do
                val u = Uri.parse(url)
                if (u.host.orEmpty().contains("google.") && u.path.orEmpty().startsWith("/sorry")) {
                    val q = u.getQueryParameter("continue")?.let { Uri.parse(it).getQueryParameter("q") }
                    if (!q.isNullOrBlank()) {
                        view.stopLoading()
                        view.loadUrl(Prefs.engines.getValue("Bing") + Uri.encode(q))
                        return
                    }
                }
                tab.url = url
                tab.progress = 5
            }

            override fun onPageFinished(view: WebView, url: String) {
                tab.progress = 100
                tab.canGoBack = view.canGoBack()
                tab.canGoForward = view.canGoForward()
                Prefs.addHistory(view.title.orEmpty(), url)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                // Proxy laga hai aur poora page hi nahi khula: server shayad band ho gaya
                if (!request.isForMainFrame || ProxyManager.connected.value == null) return
                val bad = listOf(ERROR_TIMEOUT, ERROR_CONNECT, ERROR_PROXY_AUTHENTICATION, ERROR_HOST_LOOKUP, ERROR_IO)
                if (error.errorCode in bad) onProxyError?.invoke(tab)
            }

            override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String?, realm: String?) {
                if (!ProxyManager.handleAuth(handler, host)) super.onReceivedHttpAuthRequest(view, handler, host, realm)
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { tab.progress = newProgress }
            override fun onReceivedTitle(view: WebView, title: String?) { tab.title = title.orEmpty() }
            override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) { customView = view; customCallback = callback }
            override fun onHideCustomView() { customView = null; customCallback = null }
            override fun onShowFileChooser(w: WebView, cb: android.webkit.ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                val f = fileChooser ?: return false
                f(cb); return true
            }
        }
        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            runCatching {
                val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val req = DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                    .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ProxyGo/$name")
                (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                Toast.makeText(ctx, "Downloading $name", Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(ctx, "Download failed", Toast.LENGTH_SHORT).show() }
        }
        tab = Tab(wv)
        return tab
    }
}
