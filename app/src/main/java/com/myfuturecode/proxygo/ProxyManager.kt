package com.myfuturecode.proxygo

import android.webkit.HttpAuthHandler
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executor

/**
 * WebView ka saara traffic chune hue proxy server se bhejta hai (androidx.webkit ProxyController).
 * Ye VPN nahi hai: sirf is browser ka traffic proxy se jaata hai, isliye VpnService permission nahi chahiye.
 */
object ProxyManager {
    val connected = mutableStateOf<ProxyServer?>(null)
    val connecting = mutableStateOf<String?>(null)
    val pings = mutableStateMapOf<String, Int>()   // id -> ms (-1 = fail)

    private val direct = Executor { it.run() }

    val supported get() = WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)

    fun connect(server: ProxyServer, done: (Boolean) -> Unit) {
        if (!supported) { done(false); return }
        connecting.value = server.id
        val scheme = when (server.type.lowercase()) {
            "socks", "socks5" -> "socks5"
            "socks4" -> "socks4"
            "https" -> "https"
            else -> "http"
        }
        val cfg = ProxyConfig.Builder()
            .addProxyRule("$scheme://${server.host}:${server.port}")
            .build()
        ProxyController.getInstance().setProxyOverride(cfg, direct) {
            connecting.value = null
            connected.value = server
            Prefs.setLastProxy(server.id)
            done(true)
        }
    }

    fun disconnect(done: () -> Unit = {}) {
        if (!supported) { connected.value = null; done(); return }
        ProxyController.getInstance().clearProxyOverride(direct) {
            connected.value = null
            done()
        }
    }

    /** Proxy wale server ko user/pass chahiye ho to WebView yahan poochta hai. */
    fun handleAuth(handler: HttpAuthHandler, host: String?): Boolean {
        val s = connected.value ?: return false
        if (s.user.isBlank() || host != s.host) return false
        handler.proceed(s.user, s.pass)
        return true
    }

    suspend fun pingAll(list: List<ProxyServer>) = coroutineScope {
        list.map { s -> async { pings[s.id] = ping(s) } }.awaitAll()
    }

    private suspend fun ping(s: ProxyServer): Int = withContext(Dispatchers.IO) {
        runCatching {
            val t = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(s.host, s.port), 4000) }
            ((System.nanoTime() - t) / 1_000_000).toInt()
        }.getOrDefault(-1)
    }

    fun fastest(list: List<ProxyServer>): ProxyServer? =
        list.filter { !it.premium && (pings[it.id] ?: 0) >= 0 }.minByOrNull { pings[it.id]?.takeIf { p -> p > 0 } ?: 9999 }
}
