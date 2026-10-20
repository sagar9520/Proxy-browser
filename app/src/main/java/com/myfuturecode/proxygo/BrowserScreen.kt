package com.myfuturecode.proxygo

import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(act: MainActivity) {
    val b = act.browser
    val tab = b.current
    var menu by remember { mutableStateOf(false) }
    var tabsSheet by remember { mutableStateOf(false) }
    var clearAsk by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }

    BackHandler(enabled = finding || !tab.isHome) {
        if (finding) { finding = false; tab.webView.clearMatches() } else b.back()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        AddressBar(act, tab)
        if (tab.progress < 100) LinearProgressIndicator(progress = { tab.progress / 100f }, Modifier.fillMaxWidth().height(2.dp), color = Purple)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (tab.isHome) HomeContent(act)
            else androidx.compose.runtime.key(tab) { WebViewHost(tab) }
        }
        if (finding) FindBar(tab) { finding = false }
        BottomBar(
            onFlag = { act.go(Screen.Proxies) },
            onTrash = { clearAsk = true },
            onHome = { b.goHome() },
            tabCount = b.tabs.size,
            onTabs = { tabsSheet = true },
            onMenu = { menu = true },
            historyDot = Prefs.history.isNotEmpty(),
        )
    }

    if (clearAsk) AlertDialog(
        onDismissRequest = { clearAsk = false },
        title = { Text("Clear history") },
        text = { Text("Would you like to clear all browser history and cookies?") },
        confirmButton = { TextButton({ act.clearBrowsingData(); clearAsk = false; toast(act, "History cleared") }) { Text("YES") } },
        dismissButton = { TextButton({ clearAsk = false }) { Text("NO") } },
    )

    if (menu) ModalBottomSheet(onDismissRequest = { menu = false }) {
        MenuGrid(act, tab, close = { menu = false }, find = { finding = true })
    }

    if (tabsSheet) ModalBottomSheet(onDismissRequest = { tabsSheet = false }) {
        TabsList(b) { tabsSheet = false }
    }

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

@Composable
private fun AddressBar(act: MainActivity, tab: Tab) {
    val focus = LocalFocusManager.current
    var editing by remember { mutableStateOf(false) }
    var text by remember(tab, tab.url, editing) { mutableStateOf(tab.url) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            placeholder = { Text("Search or enter the address", fontSize = 14.sp) },
            leadingIcon = {
                if (!tab.isHome && tab.url.startsWith("https")) Icon(Icons.Default.Lock, null, tint = GoodGreen, modifier = Modifier.size(18.dp))
                else Text("G", fontWeight = FontWeight.Black, color = Indigo)
            },
            trailingIcon = {
                if (editing || tab.isHome) IconButton({ text = "" }) { Icon(Icons.Default.Close, null) }
                else IconButton({ tab.webView.reload() }) { Icon(Icons.Default.Refresh, null) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                act.browser.open(text); focus.clearFocus()
                Ads.maybeInterstitial(act)
            }),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).height(52.dp).onFocusChanged { editing = it.isFocused },
        )
    }
}

@Composable
private fun HomeContent(act: MainActivity) {
    val cfg = RemoteConfig.config.value ?: return
    val proxy = ProxyManager.connected.value
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (proxy != null) Color(0xFFE9FBF5) else Color(0xFFFFF1F4))
                .clickable { act.go(Screen.Proxies) }.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (proxy != null) "● Proxy ON · ${proxy.flag} ${proxy.name}" else "● Proxy OFF · tap to connect",
                color = if (proxy != null) Color(0xFF0A7A5C) else Pink, fontSize = 13.sp, modifier = Modifier.weight(1f),
            )
            Text(if (proxy != null) "Change" else "Connect", fontWeight = FontWeight.Bold, color = if (proxy != null) Color(0xFF0A7A5C) else Pink, fontSize = 13.sp)
        }
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(cfg.quickSites) { _, s ->
                Column(Modifier.clickable { act.browser.open(s.url); Ads.maybeInterstitial(act) }.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(parseColor(s.color)), contentAlignment = Alignment.Center) {
                        if (s.icon.isNotBlank()) AsyncImage(s.icon, s.name, Modifier.size(48.dp))
                        else Text(s.letter, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Text(s.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Ads.Banner(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun BottomBar(
    onFlag: () -> Unit, onTrash: () -> Unit, onHome: () -> Unit, tabCount: Int, onTabs: () -> Unit, onMenu: () -> Unit, historyDot: Boolean,
) {
    Surface(tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().height(54.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(onClick = onFlag), contentAlignment = Alignment.Center) {
                Text(ProxyManager.connected.value?.flag ?: "🌐", fontSize = 24.sp)
            }
            Box {
                IconButton(onTrash) { Icon(Icons.Default.Delete, "Clear history") }
                if (historyDot) Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(8.dp).clip(CircleShape).background(Pink))
            }
            IconButton(onHome) { Icon(Icons.Default.Home, "Home") }
            Box(Modifier.size(44.dp).clickable(onClick = onTabs), contentAlignment = Alignment.Center) {
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Text("$tabCount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onMenu) { Icon(Icons.Default.Menu, "Menu") }
        }
    }
}

private data class MenuItem(val label: String, val icon: ImageVector, val active: Boolean = false, val action: () -> Unit)

@Composable
private fun MenuGrid(act: MainActivity, tab: Tab, close: () -> Unit, find: () -> Unit) {
    val b = act.browser
    val bookmarked = Prefs.bookmarks.any { it.url == tab.url }
    val items = listOf(
        MenuItem("Share", Icons.Default.Share) {
            if (!tab.isHome) act.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, tab.url), "Share"))
        },
        MenuItem("History", Icons.Default.History) { act.go(Screen.History) },
        MenuItem("Downloads", Icons.Default.Download) {
            act.startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        MenuItem("Incognito", Icons.Default.VisibilityOff, Prefs.incognito.value) {
            Prefs.incognito.value = !Prefs.incognito.value
            toast(act, if (Prefs.incognito.value) "Incognito on: history won't be saved" else "Incognito off")
        },
        MenuItem("Desktop site", Icons.Default.Computer, Prefs.desktopMode.value) {
            Prefs.set("desktop", Prefs.desktopMode, !Prefs.desktopMode.value); b.applySettings(); b.reloadAll()
        },
        MenuItem("Find in page", Icons.Default.Search) { if (!tab.isHome) find() },
        MenuItem("Forward", Icons.AutoMirrored.Filled.ArrowForward) { if (tab.webView.canGoForward()) tab.webView.goForward() },
        MenuItem("Settings", Icons.Default.Settings) { act.go(Screen.Settings) },
        MenuItem(if (bookmarked) "Bookmarked" else "Bookmark", if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, bookmarked) {
            if (!tab.isHome) Prefs.toggleBookmark(tab.title, tab.url) else act.go(Screen.Bookmarks)
        },
        MenuItem("Ad block", Icons.Default.Block, Prefs.adBlock.value) {
            Prefs.set("adblock", Prefs.adBlock, !Prefs.adBlock.value)
            toast(act, if (Prefs.adBlock.value) "Ad block on" else "Ad block off")
        },
        MenuItem("Clear data", Icons.Default.CleaningServices) { act.clearBrowsingData(); toast(act, "Browsing data cleared") },
        MenuItem("Rate us", Icons.Default.Star) { openStore(act, act.packageName) },
    )
    LazyVerticalGrid(GridCells.Fixed(4), Modifier.padding(horizontal = 8.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(items) { _, it ->
            Column(Modifier.clickable { close(); it.action() }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (it.active) Indigo else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Icon(it.icon, null, tint = if (it.active) Color.White else MaterialTheme.colorScheme.onSurface) }
                Text(it.label, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun TabsList(b: BrowserState, close: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tabs (${b.tabs.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            TextButton({ b.newTab(); close() }) { Icon(Icons.Default.Add, null); Text("New tab") }
        }
        LazyColumn {
            itemsIndexed(b.tabs) { i, t ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (i == b.currentIndex) Indigo.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { b.currentIndex = i; close() }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (t.isHome) "Home" else t.title.ifBlank { t.url }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        if (!t.isHome) Text(t.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton({ b.closeTab(i) }) { Icon(Icons.Default.Close, "Close tab") }
                }
            }
        }
    }
}

@Composable
private fun FindBar(tab: Tab, close: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    LaunchedEffect(tab) {
        tab.webView.setFindListener { active, total, done -> if (done) result = if (total == 0) "0/0" else "${active + 1}/$total" }
    }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            q, { q = it; tab.webView.findAllAsync(it) }, singleLine = true, placeholder = { Text("Find in page") },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
            modifier = Modifier.weight(1f),
        )
        Text(result, fontSize = 12.sp)
        IconButton({ tab.webView.findNext(false) }) { Icon(Icons.Default.KeyboardArrowUp, null) }
        IconButton({ tab.webView.findNext(true) }) { Icon(Icons.Default.KeyboardArrowDown, null) }
        IconButton({ tab.webView.clearMatches(); close() }) { Icon(Icons.Default.Close, null) }
    }
}

@Composable
private fun ConnectedDialog(act: MainActivity, s: ProxyServer) {
    androidx.compose.ui.window.Dialog(onDismissRequest = { act.connectedPopup = null }) {
        Box(contentAlignment = Alignment.TopCenter) {
            Surface(Modifier.padding(top = 30.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 42.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Proxy connected", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("${s.flag} ${s.name}", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    Text("Your browsing now goes through the proxy", color = Color(0xFF0A7A5C), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    GradientButton("Start browsing") { act.connectedPopup = null; Ads.maybeInterstitial(act) }
                    Spacer(Modifier.height(10.dp))
                    Ads.Banner(mrec = true)
                }
            }
            Box(Modifier.size(64.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(ButtonGradient).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
        actions()
        Spacer(Modifier.width(4.dp))
    }
}

fun parseColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Indigo)

fun toast(act: android.content.Context, msg: String) = android.widget.Toast.makeText(act, msg, android.widget.Toast.LENGTH_SHORT).show()

fun openStore(act: android.content.Context, pkg: String) {
    val market = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { act.startActivity(market) }.onFailure {
        act.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
