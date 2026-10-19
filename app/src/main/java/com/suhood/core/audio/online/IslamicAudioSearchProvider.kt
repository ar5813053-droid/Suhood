package com.suhood.core.audio.online

interface IslamicAudioSearchProvider {
    val providerId: String
    suspend fun search(query: String, rows: Int = 20): SearchOutcome
}

interface AudioStreamResolver {
    suspend fun resolve(hit: OnlineSearchHit): ResolveOutcome
}
