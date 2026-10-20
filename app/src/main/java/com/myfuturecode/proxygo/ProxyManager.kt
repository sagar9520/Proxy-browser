package com.myfuturecode.proxygo

import android.webkit.HttpAuthHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * WebView ka saara traffic chune hue proxy server se bhejta hai (androidx.webkit ProxyController).
 * Ye VPN nahi hai: sirf is browser ka traffic proxy se jaata hai, isliye VpnService permission nahi chahiye.
 *
 * Server list = config ke "proxies" (aapke apne servers) + "free_proxy_api" se aaye public proxies,
 * jinko pehle check karke sirf chalne wale dikhaye jaate hain.
 */
object ProxyManager {
    val servers = mutableStateListOf<ProxyServer>()
    val loading = mutableStateOf(false)
    val connected = mutableStateOf<ProxyServer?>(null)
    val connectedIp = mutableStateOf("")
    val connecting = mutableStateOf<String?>(null)
    val pings = mutableStateMapOf<String, Int>()   // id -> ms (-1 = kaam nahi kar raha)

    private val direct = Executor { it.run() }
    private val http = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()

    val supported get() = WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)

    /** Config + free list se servers laao aur check karo. */
    suspend fun loadServers() {
        if (loading.value) return
        loading.value = true
        try {
            val cfg = RemoteConfig.current
            val own = cfg.proxies
            servers.clear(); servers.addAll(own)
            val free = runCatching { fetchFree(cfg.freeProxyApi) }.getOrDefault(emptyList())
            val checked = verifyMany(free.take(cfg.maxFreeProxies * 3), cfg.maxFreeProxies)
            servers.addAll(checked.sortedBy { pings[it.id] ?: 9999 })
            verifyMany(own, own.size)
        } finally {
            loading.value = false
        }
    }

    private suspend fun fetchFree(url: String): List<ProxyServer> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        http.newCall(Request.Builder().url(url).build()).execute().use { res ->
            val arr = JSONObject(res.body!!.string()).optJSONArray("data") ?: return@use emptyList()
            val counter = mutableMapOf<String, Int>()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val cc = o.optString("country")
                if (cc.isBlank()) return@mapNotNull null
                val protos = o.optJSONArray("protocols")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: listOf("http")
                if ("http" !in protos && "https" !in protos) return@mapNotNull null
                val port = o.optString("port").toIntOrNull() ?: return@mapNotNull null
                val n = (counter[cc] ?: 0) + 1
                counter[cc] = n
                val up = o.optDouble("upTime", 50.0)
                ProxyServer(
                    name = "${countryName(cc)} $n", country = cc, type = "http",
                    host = o.optString("ip"), port = port, user = "", pass = "",
                    quality = if (up >= 90) "high" else if (up >= 60) "medium" else "low",
                    latency = o.optDouble("latency", 0.0).toInt(), fromApi = true,
                )
            }
        }
    }

    /** Parallel check; `want` chalne wale mil gaye to baaki chhod do. */
    private suspend fun verifyMany(list: List<ProxyServer>, want: Int): List<ProxyServer> = coroutineScope {
        val gate = Semaphore(12)
        val ok = mutableListOf<ProxyServer>()
        list.map { s ->
            async {
                gate.withPermit {
                    if (synchronized(ok) { ok.size } >= want) return@withPermit
                    if (verify(s) != null) synchronized(ok) { ok.add(s) }
                }
            }
        }.awaitAll()
        ok
    }

    /** Proxy ke through ek chhota HTTPS request. Chala to bahar dikhne wala IP, warna null. */
    suspend fun verify(s: ProxyServer): String? = withContext(Dispatchers.IO) {
        val t = System.nanoTime()
        val builder = http.newBuilder()
            .proxy(Proxy(if (s.type.startsWith("socks")) Proxy.Type.SOCKS else Proxy.Type.HTTP, InetSocketAddress(s.host, s.port)))
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(6, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS)
        if (s.user.isNotBlank()) builder.proxyAuthenticator(Authenticator { _, r ->
            r.request.newBuilder().header("Proxy-Authorization", Credentials.basic(s.user, s.pass)).build()
        })
        val ip = runCatching {
            builder.build().newCall(Request.Builder().url("https://api.ipify.org?format=json").build()).execute().use { r ->
                if (r.isSuccessful) JSONObject(r.body!!.string()).optString("ip").ifBlank { null } else null
            }
        }.getOrNull()
        val ms = ((System.nanoTime() - t) / 1_000_000).toInt()
        withContext(Dispatchers.Main) { pings[s.id] = if (ip != null) ms else -1 }
        ip
    }

    /** Check karke WebView pe proxy lagao. */
    suspend fun connect(server: ProxyServer): Boolean {
        if (!supported) return false
        connecting.value = server.id
        try {
            val ip = verify(server) ?: return false
            val scheme = when (server.type.lowercase()) {
                "socks", "socks5" -> "socks5"
                "socks4" -> "socks4"
                "https" -> "https"
                else -> "http"
            }
            val cfg = ProxyConfig.Builder().addProxyRule("$scheme://${server.host}:${server.port}").build()
            suspendCoroutine { c -> ProxyController.getInstance().setProxyOverride(cfg, direct) { c.resume(Unit) } }
            connected.value = server
            connectedIp.value = ip
            Prefs.setLastProxy(server.id)
            return true
        } finally {
            connecting.value = null
        }
    }

    fun disconnect(done: () -> Unit = {}) {
        if (!supported) { connected.value = null; done(); return }
        ProxyController.getInstance().clearProxyOverride(direct) {
            connected.value = null
            connectedIp.value = ""
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

    fun fastest(): ProxyServer? =
        servers.filter { (pings[it.id] ?: 0) > 0 }.minByOrNull { pings[it.id]!! }
}
