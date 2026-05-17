import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class WikiPage(
    val title: String,
    val wikitext: String
)

internal class WikiClient {
    private val client = OkHttpClient()
    private val requestDelayMillis = System.getenv("OSRS_WIKI_REQUEST_DELAY_MS")
        ?.toLongOrNull()
        ?.coerceAtLeast(0)
        ?: 250L
    private var lastRequestAtMillis = 0L

    fun fetchPage(title: String): WikiPage {
        val url = "https://oldschool.runescape.wiki/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("prop", "revisions")
            .addQueryParameter("titles", title)
            .addQueryParameter("rvprop", "content")
            .addQueryParameter("rvslots", "main")
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
            .addQueryParameter("redirects", "1")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "osrs-wiki-xp recipe tree prototype")
            .build()

        waitForRateLimit()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Wiki request failed for '$title': HTTP ${response.code}")
            }

            val body = response.body?.string() ?: error("Wiki request failed for '$title': empty body")
            val root = JsonParser.parseString(body).asJsonObject
            val pages = root.getAsJsonObject("query").getAsJsonArray("pages")
            val page = pages.firstOrNull()?.asJsonObject ?: error("Wiki page not found: $title")

            if (page.has("missing")) {
                error("Wiki page is missing: $title")
            }

            val actualTitle = page.get("title").asString
            val content = page.getAsJsonArray("revisions")
                .first()
                .asJsonObject
                .getAsJsonObject("slots")
                .getAsJsonObject("main")
                .get("content")
                .asString

            return WikiPage(actualTitle, content)
        }
    }

    @Synchronized
    private fun waitForRateLimit() {
        if (requestDelayMillis == 0L) {
            return
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestAtMillis
        if (lastRequestAtMillis != 0L && elapsed < requestDelayMillis) {
            Thread.sleep(requestDelayMillis - elapsed)
        }
        lastRequestAtMillis = System.currentTimeMillis()
    }
}
