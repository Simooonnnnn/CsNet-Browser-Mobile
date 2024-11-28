package com.csnet.browser

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import android.view.MotionEvent
import android.view.ViewConfiguration
import java.util.UUID
import com.csnet.browser.TabInfo

@Composable
fun BrowserScreen(
    onLoadUrl: (WebView?, String, ColorScheme) -> Unit  // Add ColorScheme parameter
) {    // State variables remain unchanged
    var url by rememberSaveable { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    var isWebViewVisible by remember { mutableStateOf(false) }
    var isLoadingCsNet by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var tabs by rememberSaveable {
        mutableStateOf(listOf(TabInfo(
            id = UUID.randomUUID().toString(),
            title = "New Tab",
            url = "",
            isActive = true
        )))
    }
    var isTabsOverviewVisible by remember { mutableStateOf(false) }
    var activeTabId by rememberSaveable { mutableStateOf(tabs.first().id) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showClearDataDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val imeInsets = WindowInsets.ime.asPaddingValues()
    var isWebViewTouched by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isWebViewTouched,
        drawerContent = {
            SideMenu(
                onDismiss = { scope.launch { drawerState.close() } },
                onClearData = {
                    showClearDataDialog = true
                    scope.launch { drawerState.close() }
                },
                onSettings = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = systemBarsPadding.calculateTopPadding())
        ) {
            // Main content with enhanced Material 3 styling
            if (isWebViewVisible) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showWebView) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    // WebView configuration remains unchanged
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            isLoading = true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                            isLoadingCsNet = false

                                            url?.let { currentUrl ->
                                                tabs = tabs.map { tab ->
                                                    if (tab.id == activeTabId) {
                                                        tab.copy(
                                                            url = currentUrl,
                                                            title = view?.title ?: currentUrl
                                                        )
                                                    } else tab
                                                }
                                            }
                                        }
                                    }

                                    setOnTouchListener { _, event ->
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN -> isWebViewTouched = true
                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isWebViewTouched = false
                                        }
                                        false
                                    }

                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                    }
                                    webView = this
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = systemBarsPadding.calculateBottomPadding())
                        )
                    }

                    // Enhanced CsNet loading screen with Material 3 design
                    if (isLoadingCsNet) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(56.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 6.dp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Loading CsNet results...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Summarizing search results",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Enhanced logo display with Material You colors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = systemBarsPadding.calculateBottomPadding() + 80.dp)
                        .offset(x = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CsNetLogo(
                            modifier = Modifier.size(200.dp),
                            size = 400f
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search or enter URL",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Enhanced bottom navigation with Material 3 styling
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp,
                                onClick = { isSearchMode = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isTabsOverviewVisible = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    Icons.Outlined.Layers,
                                    contentDescription = "Tabs",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                    }
                }
            }

            // Enhanced loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Search screen and tabs overview remain functionally the same but inherit Material 3 styling
            if (isSearchMode) {
                SearchScreen(
                    onDismiss = { isSearchMode = false },
                    onSearch = { query, isGoogleSearch ->
                        if (isGoogleSearch) {
                            scope.launch {
                                isWebViewVisible = true
                                showWebView = true
                                kotlinx.coroutines.delay(100)

                                val newTabId = UUID.randomUUID().toString()
                                val newTab = TabInfo(
                                    id = newTabId,
                                    title = query,
                                    url = "https://www.google.com/search?q=$query",
                                    isActive = true
                                )
                                tabs = tabs.map { it.copy(isActive = false) } + newTab
                                activeTabId = newTabId
                                onLoadUrl(webView, "https://www.google.com/search?q=$query", colorScheme)  // For Google searches
                                isSearchMode = false
                            }
                        } else {
                            scope.launch {
                                val csNetUrl = "csnet:$query"
                                isWebViewVisible = true
                                showWebView = true
                                kotlinx.coroutines.delay(100)

                                val newTabId = UUID.randomUUID().toString()
                                val newTab = TabInfo(
                                    id = newTabId,
                                    title = "CsNet: $query",
                                    url = csNetUrl,
                                    isActive = true
                                )
                                tabs = tabs.map { it.copy(isActive = false) } + newTab
                                activeTabId = newTabId
                                isLoadingCsNet = true
                                onLoadUrl(webView, csNetUrl, colorScheme)  // For CsNet searches
                                isSearchMode = false
                            }
                        }
                    }
                )
            }

            if (isTabsOverviewVisible) {
                TabsOverviewScreen(
                    tabs = tabs,
                    onTabClose = { tabId ->
                        if (tabs.size > 1) {
                            val updatedTabs = tabs.filter { it.id != tabId }
                            if (tabId == activeTabId) {
                                val newActiveTab = updatedTabs.last()
                                activeTabId = newActiveTab.id
                                tabs = updatedTabs.map { it.copy(isActive = it.id == newActiveTab.id) }
                            } else {
                                tabs = updatedTabs
                            }
                        }
                    },
                    onTabSelect = { tabId ->
                        activeTabId = tabId
                        tabs = tabs.map { it.copy(isActive = it.id == tabId) }
                        val selectedTab = tabs.find { it.id == tabId }
                        selectedTab?.let { tab ->
                            when {
                                tab.url.startsWith("csnet:") && tab.csNetContent != null -> {
                                    webView?.loadDataWithBaseURL(
                                        null,
                                        tab.csNetContent,
                                        "text/html",
                                        "UTF-8",
                                        tab.url
                                    )
                                }
                                tab.url.isNotEmpty() -> {
                                    onLoadUrl(webView, tab.url, colorScheme)
                                }
                                else -> {
                                    // Handle empty URL case - do nothing as it's a new tab
                                }
                            }
                        }
                        isTabsOverviewVisible = false
                    },
                    onDismiss = { isTabsOverviewVisible = false }
                )
            }
        }
    }
}