package com.myfuturecode.proxygo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ProxyServer(
    val name: String,
    val country: String,
    val type: String,
    val host: String,
    val port: Int,
    val user: String,
    val pass: String,
    val quality: String,
    val premium: Boolean,
) {
    val id get() = "$host:$port"
    val flag: String
        get() = if (country.length == 2) country.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')) }
            .joinToString("") { String(it) } else "🌐"
}

data class QuickSite(val name: String, val url: String, val letter: String, val color: String, val icon: String)

data class AdUnits(val appOpen: String, val banner: String, val interstitial: String, val rewarded: String)

data class AdsConfig(
    val enabled: Boolean,
    val network: String,
    val interstitialEvery: Int,
    val appOpen: Boolean,
    val admob: AdUnits,
    val max: AdUnits,
) {
    val units get() = if (network == "max") max else admob
}

data class PromoApp(val name: String, val pkg: String, val icon: String, val desc: String)

data class AppConfig(
    val searchUrl: String,
    val autoConnect: Boolean,
    val proxies: List<ProxyServer>,
    val quickSites: List<QuickSite>,
    val adblockHosts: List<String>,
    val ads: AdsConfig,
    val crossPromo: List<PromoApp>,
    val privacyUrl: String,
)

/**
 * Config pehle cache/asset se turant load hota hai, phir server (get_json.php?file=<package>) se refresh.
 * Server ka jawab {"meta":{...},"data":{...}} hota hai; "data" ke andar default_config.json jaisa hi format.
 */
object RemoteConfig {
    private const val PREF = "remote_config"
    private val _config = MutableStateFlow<AppConfig?>(null)
    val config: StateFlow<AppConfig?> = _config
    val current: AppConfig get() = _config.value!!

    fun loadLocal(ctx: Context) {
        val cached = ctx.getSharedPreferences(PREF, 0).getString("json", null)
        val asset = ctx.assets.open("default_config.json").bufferedReader().use { it.readText() }
        _config.value = runCatching { parse(JSONObject(cached!!)) }.getOrNull() ?: parse(JSONObject(asset))
    }

    suspend fun refresh(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        if (BuildConfig.CONFIG_URL.isBlank()) return@withContext false
        runCatching {
            val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
            val req = Request.Builder()
                .url("${BuildConfig.CONFIG_URL}?file=${ctx.packageName}")
                .header("X-App-Key", BuildConfig.CONFIG_KEY)
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use false
                val root = JSONObject(res.body!!.string())
                val data = root.optJSONObject("data") ?: return@use false
                if (data.length() == 0) return@use false
                if (!data.has("cross_promo") && root.has("cross_promo")) data.put("cross_promo", root.get("cross_promo"))
                val parsed = parse(data)
                ctx.getSharedPreferences(PREF, 0).edit().putString("json", data.toString()).apply()
                _config.value = parsed
                true
            }
        }.getOrDefault(false)
    }

    private fun units(o: JSONObject?) = AdUnits(
        o?.optString("app_open").orEmpty(), o?.optString("banner").orEmpty(),
        o?.optString("interstitial").orEmpty(), o?.optString("rewarded").orEmpty(),
    )

    private inline fun <T> JSONArray?.mapObj(f: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.let(f) }

    fun parse(o: JSONObject): AppConfig {
        val ads = o.optJSONObject("ads")
        return AppConfig(
            searchUrl = o.optString("search_url", "https://www.google.com/search?q="),
            autoConnect = o.optBoolean("auto_connect", true),
            proxies = o.optJSONArray("proxies").mapObj {
                ProxyServer(
                    it.optString("name"), it.optString("country"), it.optString("type", "http"),
                    it.optString("host"), it.optInt("port", 3128), it.optString("user"), it.optString("pass"),
                    it.optString("quality", "medium"), it.optBoolean("premium", false),
                )
            }.filter { it.host.isNotBlank() },
            quickSites = o.optJSONArray("quick_sites").mapObj {
                QuickSite(it.optString("name"), it.optString("url"), it.optString("letter", it.optString("name").take(1)),
                    it.optString("color", "#4A1FC4"), it.optString("icon"))
            },
            adblockHosts = o.optJSONArray("adblock_hosts")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            ads = AdsConfig(
                enabled = ads?.optBoolean("enabled", true) ?: false,
                network = ads?.optString("network", "admob") ?: "admob",
                interstitialEvery = ads?.optInt("interstitial_every", 2) ?: 2,
                appOpen = ads?.optBoolean("app_open", true) ?: false,
                admob = units(ads?.optJSONObject("admob")),
                max = units(ads?.optJSONObject("max")),
            ),
            crossPromo = o.optJSONArray("cross_promo").mapObj {
                PromoApp(it.optString("name", it.optString("title")), it.optString("package", it.optString("pkg")),
                    it.optString("icon"), it.optString("desc", it.optString("description")))
            },
            privacyUrl = o.optString("privacy_url"),
        )
    }
}
