package com.suhood.core.audio.filter

import com.suhood.core.model.AudioTrack

enum class ContentClass { ALLOWED, BLOCKED, UNCERTAIN }

data class Classification(
    val state: ContentClass,
    val reason: String
)

/**
 * Conservative classifier for Islamic-audio preference.
 * Uses category metadata, allow/block keyword lists, and source tags.
 * Does NOT claim religious certainty from a single English word.
 */
object IslamicContentClassifier {

    private val allowedCategories = setOf(
        "nasheeds", "nasheed", "madah", "tilawat", "quran", "adhkar",
        "duas", "lectures", "reminders", "naat", "nazam", "local"
    )

    private val allowedSignals = listOf(
        "nasheed", "naat", "nazam", "tilawat", "quran", "quraan", "surah",
        "ayah", "adhkar", "dhikr", "dua", "duaa", "madah", "hamd", "salawat",
        "islamic", "deen", "iman", "rahman", "tawakkul", "subhan"
    )

    /** Ordinary entertainment music signals (conservative block only with low Islamic signal). */
    private val blockedSignals = listOf(
        "nightclub", "party mix", "explicit", "club banger", "hip-hop instrumental",
        "trap beat", "edm festival", "karaoke pop"
    )

    fun classify(track: AudioTrack): Classification {
        val cat = track.categoryId.lowercase()
        if (cat in allowedCategories || cat.startsWith("local")) {
            return Classification(ContentClass.ALLOWED, "Category: ${track.categoryId}")
        }

        val blob = listOf(track.title, track.creator, track.collection, track.titleAr, track.language)
            .joinToString(" ")
            .lowercase()

        val hasAllowed = allowedSignals.any { blob.contains(it) }
        val hasBlocked = blockedSignals.any { blob.contains(it) }

        // Demo sample tracks are allowed for offline testing
        if (track.isDemo) {
            return Classification(ContentClass.ALLOWED, "Local sample / demo catalog")
        }

        if (hasBlocked && !hasAllowed) {
            return Classification(ContentClass.BLOCKED, "Matched non-Islamic entertainment signals")
        }
        if (hasAllowed) {
            return Classification(ContentClass.ALLOWED, "Islamic metadata signals")
        }
        return Classification(ContentClass.UNCERTAIN, "Insufficient metadata to verify")
    }

    fun classifyQuery(query: String): Classification {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return Classification(ContentClass.UNCERTAIN, "Empty query")
        if (blockedSignals.any { q.contains(it) } && allowedSignals.none { q.contains(it) }) {
            return Classification(ContentClass.BLOCKED, "Query matches non-Islamic entertainment")
        }
        if (allowedSignals.any { q.contains(it) }) {
            return Classification(ContentClass.ALLOWED, "Islamic query")
        }
        return Classification(ContentClass.UNCERTAIN, "Ambiguous query")
    }

    fun filterTracks(tracks: List<AudioTrack>, includeUncertain: Boolean = false): List<AudioTrack> =
        tracks.filter {
            when (classify(it).state) {
                ContentClass.ALLOWED -> true
                ContentClass.UNCERTAIN -> includeUncertain
                ContentClass.BLOCKED -> false
            }
        }

    fun isPlayable(track: AudioTrack): Boolean =
        classify(track).state == ContentClass.ALLOWED
}
