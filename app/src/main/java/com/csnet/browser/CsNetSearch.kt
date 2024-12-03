package com.csnet.browser

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CsNetSearch(private val context: Context) {
    private fun colorToRgb(color: Int): String {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        return "rgb($red, $green, $blue)"
    }
    private data class UnifiedAnalysis(
        val sources: List<String>,
        val content: List<String>,
        val keywords: List<String>,
        val bulletPoints: List<String>
    )

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

    private fun containsUnwantedDomain(url: String): Boolean {
        return unwantedDomains.any { url.contains(it) }
    }

    private fun isRelevantToQuery(text: String, query: String): Boolean {
        val queryWords = query.lowercase().split(" ", ",", ".")
            .filter { it.length > 2 }
        return queryWords.any { text.lowercase().contains(it) }
    }

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
            .distinctBy { it.lowercase() }
            .sortedByDescending { it.length }
            .take(5)
    }

    suspend fun performSearch(query: String, colorScheme: ColorScheme): String = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "https://duckduckgo.com/html/?q=${android.net.Uri.encode(query)}"
            val searchResults = Jsoup.connect(searchUrl)
                .timeout(5000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val links = searchResults.select("div.result__body a.result__url")
                .map { "https://${it.text().trim()}" }
                .filter { !containsUnwantedDomain(it) }
                .take(5)

            val images = searchResults.select("div.result__body")
                .flatMap { result ->
                    val url = result.select("a.result__url").firstOrNull()?.text()?.let { "https://$it" }
                    url?.let {
                        try {
                            Jsoup.connect(it)
                                .timeout(5000)
                                .get()
                                .select("img[src~=(?i)\\.(png|jpe?g)]")
                                .map { img -> img.absUrl("src") }
                        } catch (e: Exception) { null }
                    } ?: emptyList()
                }
                .filter { it.isNotEmpty() && it.startsWith("http") }
                .distinct()
                .take(3)
                .ifEmpty {
                    listOf(
                        "https://via.placeholder.com/300x200?text=No+Image",
                        "https://via.placeholder.com/300x200?text=No+Image",
                        "https://via.placeholder.com/300x200?text=No+Image"
                    )
                }

            coroutineScope {
                val contentResults = links.map { link ->
                    async {
                        try {
                            val doc = Jsoup.connect(link)
                                .timeout(5000)
                                .get()
                            doc.select("script, style, nav, header, footer, iframe").remove()

                            val paragraphs = doc.select("article p, main p, .content p")
                                .map { it.text().trim() }
                                .filter { it.length in 50..500 && isRelevantToQuery(it, query) }
                                .take(2)

                            if (paragraphs.isNotEmpty()) {
                                Pair(paragraphs, URI(link).host)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                val allContent = contentResults.flatMap { it.first }
                val sources = contentResults.map { it.second }

                val unifiedKeywords = extractKeywords(allContent.joinToString(" "))
                val unifiedBulletPoints = extractBulletPoints(allContent.joinToString(" "))

                generateUnifiedResultsHtml(
                    query,
                    images,
                    UnifiedAnalysis(
                        sources = sources,
                        content = allContent,
                        keywords = unifiedKeywords,
                        bulletPoints = unifiedBulletPoints
                    ),
                    colorScheme
                )
            }
        } catch (e: Exception) {
            generateErrorHtml("Search failed: ${e.message}", colorScheme)
        }
    }

    private fun generateUnifiedResultsHtml(
        query: String,
        images: List<String>,
        analysis: UnifiedAnalysis,
        colorScheme: ColorScheme
    ): String {
        val primary = colorScheme.primary.toArgb()
        val onPrimary = colorScheme.onPrimary.toArgb()
        val primaryContainer = colorScheme.primaryContainer.toArgb()
        val onPrimaryContainer = colorScheme.onPrimaryContainer.toArgb()
        val surface = colorScheme.surface.toArgb()
        val surfaceContainer = ColorUtils.blendARGB(colorScheme.surface.toArgb(), colorScheme.primary.toArgb(), 0.05f)
        val onSurface = colorScheme.onSurface.toArgb()
        val onSurfaceVariant = colorScheme.onSurfaceVariant.toArgb()
        val outline = colorScheme.outline.toArgb()
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
            <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&display=swap" rel="stylesheet">
            <style>
:root {
    --md-sys-color-primary: ${colorToRgb(primary)};
    --md-sys-color-on-primary: ${colorToRgb(onPrimary)};
    --md-sys-color-primary-container: ${colorToRgb(primaryContainer)};
    --md-sys-color-on-primary-container: ${colorToRgb(onPrimaryContainer)};
    --md-sys-color-surface: ${colorToRgb(surface)};
    --md-sys-color-surface-container: ${colorToRgb(surfaceContainer)};
    --md-sys-color-on-surface: ${colorToRgb(onSurface)};
    --md-sys-color-on-surface-variant: ${colorToRgb(onSurfaceVariant)};
    --md-sys-color-outline: ${colorToRgb(outline)};
}
                body {
                    font-family: 'Roboto', system-ui, -apple-system, sans-serif;
                    line-height: 1.6;
                    margin: 0;
                    padding: 16px;
                    background: var(--md-sys-color-surface);
                    color: var(--md-sys-color-on-surface);
                }

                h1 {
                    font-size: 28px;
                    font-weight: 400;
                    color: var(--md-sys-color-on-surface);
                    margin: 24px 0;
                    padding: 0 16px;
                }

.images-container {
                    display: flex;
                    gap: 16px;
                    overflow-x: auto;
                    margin: 0 0 24px 0;
                    padding: 0 16px;
                    scroll-snap-type: x mandatory;
                    -webkit-overflow-scrolling: touch;
                    scrollbar-width: none;  /* Firefox */
                    -ms-overflow-style: none;  /* IE and Edge */
                }

                .images-container::-webkit-scrollbar {
                    display: none;  /* Chrome, Safari and Opera */
                }

                .image-card {
                    flex: 0 0 auto;
                    scroll-snap-align: start;
                    background: var(--md-sys-color-surface-container);
                    border-radius: 16px;
                    overflow: hidden;
                    box-shadow: var(--md-sys-elevation-2);
                    transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
                    line-height: 0;  /* Remove the white space below images */
                }

                .result-image {
                    width: 300px;
                    height: 200px;
                    object-fit: cover;
                    display: block;  /* Remove any inline spacing */
                }
                
                .result-card {
                    background: var(--md-sys-color-surface-container);
                    padding: 24px;
                    margin: 16px 0;
                    border-radius: 28px;
                    box-shadow: var(--md-sys-elevation-1);
                }

                .sources {
                    color: var(--md-sys-color-primary);
                    font-size: 14px;
                    margin-bottom: 20px;
                    padding-bottom: 16px;
                    border-bottom: 1px solid var(--md-sys-color-outline);
                }

                .keywords {
                    margin: 24px 0;
                }

                .keyword-tags {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 8px;
                    margin-top: 12px;
                }

                .keyword {
                    background: var(--md-sys-color-primary-container);
                    color: var(--md-sys-color-on-primary-container);
                    padding: 8px 16px;
                    border-radius: 20px;
                    font-size: 14px;
                    font-weight: 500;
                    transition: all 0.2s ease;
                }

                .keyword:hover {
                    background: var(--md-sys-color-primary);
                    color: var(--md-sys-color-on-primary);
                    transform: translateY(-1px);
                }

                .key-points {
                    background: var(--md-sys-color-surface);
                    padding: 20px;
                    border-radius: 16px;
                    margin: 24px 0;
                }

                .key-points ul {
                    list-style: none;
                    padding: 0;
                    margin: 16px 0;
                }

                .key-points li {
                    position: relative;
                    padding-left: 28px;
                    margin: 16px 0;
                    color: var(--md-sys-color-on-surface);
                }

                .key-points li::before {
                    content: "";
                    position: absolute;
                    left: 0;
                    top: 8px;
                    width: 8px;
                    height: 8px;
                    background: var(--md-sys-color-primary);
                    border-radius: 50%;
                }

                .content {
                    color: var(--md-sys-color-on-surface);
                }

                h4 {
                    color: var(--md-sys-color-on-surface);
                    font-size: 20px;
                    margin: 16px 0;
                    font-weight: 500;
                }

                p {
                    margin: 16px 0;
                    line-height: 1.7;
                    color: var(--md-sys-color-on-surface-variant);
                }

                @media (prefers-color-scheme: dark) {
                    :root {
                        --md-sys-color-primary: rgb(208, 188, 255);
                        --md-sys-color-on-primary: rgb(56, 30, 114);
                        --md-sys-color-primary-container: rgb(79, 55, 139);
                        --md-sys-color-on-primary-container: rgb(234, 221, 255);
                        --md-sys-color-surface: rgb(28, 27, 31);
                        --md-sys-color-surface-container: rgb(73, 69, 79);
                        --md-sys-color-on-surface: rgb(230, 225, 229);
                        --md-sys-color-on-surface-variant: rgb(202, 196, 208);
                        --md-sys-color-outline: rgb(147, 143, 153);
                    }
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
                    <h4>Keywords</h4>
                    <div class="keyword-tags">
                        ${analysis.keywords.joinToString("") { "<span class='keyword'>$it</span>" }}
                    </div>
                </div>
                <div class="key-points">
                    <h4>Key Points</h4>
                    <ul>
                        ${analysis.bulletPoints.joinToString("\n") { "<li>$it</li>" }}
                    </ul>
                </div>
                <div class="content">
                    <h4>Detailed Information</h4>
                    ${analysis.content.joinToString("\n") { "<p>$it</p>" }}
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun generateErrorHtml(
        message: String,
        colorScheme: ColorScheme
    ): String {
        // Convert the Material You colors to RGB values
        val primary = colorScheme.primary.toArgb()
        val surface = colorScheme.surface.toArgb()
        val onSurface = colorScheme.onSurface.toArgb()
        val surfaceContainer = ColorUtils.blendARGB(colorScheme.surface.toArgb(), colorScheme.primary.toArgb(), 0.05f)

        return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&display=swap" rel="stylesheet">
        <style>
            :root {
                --md-sys-color-primary: ${colorToRgb(primary)};
                --md-sys-color-surface: ${colorToRgb(surface)};
                --md-sys-color-surface-container: ${colorToRgb(surfaceContainer)};
                --md-sys-color-on-surface: ${colorToRgb(onSurface)};
                --md-sys-elevation-2: 0 2px 4px rgba(0,0,0,0.3);
            }
            
            body {
                font-family: 'Roboto', system-ui, -apple-system, sans-serif;
                display: flex;
                justify-content: center;
                align-items: center;
                height: 100vh;
                margin: 0;
                background: var(--md-sys-color-surface);
            }
            
            .error-card {
                background: var(--md-sys-color-surface-container);
                padding: 32px;
                border-radius: 28px;
                box-shadow: var(--md-sys-elevation-2);
                text-align: center;
                max-width: 80%;
            }
            
            h2 {
                color: var(--md-sys-color-primary);
                margin-bottom: 16px;
                font-weight: 500;
            }
            
            p {
                color: var(--md-sys-color-on-surface);
                margin: 0;
                line-height: 1.6;
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