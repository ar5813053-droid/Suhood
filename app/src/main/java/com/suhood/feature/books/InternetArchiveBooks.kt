package com.suhood.feature.books

import com.suhood.core.network.HttpJson
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

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

object InternetArchiveBooks {
    private const val ADV = "https://archive.org/advancedsearch.php"
    private const val META = "https://archive.org/metadata"

    /** Search public Islamic texts on Internet Archive (documented advancedsearch API). */
    fun search(query: String, rows: Int = 24, page: Int = 1): List<IaBook> {
        val q = buildString {
            append("(")
            append(if (query.isBlank()) "subject:(Islam OR Islamic OR Quran OR Hadith OR Seerah)" else query.trim())
            append(")")
            append(" AND mediatype:texts")
            append(" AND (language:eng OR language:English OR language:ara OR language:Arabic OR language:urd)")
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
            val f = files.optJSONObject(i)
            if (f == null) continue
            val fmt = f.optString("format")
            if (fmt.isBlank()) continue
            if (fmt !in formats) formats += fmt
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

    /** Prefer plain text / EPUB links that are publicly listed on IA. */
    fun preferredTextUrl(identifier: String, formats: List<String>): String? {
        val lower = formats.map { it.lowercase() }
        return when {
            lower.any { it.contains("text") || it.contains("plain") } ->
                "https://archive.org/stream/$identifier/${identifier}_djvu.txt"
            lower.any { it.contains("epub") } ->
                "https://archive.org/download/$identifier/${identifier}.epub"
            else -> null
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
