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
        // Safed status/nav bar par dark icons (phone dark mode me bhi)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AColor.WHITE, AColor.WHITE),
        )
        browser = BrowserState(this)
        browser.fileChooser = { cb -> pendingFile?.onReceiveValue(null); pendingFile = cb; pickFile.launch("*/*") }
        pendingUrl = intent?.dataString
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
