package vintagebadger.trainingplanner.wiki

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.Duration

class WikiClient(
    cacheDir: Path,
    private val refreshCache: Boolean,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val gson: Gson = Gson(),
) {
    private val pagesDir = cacheDir.resolve("pages")

    fun fetchPage(title: String): WikiPage {
        Files.createDirectories(pagesDir)
        val normalizedRequest = normalizeTitle(title)
        val cacheFile = pagesDir.resolve("${cacheKey(normalizedRequest)}.json")

        if (!refreshCache && Files.exists(cacheFile)) {
            return readCache(cacheFile)
        }

        return try {
            val page = fetchRemote(normalizedRequest)
            writeCache(cacheFile, page)
            if (normalizeIdentity(page.normalizedTitle) != normalizeIdentity(normalizedRequest)) {
                writeCache(pagesDir.resolve("${cacheKey(page.normalizedTitle)}.json"), page)
            }
            page
        } catch (_: Exception) {
            if (Files.exists(cacheFile)) {
                val cached = readCache(cacheFile)
                cached.copy(warnings = cached.warnings + "API_ERROR:$normalizedRequest")
            } else {
                WikiPage(
                    requestedTitle = normalizedRequest,
                    normalizedTitle = normalizedRequest,
                    revisionId = null,
                    fetchedAt = Instant.now().toString(),
                    wikitext = "",
                    redirectTarget = null,
                    warnings = listOf("API_ERROR:$normalizedRequest"),
                )
            }
        }
    }

    private fun fetchRemote(title: String): WikiPage {
        val url = "https://oldschool.runescape.wiki/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "parse")
            .addQueryParameter("page", title)
            .addQueryParameter("prop", "wikitext")
            .addQueryParameter("format", "json")
            .addQueryParameter("redirects", "true")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "runelite-training-planner wiki recipe generator")
            .build()

        val response = httpClient.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) error("OSRS Wiki API returned HTTP ${it.code} for $title: $body")

            val root = gson.fromJson(body, WikiApiParseResponse::class.java)
                ?: error("Unexpected JSON root for $title")
            root.error?.let { apiError -> error("OSRS Wiki API error for $title: ${apiError.code}: ${apiError.info}") }
            val parse = root.parse ?: error("Missing parse object for $title")
            val wikitext = parse.wikitext?.value ?: ""
            val parsedTitle = parse.title ?: title
            val redirectTarget = parse.redirects?.lastOrNull()?.to

            return WikiPage(
                requestedTitle = title,
                normalizedTitle = normalizeTitle(redirectTarget ?: parsedTitle),
                revisionId = parse.revisionId,
                fetchedAt = Instant.now().toString(),
                wikitext = wikitext,
                redirectTarget = redirectTarget,
                warnings = emptyList(),
            )
        }
    }

    private fun readCache(cacheFile: Path): WikiPage {
        val cached = gson.fromJson(Files.readString(cacheFile), CachedWikiPage::class.java)
            ?: error("Invalid cache JSON: $cacheFile")
        return WikiPage(
            requestedTitle = cached.requestedTitle.orEmpty(),
            normalizedTitle = cached.normalizedTitle ?: cached.requestedTitle.orEmpty(),
            revisionId = cached.revisionId,
            fetchedAt = cached.fetchedAt.orEmpty(),
            wikitext = cached.wikitext.orEmpty(),
            redirectTarget = cached.redirectTarget,
            warnings = cached.warnings ?: emptyList(),
        )
    }

    private fun writeCache(cacheFile: Path, page: WikiPage) {
        Files.createDirectories(cacheFile.parent)
        Files.writeString(cacheFile, gson.toJson(page))
    }

    private fun cacheKey(title: String): String {
        return normalizeTitle(title)
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "page" }
    }

    companion object {
        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(30))
                .build()
        }
    }
}

data class WikiPage(
    val requestedTitle: String,
    val normalizedTitle: String,
    val revisionId: Long?,
    val fetchedAt: String,
    val wikitext: String,
    val redirectTarget: String?,
    val warnings: List<String>,
)

private data class CachedWikiPage(
    val requestedTitle: String?,
    val normalizedTitle: String?,
    val revisionId: Long?,
    val fetchedAt: String?,
    val wikitext: String?,
    val redirectTarget: String?,
    val warnings: List<String>?,
)

private data class WikiApiParseResponse(
    val parse: WikiApiParsePayload?,
    val error: WikiApiError?,
)

private data class WikiApiParsePayload(
    val title: String?,
    @SerializedName("revid")
    val revisionId: Long?,
    val wikitext: WikiApiWikitext?,
    val redirects: List<WikiApiRedirect>?,
)

private data class WikiApiWikitext(
    @SerializedName("*")
    val value: String?,
)

private data class WikiApiRedirect(
    val from: String?,
    val to: String?,
)

private data class WikiApiError(
    val code: String?,
    val info: String?,
)
