package com.myfuturecode.proxygo

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt
import kotlin.random.Random

// ---------------- Splash ----------------

@Composable
fun SplashScreen(act: MainActivity, done: () -> Unit) {
    val anim = rememberInfiniteTransition(label = "bar")
    val p by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val twinkle by anim.animateFloat(.25f, .8f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "t")
    val stars = remember { List(55) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    LaunchedEffect(Unit) {
        withTimeoutOrNull(6000) { RemoteConfig.refresh(act) }
        Ads.preload(act)
        Ads.loadAppOpen()
        // Proxy list background me load hoti rahe; auto-connect ke liye max ~9 sec
        val job = launch { ProxyManager.loadServers() }
        if (Prefs.autoConnect.value && RemoteConfig.current.autoConnect && ProxyManager.connected.value == null) {
            withTimeoutOrNull(9000) {
                while (ProxyManager.servers.none { (ProxyManager.pings[it.id] ?: 0) > 0 } && job.isActive) delay(250)
                val target = ProxyManager.servers.firstOrNull { it.id == Prefs.lastProxy.value && (ProxyManager.pings[it.id] ?: 0) > 0 }
                    ?: ProxyManager.fastest()
                if (target != null) ProxyManager.connect(target)
            }
        } else delay(1500)
        withTimeoutOrNull(3000) { while (!Ads.appOpenReady) delay(200) }
        Ads.showAppOpen(act) { done() }
    }
    Box(Modifier.fillMaxSize().background(SplashGradient), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            stars.forEach { (x, y, s) ->
                drawCircle(Color.White.copy(alpha = if (s > .5f) twinkle else 1f - twinkle), (0.8f + s * 1.6f).dp.toPx(), Offset(x * size.width, y * size.height))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(Color.White.copy(alpha = .06f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(100.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Key, null, tint = Color.White, modifier = Modifier.size(52.dp))
                }
            }
            Text("ProxyGo Browser", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 26.dp))
            Text("Fast · Private · Unblock", color = Mint, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(210.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = .18f))) {
                Box(Modifier.fillMaxWidth(p).height(3.dp).clip(CircleShape).background(Color.White))
            }
            Text("Wait a second, proxy is loading", color = Color.White.copy(alpha = .8f), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

// ---------------- Proxy list ----------------

@Composable
fun ProxyListScreen(act: MainActivity) {
    BackHandler { act.go(Screen.Browser) }
    val cfg = RemoteConfig.config.value ?: return
    val scope = rememberCoroutineScope()
    var askAd by remember { mutableStateOf<ProxyServer?>(null) }
    var loadingDlg by remember { mutableStateOf<ProxyServer?>(null) }
    val list = ProxyManager.servers

    fun doConnect(s: ProxyServer) {
        loadingDlg = s
        scope.launch {
            if (ProxyManager.connected.value != null) {
                kotlin.coroutines.suspendCoroutine { c -> ProxyManager.disconnect { c.resumeWith(Result.success(Unit)) } }
            }
            val ok = ProxyManager.connect(s)
            loadingDlg = null
            when {
                ok -> { act.browser.reloadAll(); act.connectedPopup = s; act.go(Screen.Browser) }
                !ProxyManager.supported -> toast(act, "Please update Android System WebView to use proxy")
                else -> toast(act, "${s.name} is not responding, try another server")
            }
        }
    }

    LaunchedEffect(Unit) { if (list.isEmpty() && !ProxyManager.loading.value) ProxyManager.loadServers() }

    Column(Modifier.fillMaxSize().background(Color.White).navigationBarsPadding()) {
        TopBar("Proxy list", onBack = { act.go(Screen.Browser) }, centered = true) {
            if (ProxyManager.loading.value) CircularProgressIndicator(Modifier.padding(end = 14.dp).size(20.dp), color = Muted, strokeWidth = 2.dp)
            else IconButton({ scope.launch { RemoteConfig.refresh(act); ProxyManager.loadServers() } }) {
                Icon(Icons.Default.Refresh, "Refresh", tint = Muted)
            }
        }
        if (list.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (ProxyManager.loading.value) {
                    CircularProgressIndicator(color = Navy)
                    Text("Finding fast servers…", color = Muted, modifier = Modifier.padding(top = 16.dp))
                } else {
                    Text("No server is reachable right now.\nCheck your internet and tap refresh.", color = Muted, textAlign = TextAlign.Center)
                    TextButton({ scope.launch { ProxyManager.loadServers() } }) { Text("Refresh", color = LinkBlue) }
                }
            }
        } else LazyColumn(Modifier.weight(1f)) {
            items(list, key = { it.id }) { s ->
                val ping = ProxyManager.pings[s.id]
                val isOn = ProxyManager.connected.value?.id == s.id
                val busy = ProxyManager.connecting.value == s.id || loadingDlg?.id == s.id
                ProxyRow(s, ping, if (busy) 2 else if (isOn) 1 else 0) {
                    when {
                        busy -> {}
                        isOn -> ProxyManager.disconnect { act.browser.reloadAll() }
                        cfg.connectAd && Ads.rewardedAvailable -> askAd = s
                        else -> Ads.maybeInterstitial(act) { doConnect(s) }
                    }
                }
                HorizontalDivider(Modifier.padding(start = 76.dp), color = Line)
            }
            if (ProxyManager.loading.value) item {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Muted, strokeWidth = 2.dp)
                    Text("  Checking more servers…", color = Muted, fontSize = 13.sp)
                }
            }
        }
        Ads.Banner()
    }

    askAd?.let { s ->
        WatchVideoDialog(
            onClose = { askAd = null },
            onWatch = {
                askAd = null
                Ads.showRewarded(act, onReward = { doConnect(s) }, onFail = { doConnect(s) })
            },
        )
    }
    loadingDlg?.let { LoadingDialog() }
}

@Composable
private fun ProxyRow(s: ProxyServer, ping: Int?, state: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            s.flagUrl, s.country, Modifier.size(width = 44.dp, height = 30.dp).clip(RoundedCornerShape(4.dp)).border(.5.dp, Line, RoundedCornerShape(4.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(s.name, fontSize = 15.sp, color = Ink, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val qColor = when (s.quality.lowercase()) { "high" -> GoodGreen; "medium" -> Orange; else -> Pink }
            Text(
                buildAnnotatedString {
                    append("Quality: ")
                    withStyle(SpanStyle(color = qColor, fontWeight = FontWeight.Bold)) { append(s.quality.lowercase()) }
                    if (ping != null && ping > 0) withStyle(SpanStyle(color = Muted)) { append("   ·   $ping ms") }
                },
                fontSize = 12.sp, color = Muted,
            )
        }
        val (label, color) = when (state) { 2 -> "Connecting" to Orange; 1 -> "Disconnect" to Pink; else -> "Connect" to Navy }
        Box(
            Modifier.width(104.dp).height(34.dp).clip(RoundedCornerShape(8.dp)).background(color).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun WatchVideoDialog(onClose: () -> Unit, onWatch: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(78.dp).clip(CircleShape).background(Orange.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = Orange, modifier = Modifier.size(46.dp))
                }
                Text("Free fast server", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 14.dp))
                Text(
                    "Watch a short video to connect this server for free.",
                    fontSize = 14.sp, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                )
                Row(
                    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(ButtonGradient).clickable(onClick = onWatch),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("  Watch video", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClose, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.Close, "Close", tint = Muted) }
        }
    }
}

@Composable
private fun LoadingDialog() {
    Dialog(onDismissRequest = {}) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(horizontal = 36.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = Navy)
            Text("Loading…", color = Ink, modifier = Modifier.padding(top = 14.dp))
        }
    }
}

// ---------------- Settings ----------------

private enum class SetPage(val title: String) { Root("Settings"), General("General"), Display("Display"), Privacy("Privacy"), Advanced("Advanced"), About("About"), Faq("FAQ") }

@Composable
fun SettingsScreen(act: MainActivity) {
    var page by remember { mutableStateOf(SetPage.Root) }
    val back = { if (page == SetPage.Root) act.go(Screen.Browser) else page = SetPage.Root }
    BackHandler(onBack = back)
    val cfg = RemoteConfig.current
    var enginePick by remember { mutableStateOf(false) }
    var promo by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val b = act.browser
    Column(Modifier.fillMaxSize().background(Color.White).navigationBarsPadding()) {
        TopBar(page.title, onBack = back)
        LazyColumn(Modifier.weight(1f)) {
            when (page) {
                SetPage.Root -> {
                    item { NavRow("General", "Search engine, downloads", Icons.Outlined.Settings) { page = SetPage.General } }
                    item { NavRow("Display", "Text size, desktop site", Icons.Outlined.Palette) { page = SetPage.Display } }
                    item { NavRow("Privacy", "History, cookies, ad block", Icons.Outlined.Lock) { page = SetPage.Privacy } }
                    item { NavRow("Advanced", "JavaScript, images, proxy", Icons.Outlined.Tune) { page = SetPage.Advanced } }
                    item { NavRow("FAQ", "Common questions", Icons.Outlined.QuestionAnswer) { page = SetPage.Faq } }
                    item { NavRow("About", "Version ${BuildConfig.VERSION_NAME}", Icons.Outlined.Info) { page = SetPage.About } }
                }
                SetPage.General -> {
                    item { ValueRow("Search engine", Prefs.searchEngine.value) { enginePick = true } }
                    item { CheckRow("Show search suggestions", null, Prefs.searchSuggest, "suggest") }
                    item { ValueRow("Download location", "Internal storage/Download/ProxyGo") {} }
                    item { CheckRow("Auto-connect proxy on start", "Connect to last or fastest server", Prefs.autoConnect, "auto_connect") }
                }
                SetPage.Display -> {
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Text("Text size  ${Prefs.textZoom.intValue}%", fontSize = 15.sp, color = Ink)
                            Slider(
                                Prefs.textZoom.intValue.toFloat(),
                                { Prefs.setTextZoom((it / 10).roundToInt() * 10) },
                                valueRange = 50f..200f, steps = 14,
                                onValueChangeFinished = { b.applySettings() },
                            )
                            Text("Preview text at this size", fontSize = (15 * Prefs.textZoom.intValue / 100f).sp, color = Muted)
                        }
                    }
                    item { CheckRow("Desktop site", "Request desktop version of websites", Prefs.desktopMode, "desktop") { b.applySettings(); b.reloadAll() } }
                    item { CheckRow("Wide viewport", "Fit pages to screen width", Prefs.wideViewport, "wide") { b.applySettings() } }
                    item { CheckRow("Open pages in overview", "Zoomed out first", Prefs.overview, "overview") { b.applySettings() } }
                }
                SetPage.Privacy -> {
                    item { CheckRow("Block ads on websites", null, Prefs.adBlock, "adblock") }
                    item { CheckRow("Accept cookies", null, Prefs.acceptCookies, "cookies") { b.applySettings() } }
                    item { CheckRow("Clear history on exit", null, Prefs.clearOnExit, "clear_on_exit") }
                    item { ValueRow("Clear browsing data", "History, cookies, cache") { confirmClear = true } }
                    if (cfg.privacyUrl.isNotBlank()) item { ValueRow("Privacy policy", null) { b.open(cfg.privacyUrl); act.go(Screen.Browser) } }
                }
                SetPage.Advanced -> {
                    item { CheckRow("Enable JavaScript", null, Prefs.javascript, "javascript") { b.applySettings(); b.reloadAll() } }
                    item { CheckRow("Block images", "Save data", Prefs.blockImages, "block_images") { b.applySettings(); b.reloadAll() } }
                    item {
                        ValueRow("Proxy", ProxyManager.connected.value?.let { "${it.name} · ${ProxyManager.connectedIp.value}" } ?: "Not connected") {
                            act.go(Screen.Proxies)
                        }
                    }
                    item { ValueRow("Clear cache", null) { b.clearCache(); toast(act, "Cache cleared") } }
                }
                SetPage.About -> {
                    item {
                        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(78.dp).clip(RoundedCornerShape(20.dp)).background(BrandGradient), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Key, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            }
                            Text("ProxyGo Browser", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 12.dp))
                            Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = Muted)
                        }
                    }
                    item { ValueRow("Rate us", "Tell us what you think") { openStore(act, act.packageName) } }
                    item {
                        ValueRow("Share app", null) {
                            act.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, "Try ProxyGo Browser: https://play.google.com/store/apps/details?id=${act.packageName}"), "Share"))
                        }
                    }
                    if (cfg.crossPromo.isNotEmpty()) item { ValueRow("More apps", "From MyFutureCode") { promo = true } }
                    if (cfg.privacyUrl.isNotBlank()) item { ValueRow("Privacy policy", null) { b.open(cfg.privacyUrl); act.go(Screen.Browser) } }
                }
                SetPage.Faq -> items(faq) { (q, a) ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(q, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 15.sp)
                        Text(a, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    HorizontalDivider(color = Line)
                }
            }
        }
    }
    if (enginePick) AlertDialog(
        onDismissRequest = { enginePick = false },
        containerColor = Color.White,
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
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        containerColor = Color.White,
        icon = { Icon(Icons.Outlined.Delete, null) },
        title = { Text("Clear browsing data?") },
        text = { Text("History, cookies and cached files will be removed.") },
        confirmButton = { TextButton({ act.clearBrowsingData(); confirmClear = false; toast(act, "Browsing data cleared") }) { Text("Clear", color = Pink) } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Cancel") } },
    )
    if (promo) AlertDialog(
        onDismissRequest = { promo = false },
        containerColor = Color.White,
        title = { Text("More apps") },
        text = {
            LazyColumn {
                items(cfg.crossPromo) { a ->
                    Row(Modifier.fillMaxWidth().clickable { openStore(act, a.pkg) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(a.icon, a.name, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(a.name, fontWeight = FontWeight.SemiBold)
                            if (a.desc.isNotBlank()) Text(a.desc, fontSize = 12.sp, color = Muted, maxLines = 2)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ promo = false }) { Text("Close") } },
    )
}

private val faq = listOf(
    "What does the proxy do?" to "Pages you open in this browser go through the selected server, so websites see the server's location instead of yours.",
    "Is this a VPN?" to "No. Only this browser's traffic uses the proxy. Other apps on your phone are not affected.",
    "A server is not working" to "Free servers change often. Open the proxy list, tap refresh and pick another country.",
    "Why do I see ads?" to "Ads keep the app and its servers free. You can turn on 'Block ads on websites' to block ads inside web pages.",
    "Where are my downloads?" to "Files are saved in Internal storage/Download/ProxyGo.",
)

@Composable
private fun NavRow(title: String, sub: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Indigo.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Indigo, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, fontSize = 15.sp, color = Ink)
            Text(sub, fontSize = 12.sp, color = Muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
    }
}

@Composable
private fun CheckRow(title: String, sub: String?, state: MutableState<Boolean>, key: String, after: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clickable { Prefs.set(key, state, !state.value); after() }.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(title, fontSize = 15.sp, color = Ink)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Muted)
        }
        Checkbox(state.value, { Prefs.set(key, state, it); after() }, colors = CheckboxDefaults.colors(checkedColor = LinkBlue))
    }
}

@Composable
private fun ValueRow(title: String, sub: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 13.dp)) {
        Text(title, fontSize = 15.sp, color = Ink)
        if (sub != null) Text(sub, fontSize = 12.sp, color = Muted)
    }
}

// ---------------- History / Bookmarks ----------------

@Composable
fun PagesScreen(act: MainActivity, bookmarks: Boolean) {
    BackHandler { act.go(Screen.Browser) }
    val list = if (bookmarks) Prefs.bookmarks else Prefs.history
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    Column(Modifier.fillMaxSize().background(Color.White).navigationBarsPadding()) {
        TopBar(if (bookmarks) "Bookmarks" else "History", onBack = { act.go(Screen.Browser) }) {
            if (!bookmarks && list.isNotEmpty()) TextButton({ Prefs.clearHistory() }) { Text("Clear all", color = Pink) }
        }
        if (list.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (bookmarks) "No bookmarks yet" else "No history yet", color = Muted)
            Spacer(Modifier.height(4.dp))
            Text(if (bookmarks) "Tap Bookmark in the menu to save a page" else "Pages you visit will show here", color = Muted, fontSize = 12.sp)
        } else LazyColumn(Modifier.weight(1f)) {
            items(list.toList()) { e ->
                Row(
                    Modifier.fillMaxWidth().clickable { act.browser.open(e.url); act.go(Screen.Browser) }.padding(start = 18.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        "https://www.google.com/s2/favicons?sz=64&domain_url=" + android.net.Uri.encode(e.url), null,
                        Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Tile),
                    )
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(e.title.ifBlank { e.url }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink, fontSize = 15.sp)
                        Text(e.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = Muted)
                        if (!bookmarks) Text(fmt.format(Date(e.time)), fontSize = 11.sp, color = Muted)
                    }
                    IconButton({ if (bookmarks) Prefs.removeBookmark(e) else Prefs.removeHistory(e) }) { Icon(Icons.Default.Close, "Remove", tint = Muted) }
                }
                HorizontalDivider(Modifier.padding(start = 60.dp), color = Line)
            }
        }
        Ads.Banner()
    }
}
