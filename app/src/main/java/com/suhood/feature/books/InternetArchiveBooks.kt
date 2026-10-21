package com.suhood.feature.books

import com.suhood.core.network.HttpJson
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset

data class IaBook(
    val identifier: String,
    val title: String,
    val creator: String,
    val description: String,
    val language: String,
    val year: String,
    val coverUrl: String,
    val itemUrl: String,
    val formats: List<String>
)

data class IaTextResult(
    val plainText: String,
    val sourceLabel: String,
    val isHtmlFallback: Boolean = false
)

object InternetArchiveBooks {
    private const val ADV = "https://archive.org/advancedsearch.php"
    private const val META = "https://archive.org/metadata"

    fun search(query: String, rows: Int = 24, page: Int = 1): List<IaBook> {
        val q = buildString {
            append("(")
            append(query.ifBlank { "Islam" })
            append(")")
            append(" AND mediatype:texts")
            append(" AND (language:eng OR language:English OR language:ara OR language:Arabic OR language:urd OR subject:Islam OR subject:Quran OR subject:Hadith)")
        }
        val url = ADV +
            "?q=" + URLEncoder.encode(q, "UTF-8") +
            "&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=description&fl[]=language&fl[]=year&fl[]=format" +
            "&sort[]=downloads+desc&rows=$rows&page=$page&output=json"
        val root = JSONObject(HttpJson.get(url))
        val docs = root.optJSONObject("response")?.optJSONArray("docs") ?: JSONArray()
        val out = mutableListOf<IaBook>()
        for (i in 0 until docs.length()) {
            val d = docs.getJSONObject(i)
            val id = d.optString("identifier")
            if (id.isBlank()) continue
            out += IaBook(
                identifier = id,
                title = firstString(d, "title").ifBlank { id },
                creator = firstString(d, "creator").ifBlank { "Unknown" },
                description = firstString(d, "description").take(400),
                language = firstString(d, "language").ifBlank { "—" },
                year = firstString(d, "year").ifBlank { "—" },
                coverUrl = "https://archive.org/services/img/$id",
                itemUrl = "https://archive.org/details/$id",
                formats = formatList(d.opt("format"))
            )
        }
        return out
    }

    fun metadata(identifier: String): IaBook {
        val root = JSONObject(HttpJson.get("$META/$identifier"))
        val meta = root.optJSONObject("metadata") ?: JSONObject()
        val files = root.optJSONArray("files") ?: JSONArray()
        val formats = mutableListOf<String>()
        for (i in 0 until files.length()) {
            val f = files.optJSONObject(i) ?: continue
            val fmt = f.optString("format")
            if (fmt.isNotBlank() && fmt !in formats) formats += fmt
        }
        val id = meta.optString("identifier").ifBlank { identifier }
        return IaBook(
            identifier = id,
            title = firstString(meta, "title").ifBlank { id },
            creator = firstString(meta, "creator").ifBlank { "Unknown" },
            description = firstString(meta, "description").take(1200),
            language = firstString(meta, "language").ifBlank { "—" },
            year = firstString(meta, "year").ifBlank { firstString(meta, "date").ifBlank { "—" } },
            coverUrl = "https://archive.org/services/img/$id",
            itemUrl = "https://archive.org/details/$id",
            formats = formats
        )
    }

    /**
     * Resolve real plain text from Internet Archive file list.
     * Never returns raw HTML meta pages for TTS/reader.
     */
    fun loadReadableText(identifier: String): IaTextResult {
        val root = JSONObject(HttpJson.get("$META/$identifier", timeoutMs = 25_000))
        val files = root.optJSONArray("files") ?: JSONArray()

        // Prefer true text layers
        val candidates = mutableListOf<Pair<String, String>>() // name to label
        for (i in 0 until files.length()) {
            val f = files.optJSONObject(i) ?: continue
            val name = f.optString("name")
            val fmt = f.optString("format").lowercase()
            if (name.isBlank()) continue
            when {
                fmt.contains("djvutxt") || name.endsWith("_djvu.txt", true) ->
                    candidates += name to "DjVu text layer"
                fmt == "text" || fmt.contains("plain") || name.endsWith(".txt", true) ->
                    candidates += name to "Plain text"
                fmt.contains("html") && name.endsWith(".html", true) && !name.contains("meta", true) ->
                    candidates += name to "HTML content"
            }
        }
        // Stable priority: djvu txt first, then .txt, then html body
        candidates.sortBy { (name, _) ->
            when {
                name.endsWith("_djvu.txt", true) -> 0
                name.endsWith(".txt", true) -> 1
                else -> 2
            }
        }

        for ((name, label) in candidates) {
            val url = "https://archive.org/download/$identifier/${URLEncoder.encode(name, "UTF-8").replace("+", "%20")}"
            val raw = runCatching { httpGet(url, 40_000) }.getOrNull().orEmpty()
            if (raw.isBlank()) continue
            val cleaned = stripToReadableText(raw)
            if (cleaned.length >= 80) {
                return IaTextResult(cleaned.take(120_000), label)
            }
        }

        // Legacy stream endpoint as last resort
        val streamUrl = "https://archive.org/stream/$identifier/${identifier}_djvu.txt"
        val streamRaw = runCatching { httpGet(streamUrl, 30_000) }.getOrNull().orEmpty()
        val streamClean = stripToReadableText(streamRaw)
        if (streamClean.length >= 80) {
            return IaTextResult(streamClean.take(120_000), "Stream text")
        }

        return IaTextResult(
            plainText = "No extractable text layer was found for this item.\n\n" +
                "Many Archive books are scan-only PDFs without a text layer. " +
                "Open the Internet Archive page to read the PDF, or pick another book with a Text/DjVuTXT format.",
            sourceLabel = "unavailable",
            isHtmlFallback = false
        )
    }

    /** Strip tags, scripts, meta — keep readable prose only. */
    fun stripToReadableText(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""
        // If clearly HTML document
        val looksHtml = s.startsWith("<!", true) ||
            s.contains("<html", true) ||
            s.contains("<meta ", true) ||
            s.contains("<head", true)
        if (looksHtml) {
            s = s
                .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
                .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
                .replace(Regex("(?is)<noscript[^>]*>.*?</noscript>"), " ")
                .replace(Regex("(?is)<!--.*?-->"), " ")
                // drop head entirely
                .replace(Regex("(?is)<head[^>]*>.*?</head>"), " ")
                .replace(Regex("(?is)<meta[^>]*>"), " ")
                .replace(Regex("(?is)<link[^>]*>"), " ")
                .replace(Regex("(?is)<title[^>]*>.*?</title>"), " ")
                .replace(Regex("(?i)<br\\s*/?>"), "\n")
                .replace(Regex("(?i)</p>"), "\n\n")
                .replace(Regex("(?i)</div>"), "\n")
                .replace(Regex("(?i)</h[1-6]>"), "\n\n")
                .replace(Regex("(?is)<[^>]+>"), " ")
                .replace(Regex("&nbsp;"), " ")
                .replace(Regex("&amp;"), "&")
                .replace(Regex("&lt;"), "<")
                .replace(Regex("&gt;"), ">")
                .replace(Regex("&quot;"), "\"")
                .replace(Regex("&#\\d+;"), " ")
        }
        // Collapse whitespace
        s = s.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        // Reject if still mostly markup keywords
        val lower = s.lowercase()
        if (lower.contains("og:description") || lower.contains("twitter:card") ||
            (lower.contains("content=\"") && s.length < 500)
        ) {
            return ""
        }
        return s
    }

    private fun httpGet(url: String, timeoutMs: Int): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Suhood/1.0 (Islamic companion)")
            setRequestProperty("Accept", "text/plain,text/html,*/*")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader(Charset.forName("UTF-8"))?.use { it.readText() }.orEmpty()
        } finally {
            conn.disconnect()
        }
    }

    private fun firstString(obj: JSONObject, key: String): String {
        if (!obj.has(key)) return ""
        return when (val v = obj.get(key)) {
            is JSONArray -> v.optString(0, "")
            else -> v.toString().trim('"')
        }
    }

    private fun formatList(raw: Any?): List<String> = when (raw) {
        is JSONArray -> (0 until raw.length()).map { raw.optString(it) }.filter { it.isNotBlank() }
        is String -> listOf(raw)
        else -> emptyList()
    }
}
