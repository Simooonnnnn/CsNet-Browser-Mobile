package com.csnet.browser

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.csnet.browser.data.SettingsStore
import com.csnet.browser.ui.theme.CsNetBrowserTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val csNetSearch = CsNetSearch()
    private var isCustomSearch = false
    private val settingsStore by lazy { SettingsStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by settingsStore.isDarkMode.collectAsState(initial = false)

            CsNetBrowserTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(onLoadUrl = { webView, url ->
                        loadUrl(webView, url)
                    })
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