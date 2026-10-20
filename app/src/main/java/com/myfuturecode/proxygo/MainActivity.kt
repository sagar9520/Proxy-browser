package com.myfuturecode.proxygo

import android.content.Intent
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebStorage
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

enum class Screen { Splash, Browser, Proxies, Settings, History, Bookmarks }

class MainActivity : ComponentActivity() {
    var screen by mutableStateOf(Screen.Splash)
    var connectedPopup by mutableStateOf<ProxyServer?>(null)
    lateinit var browser: BrowserState
    private var pendingUrl: String? = null
    private var pendingFile: ValueCallback<Array<Uri>>? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        pendingFile?.onReceiveValue(uris.toTypedArray()); pendingFile = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("ci_no_ads", false) == true) Ads.ciNoAds = true
        // Safed status/nav bar par dark icons (phone dark mode me bhi)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AColor.WHITE, AColor.WHITE),
        )
        browser = BrowserState(this)
        browser.fileChooser = { cb -> pendingFile?.onReceiveValue(null); pendingFile = cb; pickFile.launch("*/*") }
        pendingUrl = intent?.dataString
        browser.onProxyError = { tab -> switchServer(tab) }
        setContent {
            ProxyGoTheme {
                when (screen) {
                    Screen.Splash -> SplashScreen(this) {
                        (application as App).splashDone = true
                        screen = Screen.Browser
                        pendingUrl?.let { browser.open(it) }; pendingUrl = null
                    }
                    Screen.Browser -> BrowserScreen(this)
                    Screen.Proxies -> ProxyListScreen(this)
                    Screen.Settings -> SettingsScreen(this)
                    Screen.History -> PagesScreen(this, bookmarks = false)
                    Screen.Bookmarks -> PagesScreen(this, bookmarks = true)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val url = intent.dataString ?: return
        if (screen == Screen.Splash) pendingUrl = url else { screen = Screen.Browser; browser.open(url) }
    }

    private var switching = false
    private val switches = ArrayDeque<Long>()

    /** Chalu server ne jawab dena band kar diya: use list se hatao, agla tez server lagao, page reload. */
    private fun switchServer(tab: Tab) {
        val dead = ProxyManager.connected.value ?: return
        val now = System.currentTimeMillis()
        while (switches.isNotEmpty() && now - switches.first() > 120_000) switches.removeFirst()
        if (switching || switches.size >= 3) return
        switching = true; switches.addLast(now)
        lifecycleScope.launch {
            ProxyManager.pings[dead.id] = -1
            ProxyManager.servers.removeAll { it.id == dead.id }
            toast(this@MainActivity, "${dead.name} is slow, switching server…")
            val next = ProxyManager.connectBest(null, tries = 3)
            switching = false
            if (next != null) { toast(this@MainActivity, "Connected to ${next.name}"); tab.webView.reload() }
            else ProxyManager.disconnect { toast(this@MainActivity, "No server reachable, browsing without proxy"); tab.webView.reload() }
        }
    }

    fun go(s: Screen) {
        screen = s
    }

    override fun onDestroy() {
        if (isFinishing && Prefs.clearOnExit.value) clearBrowsingData()
        browser.destroy()
        super.onDestroy()
    }

    fun clearBrowsingData() {
        Prefs.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
        browser.clearCache()
    }
}
