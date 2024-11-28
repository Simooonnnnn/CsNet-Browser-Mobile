package com.csnet.browser

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.csnet.browser.ui.theme.CsNetBrowserTheme

class MainActivity : ComponentActivity() {
    private val csNetSearch = CsNetSearch()
    private var isCustomSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkTheme = isSystemInDarkTheme()

            CsNetBrowserTheme(
                darkTheme = isDarkTheme,
                dynamicColor = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(
                        onLoadUrl = { webView: WebView?, url: String ->
                            loadUrl(webView, url)
                        }
                    )
                }
            }
        }
    }

    private fun loadUrl(webView: WebView?, input: String) {
        if (input.isBlank()) return

        lifecycleScope.launch {
            if (input.startsWith("csnet:") || input.startsWith("csnet/")) {
                isCustomSearch = true
                val query = input.substringAfter(":").substringAfter("/").trim()

                // Perform the search
                val searchResults = csNetSearch.performSearch(query)

                // Load the results
                webView?.loadDataWithBaseURL(
                    input,
                    searchResults,
                    "text/html",
                    "UTF-8",
                    null
                )

                // The WebViewClient in BrowserScreen will handle hiding the loading indicator
                // when the page finishes loading
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
    }}