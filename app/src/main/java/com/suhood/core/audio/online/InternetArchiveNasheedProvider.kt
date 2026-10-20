package com.suhood.core.audio.online

import com.suhood.core.audio.filter.ContentClass
import com.suhood.core.audio.filter.IslamicContentClassifier
import com.suhood.core.model.AudioTrack
import com.suhood.core.quran.HttpJson
import java.net.URLEncoder
import org.json.JSONObject

/**
 * Free Islamic audio via Internet Archive (public download URLs).
 *
 * Pattern (similar in spirit to free clients): search public catalog → resolve
 * direct MP3/stream URL → play with ExoPlayer.
 * Does NOT use Spotify/YouTube Music reverse-engineering.
 * Only nasheed / naat / nazam / tilawat / Islamic devotional audio.
 */
class InternetArchiveNasheedProvider : IslamicAudioSearchProvider, AudioStreamResolver {

    override val providerId: String = "internet_archive"

    override suspend fun search(query: String, rows: Int): SearchOutcome {
        val q = query.trim()
        if (q.isEmpty()) return SearchOutcome.Empty("Enter a search term")

        val queryClass = IslamicContentClassifier.classifyQuery(q)
        if (queryClass.state == ContentClass.BLOCKED) {
            return SearchOutcome.Blocked(
                "Only Islamic nasheeds, naats, nazam, tilawat, and Quran audio are allowed."
            )
        }

        return try {
            // Force Islamic audio scope — never general music
            val islamicScope =
                "(nasheed OR naat OR nazam OR anasheed OR tilawat OR quran OR islamic OR " +
                    "\"devotional\" OR madah OR hamd OR salawat OR adhan OR azan)"
            val lucene = "($q) AND $islamicScope AND mediatype:(audio OR etree)"
            val encoded = URLEncoder.encode(lucene, Charsets.UTF_8.name())
            val url =
                "https://archive.org/advancedsearch.php?q=$encoded" +
                    "&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=description&fl[]=subject" +
                    "&rows=$rows&page=1&output=json&sort[]=downloads+desc"
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
                    val subject = o.optString("subject")
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
                        isDemo = false,
                        titleAr = ""
                    )
                    // Strict: require Islamic signal in metadata
                    val blob = "$title $artist $desc $subject".lowercase()
                    val islamic = IslamicContentClassifier.allowedKeywordHit(blob)
                    val blocked = IslamicContentClassifier.blockedKeywordHit(blob)
                    if (blocked && !islamic) continue
                    if (!islamic && queryClass.state != ContentClass.ALLOWED) {
                        // Ambiguous archive hit without Islamic keywords — skip
                        continue
                    }
                    add(
                        OnlineSearchHit(
                            id = "ia:$id",
                            title = title,
                            artist = artist,
                            providerId = providerId,
                            sourceId = id,
                            durationSeconds = 0,
                            artworkUrl = "https://archive.org/services/img/$id"
                        )
                    )
                }
            }
            if (hits.isEmpty()) {
                SearchOutcome.Empty("No Islamic audio found. Try: nasheed, naat, nazam, tilawat")
            } else {
                SearchOutcome.Success(hits)
            }
        } catch (e: Exception) {
            SearchOutcome.Error(e.message ?: "Search failed")
        }
    }

    /** Curated free Islamic browse (no login). */
    suspend fun browseCategory(category: String, rows: Int = 24): SearchOutcome {
        val term = when (category.lowercase()) {
            "nasheed", "nasheeds" -> "nasheed OR anasheed"
            "naat", "naats" -> "naat OR madah"
            "nazam" -> "nazam OR islamic poetry"
            "tilawat", "quran" -> "tilawat OR quran recitation"
            "adhan" -> "adhan OR azan"
            else -> "nasheed OR naat OR tilawat"
        }
        return search(term, rows)
    }

    override suspend fun resolve(hit: OnlineSearchHit): ResolveOutcome {
        val identifier = hit.sourceId.ifBlank {
            hit.id.removePrefix("ia:")
        }
        if (identifier.isBlank()) return ResolveOutcome.Failed("Missing Archive identifier")
        return try {
            val metaUrl = "https://archive.org/metadata/$identifier"
            val body = HttpJson.get(metaUrl)
            val root = JSONObject(body)
            val files = root.optJSONArray("files")
                ?: return ResolveOutcome.Failed("No files for this item")

            var bestName: String? = null
            var bestScore = -1
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val name = f.optString("name")
                if (name.isBlank()) continue
                val format = f.optString("format").lowercase()
                val lower = name.lowercase()
                // Prefer streamable audio
                val score = when {
                    lower.endsWith(".mp3") -> 100
                    format.contains("mp3") || format.contains("vbr mp3") -> 95
                    lower.endsWith(".m4a") || lower.endsWith(".aac") -> 80
                    lower.endsWith(".ogg") || lower.endsWith(".opus") -> 70
                    lower.endsWith(".flac") -> 60
                    format.contains("audio") -> 40
                    else -> -1
                }
                if (score > bestScore) {
                    bestScore = score
                    bestName = name
                }
            }
            if (bestName == null) {
                return ResolveOutcome.Failed("No playable audio file in this item")
            }
            val encodedFile = bestName.split('/').joinToString("/") {
                URLEncoder.encode(it, Charsets.UTF_8.name()).replace("+", "%20")
            }
            val streamUrl = "https://archive.org/download/$identifier/$encodedFile"

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
            if (IslamicContentClassifier.classify(track).state == ContentClass.BLOCKED) {
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
