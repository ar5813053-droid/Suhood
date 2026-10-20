package com.suhood.core.audio.online

/**
 * Online search hit before stream resolution.
 * Not playable until [AudioStreamResolver] produces a media URI.
 */
data class OnlineSearchHit(
    val id: String,
    val title: String,
    val artist: String,
    val description: String = "",
    val providerId: String,
    /** Archive.org identifier (or other provider key) used to resolve the file list. */
    val sourceId: String = "",
    val artworkUrl: String? = null,
    val durationSeconds: Int = 0
)

data class ResolvedStream(
    val hit: OnlineSearchHit,
    val streamUrl: String,
    val contentType: String? = null
)

sealed class SearchOutcome {
    data class Success(val hits: List<OnlineSearchHit>) : SearchOutcome()
    data class Empty(val message: String) : SearchOutcome()
    data class Blocked(val message: String) : SearchOutcome()
    data class Error(val message: String) : SearchOutcome()
}

sealed class ResolveOutcome {
    data class Ready(val stream: ResolvedStream) : ResolveOutcome()
    data class Failed(val message: String) : ResolveOutcome()
}
