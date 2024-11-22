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

            // Extract relevant links and images
            val links = searchResults.select("div.result__body a.result__url")
                .map { "https://${it.text().trim()}" }
                .filter { !containsUnwantedDomain(it) }
                .take(5)

            // Extract images (fallback to placeholder if no images found)
            val images = searchResults.select("div.result__body img")
                .map { it.attr("src") }
                .filter { it.startsWith("http") }
                .take(3)
                .ifEmpty {
                    listOf(
                        "https://via.placeholder.com/300x200?text=No+Image",
                        "https://via.placeholder.com/300x200?text=No+Image",
                        "https://via.placeholder.com/300x200?text=No+Image"
                    )
                }

            // Analyze content from each link
            val contentPieces = mutableListOf<Pair<String, String>>()
            for (link in links) {
                try {
                    val doc = Jsoup.connect(link).get()
                    doc.select("script, style, nav, header, footer, iframe").remove()

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

            generateSearchResultsHtml(query, images, contentPieces)
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

    private fun generateSearchResultsHtml(
        query: String,
        images: List<String>,
        results: List<Pair<String, String>>
    ): String {
        val imagesHtml = images.joinToString("\n") { imageUrl ->
            """
            <div class="image-card">
                <img src="$imageUrl" alt="Search result" class="result-image">
            </div>
            """.trimIndent()
        }

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
                    margin: 0;
                    background: #f8f9fa;
                }
                h1 {
                    color: #202124;
                    font-size: 24px;
                    margin-bottom: 16px;
                }
                .images-container {
                    display: flex;
                    gap: 8px;
                    overflow-x: auto;
                    margin-bottom: 24px;
                    padding-bottom: 8px;
                    -webkit-overflow-scrolling: touch;
                }
                .image-card {
                    flex: 0 0 auto;
                    background: white;
                    border-radius: 8px;
                    overflow: hidden;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .result-image {
                    width: 200px;
                    height: 150px;
                    object-fit: cover;
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
            </style>
        </head>
        <body>
            <h1>$query</h1>
            <div class="images-container">
                $imagesHtml
            </div>
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