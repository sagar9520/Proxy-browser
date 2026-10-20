package com.myfuturecode.proxygo

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

data class PageEntry(val title: String, val url: String, val time: Long)

/** Saari settings Compose state me, taaki toggle karte hi UI update ho. */
object Prefs {
    private lateinit var sp: SharedPreferences

    val blockImages = mutableStateOf(false)
    val javascript = mutableStateOf(true)
    val desktopMode = mutableStateOf(false)
    val adBlock = mutableStateOf(true)
    val incognito = mutableStateOf(false)
    val clearOnExit = mutableStateOf(false)
    val autoConnect = mutableStateOf(true)
    val darkMode = mutableStateOf(false)
    val searchEngine = mutableStateOf("Google")
    val lastProxy = mutableStateOf("")
    val history = mutableStateListOf<PageEntry>()
    val bookmarks = mutableStateListOf<PageEntry>()

    val engines = linkedMapOf(
        "Google" to "https://www.google.com/search?q=",
        "Bing" to "https://www.bing.com/search?q=",
        "DuckDuckGo" to "https://duckduckgo.com/?q=",
        "Yahoo" to "https://search.yahoo.com/search?p=",
    )

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("prefs", 0)
        blockImages.value = sp.getBoolean("block_images", false)
        javascript.value = sp.getBoolean("javascript", true)
        desktopMode.value = sp.getBoolean("desktop", false)
        adBlock.value = sp.getBoolean("adblock", true)
        clearOnExit.value = sp.getBoolean("clear_on_exit", false)
        autoConnect.value = sp.getBoolean("auto_connect", true)
        darkMode.value = sp.getBoolean("dark", false)
        searchEngine.value = sp.getString("engine", "Google")!!
        lastProxy.value = sp.getString("last_proxy", "")!!
        history.addAll(load("history"))
        bookmarks.addAll(load("bookmarks"))
    }

    fun set(key: String, state: androidx.compose.runtime.MutableState<Boolean>, v: Boolean) {
        state.value = v
        sp.edit().putBoolean(key, v).apply()
    }

    fun setEngine(name: String) { searchEngine.value = name; sp.edit().putString("engine", name).apply() }
    fun setLastProxy(id: String) { lastProxy.value = id; sp.edit().putString("last_proxy", id).apply() }

    fun searchUrl(q: String): String =
        (engines[searchEngine.value] ?: RemoteConfig.current.searchUrl) + android.net.Uri.encode(q)

    fun addHistory(title: String, url: String) {
        if (incognito.value || url.isBlank() || url.startsWith("about:")) return
        history.removeAll { it.url == url }
        history.add(0, PageEntry(title.ifBlank { url }, url, System.currentTimeMillis()))
        while (history.size > 300) history.removeAt(history.lastIndex)
        save("history", history)
    }

    fun clearHistory() { history.clear(); save("history", history) }

    fun toggleBookmark(title: String, url: String) {
        if (bookmarks.any { it.url == url }) bookmarks.removeAll { it.url == url }
        else bookmarks.add(0, PageEntry(title.ifBlank { url }, url, System.currentTimeMillis()))
        save("bookmarks", bookmarks)
    }

    fun removeBookmark(e: PageEntry) { bookmarks.remove(e); save("bookmarks", bookmarks) }

    private fun load(key: String): List<PageEntry> = runCatching {
        val a = JSONArray(sp.getString(key, "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> PageEntry(o.optString("t"), o.optString("u"), o.optLong("d")) } }
    }.getOrDefault(emptyList())

    private fun save(key: String, list: List<PageEntry>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("t", it.title).put("u", it.url).put("d", it.time)) }
        sp.edit().putString(key, a.toString()).apply()
    }
}
