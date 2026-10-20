package com.myfuturecode.proxygo

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.DateFormat
import java.util.Date
import kotlin.random.Random

// ---------------- Splash ----------------

@Composable
fun SplashScreen(act: MainActivity, done: () -> Unit) {
    val anim = rememberInfiniteTransition(label = "bar")
    val p by anim.animateFloat(0.1f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val stars = remember { List(40) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    LaunchedEffect(Unit) {
        withTimeoutOrNull(6000) { RemoteConfig.refresh(act) }
        Ads.preload(act)
        val cfg = RemoteConfig.current
        if (Prefs.autoConnect.value && cfg.autoConnect && cfg.proxies.isNotEmpty() && ProxyManager.connected.value == null) {
            val free = cfg.proxies.filter { !it.premium }
            val last = free.firstOrNull { it.id == Prefs.lastProxy.value }
            val target = last ?: run { withTimeoutOrNull(4500) { ProxyManager.pingAll(free) }; ProxyManager.fastest(free) }
            if (target != null) ProxyManager.connect(target) {}
        }
        // App-open ad ka thoda intezaar (max ~4 sec), phir aage
        Ads.loadAppOpen()
        withTimeoutOrNull(4000) { while (!Ads.appOpenReady) delay(200) }
        Ads.showAppOpen(act) { done() }
    }
    Box(Modifier.fillMaxSize().background(BrandGradient), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            stars.forEach { drawCircle(Color.White.copy(alpha = .45f), 1.6.dp.toPx(), Offset(it.x * size.width, it.y * size.height)) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(120.dp).clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = .14f))
                    .border(2.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(70.dp)) }
            Text("ProxyGo Browser", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 24.dp))
            Box(Modifier.padding(top = 14.dp, bottom = 8.dp).width(150.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f))) {
                Box(Modifier.fillMaxWidth(p).height(5.dp).clip(CircleShape).background(Mint))
            }
            Text("Wait a second, secure proxy is loading", color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
        }
        Text("by MyFutureCode", color = Color.White.copy(alpha = .6f), fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp))
    }
}

// ---------------- Proxy list ----------------

private val unlocked = mutableStateListOf<String>()   // is session me ad dekh ke khole gaye premium servers

@Composable
fun ProxyListScreen(act: MainActivity) {
    BackHandler { act.go(Screen.Browser) }
    val cfg = RemoteConfig.config.value ?: return
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val list = cfg.proxies

    fun connect(s: ProxyServer) {
        ProxyManager.connect(s) { ok ->
            if (ok) { act.connectedPopup = s; act.go(Screen.Browser); act.browser.reloadAll() }
            else toast(act, "Proxy is not supported on this WebView. Please update Android System WebView.")
        }
    }

    LaunchedEffect(Unit) { ProxyManager.pingAll(list) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        TopBar("Proxy list", onBack = { act.go(Screen.Browser) }) {
            IconButton({
                scope.launch {
                    refreshing = true
                    RemoteConfig.refresh(act); ProxyManager.pingAll(RemoteConfig.current.proxies)
                    refreshing = false
                }
            }) { Icon(Icons.Default.Refresh, "Refresh", tint = if (refreshing) Color.Gray else Indigo) }
        }
        if (list.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No proxy servers yet.\nServers are added from the app config.", color = Color.Gray)
            }
        } else LazyColumn(Modifier.weight(1f)) {
            item {
                ProxyRow("⚡", "Auto (fastest)", "Best free server is chosen for you", null, Color.Transparent,
                    button = "Connect", buttonColor = Indigo, highlight = true) {
                    ProxyManager.fastest(list)?.let { connect(it) } ?: toast(act, "No server reachable right now")
                }
            }
            items(list, key = { "${it.id}#${it.name}" }) { s ->
                val ping = ProxyManager.pings[s.id]
                val isOn = ProxyManager.connected.value?.id == s.id
                val locked = s.premium && s.id !in unlocked
                val qColor = when (s.quality.lowercase()) { "high" -> GoodGreen; "medium" -> Gold; else -> Pink }
                val sub = s.quality.replaceFirstChar { it.uppercase() } + when (ping) { null -> " · …"; -1 -> " · offline"; else -> " · $ping ms" }
                ProxyRow(
                    s.flag, s.name, sub, qColor, Color.Transparent,
                    button = when { ProxyManager.connecting.value == s.id -> "Connecting"; isOn -> "Disconnect"; locked -> "▶ Unlock"; else -> "Connect" },
                    buttonColor = when { isOn -> Pink; locked -> Gold; else -> Indigo },
                ) {
                    when {
                        isOn -> ProxyManager.disconnect { act.browser.reloadAll() }
                        locked -> Ads.showRewarded(act, onReward = { unlocked.add(s.id); connect(s) }, onFail = { toast(act, "Ad not ready, try again in a moment") })
                        else -> Ads.maybeInterstitial(act) { connect(s) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        Ads.Banner()
    }
}

@Composable
private fun ProxyRow(
    flag: String, name: String, sub: String, dot: Color?, bg: Color, button: String, buttonColor: Color, highlight: Boolean = false, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(if (highlight) Indigo.copy(alpha = .07f) else bg).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(flag, fontSize = 28.sp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dot != null) Box(Modifier.padding(end = 5.dp).size(8.dp).clip(CircleShape).background(dot))
                Text(sub, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Box(
            Modifier.clip(RoundedCornerShape(10.dp)).background(buttonColor).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text(button, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

// ---------------- Settings ----------------

@Composable
fun SettingsScreen(act: MainActivity) {
    BackHandler { act.go(Screen.Browser) }
    val cfg = RemoteConfig.current
    var enginePick by remember { mutableStateOf(false) }
    var promo by remember { mutableStateOf(false) }
    val b = act.browser
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        TopBar("Settings", onBack = { act.go(Screen.Browser) })
        LazyColumn(Modifier.weight(1f)) {
            item { Section("GENERAL") }
            item { Toggle("Block images", Prefs.blockImages, "block_images") { b.applySettings() } }
            item { Toggle("Enable JavaScript", Prefs.javascript, "javascript") { b.applySettings() } }
            item { Toggle("Desktop site", Prefs.desktopMode, "desktop") { b.applySettings(); b.reloadAll() } }
            item { Toggle("Block ads on websites", Prefs.adBlock, "adblock") }
            item { Row2("Search engine", Prefs.searchEngine.value) { enginePick = true } }
            item { Row2("Download location", "Download/ProxyGo") {} }
            item { Section("PRIVACY") }
            item { Toggle("Clear history on exit", Prefs.clearOnExit, "clear_on_exit") }
            item { Toggle("Auto-connect proxy on start", Prefs.autoConnect, "auto_connect") }
            item { Row2("Clear browsing data", "History, cookies, cache") { act.clearBrowsingData(); toast(act, "Browsing data cleared") } }
            item { Section("DISPLAY") }
            item { Toggle("Dark mode", Prefs.darkMode, "dark") }
            item { Section("ABOUT") }
            if (cfg.privacyUrl.isNotBlank()) item { Row2("Privacy policy", null) { act.browser.open(cfg.privacyUrl); act.go(Screen.Browser) } }
            item { Row2("Rate us", "Version ${BuildConfig.VERSION_NAME}") { openStore(act, act.packageName) } }
            item {
                Row2("Share app", null) {
                    act.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "Try ProxyGo Browser: https://play.google.com/store/apps/details?id=${act.packageName}"), "Share"))
                }
            }
            if (cfg.crossPromo.isNotEmpty()) item { Row2("More apps", "From MyFutureCode") { promo = true } }
        }
    }
    if (enginePick) AlertDialog(
        onDismissRequest = { enginePick = false },
        title = { Text("Search engine") },
        text = {
            Column {
                Prefs.engines.keys.forEach { name ->
                    Row(Modifier.fillMaxWidth().clickable { Prefs.setEngine(name); enginePick = false }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(Prefs.searchEngine.value == name, { Prefs.setEngine(name); enginePick = false })
                        Text(name)
                    }
                }
            }
        },
        confirmButton = { TextButton({ enginePick = false }) { Text("Close") } },
    )
    if (promo) AlertDialog(
        onDismissRequest = { promo = false },
        title = { Text("More apps") },
        text = {
            LazyColumn {
                items(cfg.crossPromo) { a ->
                    Row(Modifier.fillMaxWidth().clickable { openStore(act, a.pkg) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(a.icon, a.name, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(a.name, fontWeight = FontWeight.SemiBold)
                            if (a.desc.isNotBlank()) Text(a.desc, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ promo = false }) { Text("Close") } },
    )
}

@Composable
private fun Section(t: String) =
    Text(t, color = Indigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 4.dp))

@Composable
private fun Toggle(title: String, state: MutableState<Boolean>, key: String, after: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clickable { Prefs.set(key, state, !state.value); after() }.padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), fontSize = 15.sp)
        Switch(state.value, { Prefs.set(key, state, it); after() })
    }
}

@Composable
private fun Row2(title: String, sub: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(title, fontSize = 15.sp)
        if (sub != null) Text(sub, fontSize = 12.sp, color = Color.Gray)
    }
}

// ---------------- History / Bookmarks ----------------

@Composable
fun PagesScreen(act: MainActivity, bookmarks: Boolean) {
    BackHandler { act.go(Screen.Browser) }
    val list = if (bookmarks) Prefs.bookmarks else Prefs.history
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        TopBar(if (bookmarks) "Bookmarks" else "History", onBack = { act.go(Screen.Browser) }) {
            if (!bookmarks && list.isNotEmpty()) TextButton({ Prefs.clearHistory() }) { Text("Clear all") }
        }
        if (list.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(if (bookmarks) "No bookmarks yet" else "No history yet", color = Color.Gray)
        } else LazyColumn(Modifier.weight(1f)) {
            items(list.toList()) { e ->
                Row(
                    Modifier.fillMaxWidth().clickable { act.browser.open(e.url); act.go(Screen.Browser) }.padding(start = 18.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(e.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = Color.Gray)
                        Text(fmt.format(Date(e.time)), fontSize = 11.sp, color = Color.Gray)
                    }
                    if (bookmarks) IconButton({ Prefs.removeBookmark(e) }) { Icon(Icons.Default.Close, "Remove") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        Ads.Banner()
    }
}
