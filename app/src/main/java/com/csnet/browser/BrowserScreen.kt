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
fun BrowserScreen(onLoadUrl: (WebView?, String) -> Unit) {
    // State variables remain the same
    var url by rememberSaveable { mutableStateOf("") }
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
    var activeTabId by rememberSaveable {
        mutableStateOf(tabs.first().id)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showClearDataDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val imeInsets = WindowInsets.ime.asPaddingValues()
    var isWebViewTouched by remember { mutableStateOf(false) }


    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isWebViewTouched, // Disable drawer gestures when WebView is being touched
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
            // Main content
            if (isWebViewVisible) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showWebView) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
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
                                            isLoadingCsNet = false  // Add this line to ensure CsNet loading stops

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
                                            MotionEvent.ACTION_DOWN -> {
                                                isWebViewTouched = true
                                            }
                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                isWebViewTouched = false
                                            }
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

                    if (isLoadingCsNet) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Loading CsNet results...")
                            }
                        }
                    }
                }

            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = systemBarsPadding.calculateBottomPadding() + 80.dp)
                        .offset(x = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CsNetLogo(
                        modifier = Modifier.size(200.dp),  // Increased from 120.dp to 200.dp
                        size = 400f  // Increased from 120f to 200f
                    )
                }
            }

// Bottom navigation
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(0.5f)  // Changed from 1f to 0.5f to make it take up less space
                                    .padding(horizontal = 60.dp),  // Reduced horizontal padding
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { isSearchMode = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { isTabsOverviewVisible = true }) {
                                Icon(
                                    Icons.Outlined.Layers,
                                    contentDescription = "Tabs",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        // Add spacer to account for navigation bar height
                        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                    }
                }
            }

            // Rest of the overlays remain the same
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

// In BrowserScreen.kt, update the SearchScreen section:
            if (isSearchMode) {
                SearchScreen(
                    onDismiss = { isSearchMode = false },
                    onSearch = { query, isGoogleSearch ->
                        if (isGoogleSearch) {
                            // Handle Google search
                            scope.launch {
                                // Initialize first
                                isWebViewVisible = true
                                showWebView = true

                                // Wait for WebView to be ready
                                kotlinx.coroutines.delay(100)

                                // Create new tab
                                val newTabId = UUID.randomUUID().toString()
                                val newTab = TabInfo(
                                    id = newTabId,
                                    title = query,
                                    url = "https://www.google.com/search?q=$query",
                                    isActive = true
                                )
                                tabs = tabs.map { it.copy(isActive = false) } + newTab
                                activeTabId = newTabId

                                // Load the URL - no loading screen for Google search
                                onLoadUrl(webView, "https://www.google.com/search?q=$query")

                                isSearchMode = false
                            }
                        } else {
                            // Handle CsNet search
                            scope.launch {
                                val csNetUrl = "csnet:$query"

                                // Initialize first
                                isWebViewVisible = true
                                showWebView = true

                                // Wait for WebView to be ready
                                kotlinx.coroutines.delay(100)

                                // Create new tab
                                val newTabId = UUID.randomUUID().toString()
                                val newTab = TabInfo(
                                    id = newTabId,
                                    title = "CsNet: $query",
                                    url = csNetUrl,
                                    isActive = true
                                )

                                // Update tabs
                                tabs = tabs.map { it.copy(isActive = false) } + newTab
                                activeTabId = newTabId

                                // Show loading indicator only for CsNet search
                                isLoadingCsNet = true

                                // Load the URL
                                onLoadUrl(webView, csNetUrl)

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
                                    // Restore CsNet search content
                                    webView?.loadDataWithBaseURL(
                                        null,
                                        tab.csNetContent,
                                        "text/html",
                                        "UTF-8",
                                        tab.url
                                    )
                                    Unit
                                }
                                tab.url.isNotEmpty() -> {
                                    // Load regular URL
                                    onLoadUrl(webView, tab.url)
                                    Unit
                                }
                                else -> Unit
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