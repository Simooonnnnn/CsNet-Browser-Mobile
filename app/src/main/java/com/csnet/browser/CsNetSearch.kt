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

    private val stopWords = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have",
        "i", "it", "for", "not", "on", "with", "he", "as", "you",
        "do", "at", "this", "but", "his", "by", "from", "they",
        "we", "say", "her", "she", "or", "an", "will", "my",
        "one", "all", "would", "there", "their", "what", "about"
    )

    suspend fun performSearch(query: String): String = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "https://duckduckgo.com/html/?q=${android.net.Uri.encode(query)}"
            val searchResults = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val links = searchResults.select("div.result__body a.result__url")
                .map { "https://${it.text().trim()}" }
                .filter { !containsUnwantedDomain(it) }
                .take(5)

            // Improve image extraction
            val images = searchResults.select("div.result__body")
                .flatMap { result ->
                    // Try to get images from both result snippets and linked pages
                    val directImages = result.select("img").map { it.attr("src") }
                    val linkUrl = result.select("a.result__url").firstOrNull()?.text()?.let { "https://$it" }
                    val linkedPageImages = linkUrl?.let {
                        try {
                            Jsoup.connect(it).get().select("img[src~=(?i)\\.(png|jpe?g)]")
                                .map { img -> img.attr("abs:src") }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList()

                    directImages + linkedPageImages
                }
                .filter { it.startsWith("http") && it.matches(Regex(".+\\.(png|jpe?g)$", RegexOption.IGNORE_CASE)) }
                .distinct()
                .take(3)
                .ifEmpty {
                    listOf(
                        "https://via.placeholder.com/300x200?text=No+Image",
                        "https://via.placeholder.com/300x200?text=No+Image",
                        "https://via.placeholder.com/300x200?text=No+Image"
                    )
                }

            // Collect all content for unified analysis
            val allContent = mutableListOf<String>()
            val sources = mutableListOf<String>()

            for (link in links) {
                try {
                    val doc = Jsoup.connect(link).get()
                    doc.select("script, style, nav, header, footer, iframe").remove()

                    val paragraphs = doc.select("article p, main p, .content p")
                        .map { it.text().trim() }
                        .filter { it.length in 50..500 && isRelevantToQuery(it, query) }
                        .take(2)  // Take fewer paragraphs per source for unified summary

                    if (paragraphs.isNotEmpty()) {
                        allContent.addAll(paragraphs)
                        sources.add(URI(link).host)
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            // Unified analysis
            val unifiedKeywords = extractKeywords(allContent.joinToString(" "))
            val unifiedBulletPoints = extractBulletPoints(allContent.joinToString(" "))

            generateUnifiedResultsHtml(query, images, UnifiedAnalysis(
                sources = sources,
                content = allContent,
                keywords = unifiedKeywords,
                bulletPoints = unifiedBulletPoints
            ))
        } catch (e: Exception) {
            generateErrorHtml("Search failed: ${e.message}")
        }
    }

    private data class UnifiedAnalysis(
        val sources: List<String>,
        val content: List<String>,
        val keywords: List<String>,
        val bulletPoints: List<String>
    )

    private fun extractKeywords(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[\\s.,;:!?()]+"))
            .filter { it.length > 3 && !stopWords.contains(it) }
            .groupBy { it }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(8)
    }

    private fun extractBulletPoints(text: String): List<String> {
        return text.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.length in 20..150 }
            .distinctBy { it.lowercase() }  // Remove duplicate points
            .sortedByDescending { it.length }  // Prioritize more detailed points
            .take(5)
    }

    private fun generateUnifiedResultsHtml(
        query: String,
        images: List<String>,
        analysis: UnifiedAnalysis
    ): String {
        val imagesHtml = images.joinToString("\n") { imageUrl ->
            """
            <div class="image-card">
                <img src="$imageUrl" alt="Search result" class="result-image" onerror="this.src='https://via.placeholder.com/300x200?text=Image+Not+Found';">
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
                    padding: 8px 0;
                    -webkit-overflow-scrolling: touch;
                    scrollbar-width: none;
                }
                .images-container::-webkit-scrollbar {
                    display: none;
                }
                .image-card {
                    flex: 0 0 auto;
                    background: white;
                    border-radius: 8px;
                    overflow: hidden;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .result-image {
                    width: 280px;
                    height: 180px;
                    object-fit: cover;
                }
                .result-card {
                    background: white;
                    padding: 20px;
                    margin-bottom: 16px;
                    border-radius: 12px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                }
                .sources {
                    color: #1a73e8;
                    font-size: 14px;
                    margin-bottom: 16px;
                    padding-bottom: 12px;
                    border-bottom: 1px solid #e8eaed;
                }
                .keywords {
                    margin: 16px 0;
                }
                .keyword-tags {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 8px;
                    margin-top: 8px;
                }
                .keyword {
                    background: #f0f0f0;
                    color: #202124;
                    padding: 6px 12px;
                    border-radius: 16px;
                    font-size: 14px;
                    border: 1px solid #e0e0e0;
                }
                .key-points {
                    margin: 16px 0;
                }
                .key-points ul {
                    margin: 8px 0;
                    padding-left: 24px;
                }
                .key-points li {
                    margin: 8px 0;
                    color: #202124;
                }
                .content {
                    color: #202124;
                }
                h4 {
                    color: #202124;
                    font-size: 16px;
                    margin: 12px 0;
                    font-weight: 500;
                }
                p {
                    margin: 12px 0;
                    line-height: 1.7;
                }
            </style>
        </head>
        <body>
            <h1>$query</h1>
            <div class="images-container">
                $imagesHtml
            </div>
            <div class="result-card">
                <div class="sources">
                    Sources: ${analysis.sources.joinToString(", ")}
                </div>
                <div class="keywords">
                    <h4>Keywords:</h4>
                    <div class="keyword-tags">
                        ${analysis.keywords.joinToString("") { "<span class='keyword'>$it</span>" }}
                    </div>
                </div>
                <div class="key-points">
                    <h4>Key Points:</h4>
                    <ul>
                        ${analysis.bulletPoints.joinToString("\n") { "<li>$it</li>" }}
                    </ul>
                </div>
                <div class="content">
                    <h4>Detailed Information:</h4>
                    ${analysis.content.joinToString("\n") { "<p>$it</p>" }}
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    // ... (keeping the existing helper functions)
    private fun containsUnwantedDomain(url: String): Boolean {
        return unwantedDomains.any { url.contains(it) }
    }

    private fun isRelevantToQuery(text: String, query: String): Boolean {
        val queryWords = query.lowercase().split(" ", ",", ".")
            .filter { it.length > 2 }
        return queryWords.any { text.lowercase().contains(it) }
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