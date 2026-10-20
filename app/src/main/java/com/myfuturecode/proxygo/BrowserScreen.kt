package com.myfuturecode.proxygo

import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

private enum class Panel { None, Menu, Tabs }

@Composable
fun BrowserScreen(act: MainActivity) {
    val b = act.browser
    val tab = b.current
    var panel by remember { mutableStateOf(Panel.None) }
    var clearAsk by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }

    BackHandler(enabled = panel != Panel.None || finding || !tab.isHome || b.customView != null) {
        when {
            b.customView != null -> b.exitFullscreen()
            panel != Panel.None -> panel = Panel.None
            finding -> { finding = false; tab.webView.clearMatches() }
            else -> b.back()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            AddressBar(act, tab)
            Box(Modifier.fillMaxWidth().height(2.dp)) {
                if (tab.progress < 100) LinearProgressIndicator(
                    progress = { tab.progress / 100f }, modifier = Modifier.fillMaxSize(), color = LinkBlue, trackColor = Color.Transparent,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (tab.isHome) HomeContent(act)
                else androidx.compose.runtime.key(tab) { WebViewHost(tab) }
                // Menu / tabs panel neeche bar ke theek upar
                if (panel != Panel.None) Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = .08f))
                        .clickable(remember { MutableInteractionSource() }, null) { panel = Panel.None },
                )
                Column(Modifier.align(Alignment.BottomCenter)) {
                    AnimatedVisibility(panel == Panel.Menu, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                        MenuPanel(act, tab, close = { panel = Panel.None }, find = { finding = true })
                    }
                    AnimatedVisibility(panel == Panel.Tabs, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                        TabsPanel(b) { panel = Panel.None }
                    }
                }
            }
            if (finding) FindBar(tab) { finding = false }
            BottomBar(
                historyCount = Prefs.history.size,
                tabCount = b.tabs.size,
                onFlag = { panel = Panel.None; act.go(Screen.Proxies) },
                onTrash = { panel = Panel.None; clearAsk = true },
                onNewTab = { panel = Panel.None; b.newTab() },
                onMenu = { panel = if (panel == Panel.Menu) Panel.None else Panel.Menu },
                onTabs = { panel = if (panel == Panel.Tabs) Panel.None else Panel.Tabs },
            )
        }
        b.customView?.let { v ->
            AndroidView(
                factory = { FrameLayout(it).apply { setBackgroundColor(android.graphics.Color.BLACK); (v.parent as? ViewGroup)?.removeView(v); addView(v) } },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (clearAsk) AlertDialog(
        onDismissRequest = { clearAsk = false },
        title = { Text("Clear history") },
        text = { Text("Would you like to clear all browser history?") },
        confirmButton = { TextButton({ act.clearBrowsingData(); clearAsk = false; toast(act, "History cleared") }) { Text("YES") } },
        dismissButton = { TextButton({ clearAsk = false }) { Text("NO") } },
        containerColor = Color.White, shape = RoundedCornerShape(6.dp),
    )

    act.connectedPopup?.let { s -> ConnectedDialog(act, s) }
}

@Composable
private fun WebViewHost(tab: Tab) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            FrameLayout(ctx).apply {
                (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
                addView(tab.webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        },
        onRelease = { it.removeAllViews() },
    )
}

// ---------------- Address bar + search suggestions ----------------

@Composable
private fun AddressBar(act: MainActivity, tab: Tab) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val req = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    val suggestions = remember { mutableStateListOf<String>() }

    // Edit nahi kar rahe to bar me page ka URL; tap karte hi poora URL select
    LaunchedEffect(tab, tab.url, editing) {
        field = if (!editing) TextFieldValue(if (tab.isHome) "" else tab.url)
        else TextFieldValue(if (tab.isHome) "" else tab.url, TextRange(0, if (tab.isHome) 0 else tab.url.length))
    }
    LaunchedEffect(field.text, editing) {
        suggestions.clear()
        val q = field.text.trim()
        if (!editing || !Prefs.searchSuggest.value || q.length < 2 || q == tab.url) return@LaunchedEffect
        delay(250)
        suggestions.addAll(fetchSuggestions(q))
    }

    fun go(text: String) {
        if (text.isBlank()) return
        act.browser.open(text); focus.clearFocus(); keyboard?.hide(); editing = false
        Ads.maybeInterstitial(act)
    }

    Column {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().height(42.dp)
                .clip(RoundedCornerShape(21.dp)).background(Color.White)
                .border(1.dp, if (editing) Indigo.copy(alpha = .5f) else Color(0xFFDADCE3), RoundedCornerShape(21.dp))
                .clickable { req.requestFocus() }.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!tab.isHome && !editing && tab.url.startsWith("https")) Icon(Icons.Outlined.Lock, null, tint = GoodGreen, modifier = Modifier.size(16.dp))
            else GoogleG(18)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (field.text.isEmpty()) Text("Search or enter the address", color = Muted, fontSize = 13.sp)
                BasicTextField(
                    value = field, onValueChange = { field = it }, singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = Ink),
                    cursorBrush = SolidColor(Indigo),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, keyboardType = KeyboardType.Uri),
                    keyboardActions = KeyboardActions(onGo = { go(field.text) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(req).onFocusChanged { editing = it.isFocused },
                )
            }
            when {
                editing && field.text.isNotEmpty() -> SmallIcon(Icons.Outlined.Close) { field = TextFieldValue("") }
                editing -> SmallIcon(Icons.Outlined.Close) { focus.clearFocus(); keyboard?.hide() }
                tab.isHome -> SmallIcon(Icons.Outlined.Close) { req.requestFocus() }
                tab.progress < 100 -> SmallIcon(Icons.Outlined.Close) { tab.webView.stopLoading() }
                else -> SmallIcon(Icons.Outlined.Refresh) { tab.webView.reload() }
            }
        }
        if (editing && suggestions.isNotEmpty()) Column(Modifier.fillMaxWidth().background(Color.White)) {
            suggestions.take(6).forEach { s ->
                Row(Modifier.fillMaxWidth().clickable { go(s) }.padding(horizontal = 20.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = Muted, modifier = Modifier.size(18.dp))
                    Text(s, fontSize = 14.sp, color = Ink, modifier = Modifier.padding(start = 14.dp).weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(
                        Icons.Outlined.CallMade, "Fill", tint = Muted,
                        modifier = Modifier.size(18.dp).rotate(-90f).clickable { field = TextFieldValue("$s ", TextRange(s.length + 1)) },
                    )
                }
            }
            HorizontalDivider(color = Line)
        }
    }
}

private suspend fun fetchSuggestions(q: String): List<String> = withContext(Dispatchers.IO) {
    runCatching {
        val txt = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=" + android.net.Uri.encode(q)).readText()
        val arr = JSONArray(txt).getJSONArray(1)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())
}

/** Google ka rangeen "G" logo. */
@Composable
fun GoogleG(size: Int) {
    AsyncImage("https://www.google.com/s2/favicons?domain=google.com&sz=64", "Google", Modifier.size(size.dp), contentScale = ContentScale.Fit)
}

@Composable
private fun SmallIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
    }
}

// ---------------- Home (quick sites) ----------------

@Composable
private fun HomeContent(act: MainActivity) {
    val cfg = RemoteConfig.config.value ?: return
    val proxy = ProxyManager.connected.value
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            GridCells.Fixed(4), Modifier.weight(1f).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(cfg.quickSites) { s ->
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable { act.browser.open(s.url); Ads.maybeInterstitial(act) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(54.dp).shadow(1.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(Tile),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(s.iconUrl, s.name, Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Fit)
                    }
                    Text(s.name, fontSize = 11.sp, color = Color(0xFF555560), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
        // Proxy ka haal (chhota, saaf)
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .border(1.dp, Line, RoundedCornerShape(12.dp)).clickable { act.go(Screen.Proxies) }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (proxy != null) AsyncImage(proxy.flagUrl, null, Modifier.size(width = 26.dp, height = 18.dp).clip(RoundedCornerShape(3.dp)), contentScale = ContentScale.Crop)
            else Icon(Icons.Outlined.Shield, null, tint = Muted, modifier = Modifier.size(20.dp))
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(proxy?.name ?: "Proxy not connected", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(if (proxy != null) "IP ${ProxyManager.connectedIp.value}" else "Tap to choose a server", fontSize = 11.sp, color = Muted)
            }
            if (proxy != null) Text("Connected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoodGreen)
            else Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Navy).padding(horizontal = 12.dp, vertical = 6.dp))
        }
        Ads.Banner()
    }
}

// ---------------- Bottom bar ----------------

@Composable
private fun BottomBar(
    historyCount: Int, tabCount: Int,
    onFlag: () -> Unit, onTrash: () -> Unit, onNewTab: () -> Unit, onMenu: () -> Unit, onTabs: () -> Unit,
) {
    Column(Modifier.background(Color.White)) {
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            BarButton(onFlag) {
                AsyncImage(
                    flagUrl(ProxyManager.connected.value?.country ?: deviceCountry()), "Proxy servers",
                    Modifier.size(width = 26.dp, height = 18.dp).clip(RoundedCornerShape(3.dp)).border(0.5.dp, Line, RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            BarButton(onTrash) {
                Icon(Icons.Outlined.Delete, "Clear history", tint = Color(0xFF6B6B75))
                if (historyCount > 0) Text(
                    if (historyCount > 99) "99+" else "$historyCount", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp).clip(CircleShape).background(Pink).padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            BarButton(onNewTab) { Icon(Icons.Outlined.AddCircleOutline, "New tab", tint = Color(0xFF6B6B75)) }
            BarButton(onMenu) { Icon(Icons.AutoMirrored.Outlined.Notes, "Menu", tint = Color(0xFF6B6B75)) }
            BarButton(onTabs) {
                Box(Modifier.size(20.dp).border(1.6.dp, Color(0xFF6B6B75), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text("$tabCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B6B75))
                }
            }
        }
    }
}

@Composable
private fun BarButton(onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

fun deviceCountry(): String = java.util.Locale.getDefault().country.ifBlank { "IN" }

// ---------------- Menu panel ----------------

private data class MenuItem(val label: String, val icon: ImageVector, val active: Boolean = false, val action: () -> Unit)

@Composable
private fun MenuPanel(act: MainActivity, tab: Tab, close: () -> Unit, find: () -> Unit) {
    val b = act.browser
    val bookmarked = !tab.isHome && Prefs.bookmarks.any { it.url == tab.url }
    val items = listOf(
        MenuItem("Share", Icons.Outlined.Share) {
            val text = if (tab.isHome) "https://play.google.com/store/apps/details?id=${act.packageName}" else tab.url
            act.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share"))
        },
        MenuItem("History", Icons.Outlined.History) { act.go(Screen.History) },
        MenuItem("Download", Icons.Outlined.Download) {
            runCatching { act.startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        MenuItem("Incognito", Icons.Outlined.VisibilityOff, Prefs.incognito.value) {
            Prefs.incognito.value = !Prefs.incognito.value
            toast(act, if (Prefs.incognito.value) "Incognito on: history won't be saved" else "Incognito off")
        },
        MenuItem("Desktop site", Icons.Outlined.Computer, Prefs.desktopMode.value) {
            Prefs.set("desktop", Prefs.desktopMode, !Prefs.desktopMode.value); b.applySettings(); b.reloadAll()
        },
        MenuItem("Search", Icons.Outlined.Search) { if (!tab.isHome) find() else toast(act, "Open a page to search in it") },
        MenuItem("Forward", Icons.AutoMirrored.Filled.ArrowForward) { if (tab.webView.canGoForward()) tab.webView.goForward() },
        MenuItem("Settings", Icons.Outlined.Settings) { act.go(Screen.Settings) },
        MenuItem(if (bookmarked) "Saved" else "Bookmark", Icons.Outlined.BookmarkAdd, bookmarked) {
            if (tab.isHome) toast(act, "Open a page to bookmark it") else Prefs.toggleBookmark(tab.title, tab.url)
        },
        MenuItem("Bookmarks", Icons.Outlined.Bookmarks) { act.go(Screen.Bookmarks) },
        MenuItem("Ad block", Icons.Outlined.Block, Prefs.adBlock.value) {
            Prefs.set("adblock", Prefs.adBlock, !Prefs.adBlock.value)
            toast(act, if (Prefs.adBlock.value) "Ad block on" else "Ad block off")
        },
        MenuItem("Proxy", Icons.Outlined.Language, ProxyManager.connected.value != null) { act.go(Screen.Proxies) },
    )
    Surface(color = Color.White, shadowElevation = 10.dp, shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            items.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { m ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { close(); m.action() }.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(m.icon, m.label, tint = if (m.active) Indigo else Color(0xFF6B6B75), modifier = Modifier.size(24.dp))
                            Text(m.label, fontSize = 11.sp, color = if (m.active) Indigo else Color(0xFF6B6B75), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Tabs panel ----------------

@Composable
private fun TabsPanel(b: BrowserState, close: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 10.dp, shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).padding(vertical = 6.dp)) {
            LazyColumn(Modifier.weight(1f, fill = false)) {
                itemsIndexed(b.tabs) { i, t ->
                    Row(
                        Modifier.fillMaxWidth().background(if (i == b.currentIndex) Indigo.copy(alpha = .06f) else Color.Transparent)
                            .clickable { b.currentIndex = i; close() }.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Language, null, tint = Color(0xFF6B6B75), modifier = Modifier.size(20.dp))
                        Text(
                            if (t.isHome) "New tab" else t.title.ifBlank { t.url }, fontSize = 14.sp,
                            fontWeight = if (i == b.currentIndex) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 16.dp).weight(1f),
                        )
                        IconButton({ b.closeTab(i) }) {
                            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF6B6B75)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Close, "Close tab", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = Line)
            Row(Modifier.fillMaxWidth().clickable { b.newTab(); close() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AddCircleOutline, null, tint = Indigo, modifier = Modifier.size(20.dp))
                Text("New tab", color = Indigo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

// ---------------- Find in page ----------------

@Composable
private fun FindBar(tab: Tab, close: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    val req = remember { FocusRequester() }
    LaunchedEffect(tab) {
        tab.webView.setFindListener { active, total, done -> if (done) result = if (total == 0) "0/0" else "${active + 1}/$total" }
        req.requestFocus()
    }
    Row(Modifier.fillMaxWidth().background(Tile).padding(start = 14.dp, end = 4.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, null, tint = Muted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (q.isEmpty()) Text("Find in page", color = Muted, fontSize = 14.sp)
            BasicTextField(q, { q = it; tab.webView.findAllAsync(it) }, singleLine = true, textStyle = TextStyle(fontSize = 14.sp, color = Ink),
                modifier = Modifier.fillMaxWidth().focusRequester(req))
        }
        Text(result, fontSize = 12.sp, color = Muted)
        SmallIcon(Icons.Outlined.KeyboardArrowUp) { tab.webView.findNext(false) }
        SmallIcon(Icons.Outlined.KeyboardArrowDown) { tab.webView.findNext(true) }
        SmallIcon(Icons.Outlined.Close) { tab.webView.clearMatches(); close() }
    }
}

// ---------------- Proxy connected card ----------------

@Composable
private fun ConnectedDialog(act: MainActivity, s: ProxyServer) {
    Dialog(onDismissRequest = { act.connectedPopup = null }) {
        Box(Modifier.fillMaxWidth(.85f), contentAlignment = Alignment.TopCenter) {
            Surface(Modifier.padding(top = 30.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White) {
                Box {
                    IconButton({ act.connectedPopup = null }, Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Outlined.Close, "Close", tint = Color(0xFF555560))
                    }
                    Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 40.dp, bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Proxy connected", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Medium)
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(s.flagUrl, null, Modifier.size(width = 20.dp, height = 14.dp).clip(RoundedCornerShape(2.dp)), contentScale = ContentScale.Crop)
                            Text("  ${s.name} · ${ProxyManager.connectedIp.value}", fontSize = 12.sp, color = Muted)
                        }
                    }
                }
            }
            Box(
                Modifier.size(62.dp).clip(CircleShape).background(Color.White).padding(5.dp).clip(CircleShape).background(LinkBlue),
                contentAlignment = Alignment.Center,
            ) { Text("✓", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ---------------- Shared helpers ----------------

@Composable
fun TopBar(title: String, onBack: () -> Unit, centered: Boolean = false, actions: @Composable () -> Unit = {}) {
    Column(Modifier.background(Color.White).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            IconButton(onBack, Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = if (centered) Muted else Ink)
            }
            Text(
                title, fontSize = if (centered) 16.sp else 19.sp, color = if (centered) Muted else Ink,
                fontWeight = if (centered) FontWeight.Normal else FontWeight.Medium,
                modifier = if (centered) Modifier.align(Alignment.Center) else Modifier.align(Alignment.CenterStart).padding(start = 60.dp),
            )
            Row(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
        }
        HorizontalDivider(color = Line)
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(ButtonGradient).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Bold) }
}

fun toast(act: android.content.Context, msg: String) = android.widget.Toast.makeText(act, msg, android.widget.Toast.LENGTH_SHORT).show()

fun openStore(act: android.content.Context, pkg: String) {
    val market = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { act.startActivity(market) }.onFailure {
        act.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
