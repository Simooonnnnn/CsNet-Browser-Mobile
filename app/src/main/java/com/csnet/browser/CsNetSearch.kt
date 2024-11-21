package com.csnet.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URI

class CsNetSearch {
    private val unwantedDomains = setOf(
        "facebook.com", "instagram.com", "pinterest.com",
        "linkedin.com", "youtube.com", "tumblr.com",
        "microsoft.com/en-us/legal", "privacy", "terms", "cookie"
    )

    suspend fun performSearch(query: String): String = withContext(Dispatchers.IO) {
        try {
            // Get search results from DuckDuckGo
            val searchUrl = "https://duckduckgo.com/html/?q=${android.net.Uri.encode(query)}"
            val searchResults = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            // Extract relevant links
            val links = searchResults.select("div.result__body a.result__url")
                .map { "https://${it.text().trim()}" }
                .filter { !containsUnwantedDomain(it) }
                .take(5)

            // Analyze content from each link
            val contentPieces = mutableListOf<Pair<String, String>>()
            for (link in links) {
                try {
                    val doc = Jsoup.connect(link).get()
                    // Remove unwanted elements
                    doc.select("script, style, nav, header, footer, iframe").remove()

                    // Extract relevant content
                    val content = doc.select("article p, main p, .content p")
                        .map { it.text().trim() }
                        .filter { it.length in 50..500 && isRelevantToQuery(it, query) }
                        .firstOrNull()

                    if (content != null) {
                        contentPieces.add(URI(link).host to content)
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            generateSearchResultsHtml(query, contentPieces)
        } catch (e: Exception) {
            generateErrorHtml("Search failed: ${e.message}")
        }
    }

    private fun containsUnwantedDomain(url: String): Boolean {
        return unwantedDomains.any { url.contains(it) }
    }

    private fun isRelevantToQuery(text: String, query: String): Boolean {
        val queryWords = query.lowercase().split(" ", ",", ".")
            .filter { it.length > 2 }
        return queryWords.any { text.lowercase().contains(it) }
    }

    private fun generateSearchResultsHtml(query: String, results: List<Pair<String, String>>): String {
        val contentHtml = results.joinToString("\n") { (source, content) ->
            """
            <div class="result-card">
                <div class="source">From $source</div>
                <div class="content">$content</div>
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: 'Roboto', sans-serif;
                    line-height: 1.6;
                    padding: 16px;
                    max-width: 800px;
                    margin: 0 auto;
                    background: #f8f9fa;
                }
                .result-card {
                    background: white;
                    padding: 16px;
                    margin-bottom: 16px;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .source {
                    color: #1a73e8;
                    font-size: 14px;
                    margin-bottom: 8px;
                }
                .content {
                    color: #202124;
                }
                h1 {
                    color: #202124;
                    font-size: 24px;
                    margin-bottom: 24px;
                }
            </style>
        </head>
        <body>
            <h1>CsNet Search Results for: $query</h1>
            $contentHtml
        </body>
        </html>
        """.trimIndent()
    }

    private fun generateErrorHtml(message: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: 'Roboto', sans-serif;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                    background: #f8f9fa;
                }
                .error-card {
                    background: white;
                    padding: 24px;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="error-card">
                <h2>Search Error</h2>
                <p>$message</p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}