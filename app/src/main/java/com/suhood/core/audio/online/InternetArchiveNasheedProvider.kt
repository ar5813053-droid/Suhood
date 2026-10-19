package com.suhood.core.audio.online

import com.suhood.core.audio.filter.ContentClass
import com.suhood.core.audio.filter.IslamicContentClassifier
import com.suhood.core.model.AudioTrack
import com.suhood.core.quran.HttpJson
import java.net.URLEncoder
import org.json.JSONObject

/**
 * Public Internet Archive search + direct file download URLs.
 *
 * Terms: only content the Archive hosts for public download.
 * Does not scrape YouTube/Spotify or bypass DRM.
 *
 * Search: https://archive.org/advancedsearch.php
 * Metadata: https://archive.org/metadata/{identifier}
 * Stream: https://archive.org/download/{identifier}/{filename}
 */
class InternetArchiveNasheedProvider : IslamicAudioSearchProvider, AudioStreamResolver {

    override val providerId: String = "internet_archive"

    override suspend fun search(query: String, rows: Int): SearchOutcome {
        val q = query.trim()
        if (q.isEmpty()) return SearchOutcome.Empty("Enter a search term")

        val queryClass = IslamicContentClassifier.classifyQuery(q)
        if (queryClass.state == ContentClass.BLOCKED) {
            return SearchOutcome.Blocked(
                "Suhood is designed for Islamic nasheeds, naats, tilawat, and devotional poetry."
            )
        }

        return try {
            val encoded = URLEncoder.encode(
                "($q) AND mediatype:audio",
                Charsets.UTF_8.name()
            )
            val url =
                "https://archive.org/advancedsearch.php?q=$encoded" +
                    "&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=description" +
                    "&rows=$rows&page=1&output=json"
            val body = HttpJson.get(url)
            val docs = JSONObject(body).getJSONObject("response").getJSONArray("docs")
            val hits = buildList {
                for (i in 0 until docs.length()) {
                    val o = docs.getJSONObject(i)
                    val title = o.optString("title").ifBlank { "Untitled" }
                    val artist = when (val c = o.opt("creator")) {
                        is String -> c
                        else -> o.optJSONArray("creator")?.optString(0) ?: "Unknown"
                    }
                    val desc = o.optString("description")
                    val id = o.optString("identifier")
                    if (id.isBlank()) continue
                    val track = AudioTrack(
                        id = "ia:$id",
                        title = title,
                        creator = artist,
                        collection = "Internet Archive",
                        categoryId = "nasheeds",
                        language = "",
                        durationSeconds = 0,
                        isDemo = false
                    )
                    val cls = IslamicContentClassifier.classify(track)
                    if (cls.state == ContentClass.BLOCKED) continue
                    if (cls.state == ContentClass.UNCERTAIN && queryClass.state != ContentClass.ALLOWED) {
                        // keep uncertain only when user asked for Islamic content
                        if (!title.contains("nasheed", true) &&
                            !title.contains("naat", true) &&
                            !desc.contains("nasheed", true)
                        ) continue
                    }
                    add(
                        OnlineSearchHit(
                            id = "ia:$id",
                            title = title,
                            artist = artist,
                            description = desc.take(300),
                            providerId = providerId,
                            artworkUrl = "https://archive.org/services/img/$id"
                        )
                    )
                }
            }
            if (hits.isEmpty()) {
                SearchOutcome.Empty("No permitted results found. Try nasheed, naat, or tilawat.")
            } else {
                SearchOutcome.Success(hits)
            }
        } catch (e: Exception) {
            SearchOutcome.Error(e.message ?: "Network error")
        }
    }

    override suspend fun resolve(hit: OnlineSearchHit): ResolveOutcome {
        if (!hit.id.startsWith("ia:")) {
            return ResolveOutcome.Failed("Unsupported provider id")
        }
        val identifier = hit.id.removePrefix("ia:")
        return try {
            val meta = HttpJson.get("https://archive.org/metadata/$identifier")
            val files = JSONObject(meta).optJSONArray("files")
                ?: return ResolveOutcome.Failed("No files in item metadata")
            var bestName: String? = null
            var bestScore = -1
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val name = f.optString("name")
                if (name.isBlank()) continue
                val lower = name.lowercase()
                val format = f.optString("format").lowercase()
                val score = when {
                    lower.endsWith(".mp3") && "64kb" !in lower -> 30
                    format.contains("vbr mp3") || format == "mp3" -> 25
                    lower.endsWith(".m4a") || lower.endsWith(".aac") -> 20
                    lower.endsWith(".ogg") || lower.endsWith(".oga") -> 15
                    format.contains("ogg") -> 12
                    else -> -1
                }
                if (score > bestScore) {
                    bestScore = score
                    bestName = name
                }
            }
            if (bestName == null) {
                return ResolveOutcome.Failed("No playable audio file in this Archive item")
            }
            val encodedFile = bestName.split('/').joinToString("/") {
                URLEncoder.encode(it, Charsets.UTF_8.name()).replace("+", "%20")
            }
            val streamUrl = "https://archive.org/download/$identifier/$encodedFile"
            // Re-check policy before returning stream
            val track = AudioTrack(
                id = hit.id,
                title = hit.title,
                creator = hit.artist,
                collection = "Internet Archive",
                categoryId = "nasheeds",
                language = "",
                durationSeconds = 0,
                isDemo = false
            )
            if (!IslamicContentClassifier.isPlayable(track) &&
                IslamicContentClassifier.classify(track).state == ContentClass.BLOCKED
            ) {
                return ResolveOutcome.Failed("Content outside Islamic preferences")
            }
            ResolveOutcome.Ready(
                ResolvedStream(hit = hit, streamUrl = streamUrl, contentType = "audio/*")
            )
        } catch (e: Exception) {
            ResolveOutcome.Failed(e.message ?: "Failed to resolve stream")
        }
    }
}
