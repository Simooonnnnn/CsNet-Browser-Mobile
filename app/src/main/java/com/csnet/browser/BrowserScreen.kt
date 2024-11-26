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

@Composable
fun BrowserScreen(onLoadUrl: (WebView?, String) -> Unit) {
    // State variables remain the same
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

            if (isSearchMode) {
                SearchScreen(
                    onDismiss = { isSearchMode = false },
                    onSearch = { query, isGoogleSearch ->
                        isWebViewVisible = true  // Make WebView visible before loading URL
                        isSearchMode = false     // Hide search screen
                        // Slight delay to ensure WebView is ready
                        scope.launch {
                            kotlinx.coroutines.delay(100)  // Short delay
                            if (isGoogleSearch) {
                                onLoadUrl(webView, "https://www.google.com/search?q=$query")
                            } else {
                                onLoadUrl(webView, "csnet:$query")
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
}