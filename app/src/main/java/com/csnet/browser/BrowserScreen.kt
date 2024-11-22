package com.csnet.browser

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.csnet.browser.*
import kotlinx.coroutines.launch


@Composable
fun BrowserScreen(onLoadUrl: (WebView?, String) -> Unit) {
    // All state variables
    var url by rememberSaveable { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    var isWebViewVisible by remember { mutableStateOf(false) }
    var tabs by rememberSaveable {
        mutableStateOf(listOf(TabInfo(
            id = "1",
            title = "New Tab",
            url = "",
            isActive = true
        )))
    }
    var isTabsOverviewVisible by remember { mutableStateOf(false) }
    var activeTabId by rememberSaveable { mutableStateOf("1") }

    // Drawer state variables
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showClearDataDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideMenu(
                onDismiss = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onClearData = {
                    showClearDataDialog = true
                    scope.launch {
                        drawerState.close()
                    }
                },
                onSettings = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isWebViewVisible) {
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
                                        // Update tab info when page loads
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
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CsNet Browser",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Bottom navigation bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 95.dp),
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

                        IconButton(
                            onClick = { isTabsOverviewVisible = true }
                        ) {
                            Icon(
                                Icons.Default.ViewList,
                                contentDescription = "Tabs",
                                tint = MaterialTheme.colorScheme.onSurface
                            )                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            if (isSearchMode) {
                SearchScreen(
                    onDismiss = { isSearchMode = false },
                    onSearch = { query: String, isGoogleSearch: Boolean ->  // explicitly specify types
                        if (isGoogleSearch) {
                            onLoadUrl(webView, "https://www.google.com/search?q=$query")
                        } else {
                            onLoadUrl(webView, "csnet:$query")
                        }
                        isSearchMode = false
                        isWebViewVisible = true
                    }
                )
            }

            if (isTabsOverviewVisible) {
                TabsOverviewScreen(
                    tabs = tabs,
                    onTabClose = { tabId ->
                        if (tabs.size > 1) {
                            tabs = tabs.filter { it.id != tabId }
                            if (tabId == activeTabId) {
                                activeTabId = tabs.first().id
                            }
                        }
                    },
                    onTabSelect = { tabId ->
                        activeTabId = tabId
                        tabs = tabs.map { it.copy(isActive = it.id == tabId) }
                        isTabsOverviewVisible = false
                    },
                    onDismiss = { isTabsOverviewVisible = false }
                )
            }
        }

        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear Browsing Data") },
                text = { Text("Are you sure you want to clear all browsing data?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDataDialog = false
                            // Implement clear data functionality
                        }
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}