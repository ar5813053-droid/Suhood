package com.suhood.core.audio

import android.content.Context
import android.net.Uri
import com.suhood.core.data.DemoData
import com.suhood.core.model.AudioTrack

/**
 * Resolves a playable [Uri] for a catalog track.
 *
 * Current implementation: local sample tones only (test audio, not nasheeds/tilawat).
 * Replace [LocalSampleCatalogProvider] with a licensed remote provider when available.
 */
interface AudioCatalogProvider {
    fun resolveUri(context: Context, track: AudioTrack): Uri?
    fun allTracks(): List<AudioTrack>
    fun search(query: String): List<AudioTrack>
}

class LocalSampleCatalogProvider : AudioCatalogProvider {
    override fun resolveUri(context: Context, track: AudioTrack): Uri? {
        return runCatching {
            val file = SampleToneGenerator.fileForTrack(context, track.id)
            Uri.fromFile(file)
        }.getOrNull()
    }

    override fun allTracks(): List<AudioTrack> = DemoData.audioTracks

    override fun search(query: String): List<AudioTrack> {
        if (query.isBlank()) return emptyList()
        val q = query.trim()
        return DemoData.audioTracks.filter {
            it.title.contains(q, true) ||
                it.titleAr.contains(q) ||
                it.creator.contains(q, true) ||
                it.collection.contains(q, true) ||
                it.categoryId.contains(q, true)
        }
    }
}
