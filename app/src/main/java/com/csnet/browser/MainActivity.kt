package com.csnet.browser

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import android.graphics.Bitmap
import android.view.ViewGroup.LayoutParams
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.lifecycleScope
import com.csnet.browser.ui.theme.CsNetBrowserTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val csNetSearch = CsNetSearch()
    private var isCustomSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("MainActivity", "onCreate called")
        setContent {
            CsNetBrowserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(
                        onLoadUrl = { webView, input -> loadUrl(webView, input) }
                    )
                }
            }
        }
    }

    fun loadUrl(webView: WebView?, input: String) {
        if (input.isBlank()) return

        lifecycleScope.launch {
            if (input.startsWith("csnet:") || input.startsWith("csnet/")) {
                isCustomSearch = true
                val query = input.substringAfter(":").substringAfter("/").trim()
                val searchResults = csNetSearch.performSearch(query)
                webView?.loadDataWithBaseURL(null, searchResults, "text/html", "UTF-8", null)
            } else {
                isCustomSearch = false
                val url = when {
                    input.startsWith("http://") || input.startsWith("https://") -> input
                    input.contains(".") -> "https://$input"
                    else -> "https://www.google.com/search?q=${android.net.Uri.encode(input)}"
                }
                webView?.loadUrl(url)
            }
        }
    }
}

@Composable
fun BrowserScreen(onLoadUrl: (WebView?, String) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isWebViewVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Navigation bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { webView?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { webView?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Forward",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Search or enter URL") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    onLoadUrl(webView, url)
                                    focusManager.clearFocus()
                                    isWebViewVisible = true
                                }
                            )
                        )

                        IconButton(onClick = {
                            webView?.reload()
                            isWebViewVisible = true
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            onLoadUrl(webView, "csnet:$url")
                            isWebViewVisible = true
                        }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "CsNet Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Show welcome message when WebView is not visible
            if (!isWebViewVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "Welcome to CsNet Browser\nEnter a URL or search term above",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // WebView
            if (isWebViewVisible) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT
                            )
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    request?.url?.toString()?.let { urlString ->
                                        url = urlString
                                        view?.loadUrl(urlString)
                                    }
                                    return true
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                }
                            }
                            webChromeClient = WebChromeClient()
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
            }
        }
    }
}