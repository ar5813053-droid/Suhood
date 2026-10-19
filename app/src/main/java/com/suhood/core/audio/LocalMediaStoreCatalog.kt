package com.suhood.core.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.suhood.core.model.AudioTrack
import com.suhood.core.audio.filter.IslamicContentClassifier

/**
 * Scans the device for local audio via MediaStore (same family of approach
 * as offline music players — not YouTube/Spotify streaming).
 *
 * Users place nasheeds/tilawat files on the phone; Suhood lists and plays them.
 * Does not scrape protected online catalogs (unlike Meld's YouTube Music path).
 */
class LocalMediaStoreCatalog(private val context: Context) : AudioCatalogProvider {

    private val fallback = LocalSampleCatalogProvider()

    override fun allTracks(): List<AudioTrack> {
        val local = queryMediaStore()
        val raw = if (local.isEmpty()) fallback.allTracks() else local
        return IslamicContentClassifier.filterTracks(raw)
    }

    override fun search(query: String): List<AudioTrack> {
        if (query.isBlank()) return emptyList()
        val q = query.trim()
        val hits = allTracks().filter {
            it.title.contains(q, true) ||
                it.creator.contains(q, true) ||
                it.collection.contains(q, true) ||
                it.titleAr.contains(q)
        }
        return IslamicContentClassifier.filterTracks(hits)
    }

    override fun resolveUri(context: Context, track: AudioTrack): Uri? {
        if (track.id.startsWith("ms:")) {
            val id = track.id.removePrefix("ms:").toLongOrNull() ?: return null
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        }
        return fallback.resolveUri(context, track)
    }

    private fun queryMediaStore(): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        return try {
            context.contentResolver.query(
                collection, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown"
                    val artist = cursor.getString(artistCol) ?: "Unknown"
                    val album = cursor.getString(albumCol) ?: ""
                    val durMs = cursor.getLong(durCol).coerceAtLeast(0)
                    tracks.add(
                        AudioTrack(
                            id = "ms:$id",
                            title = title,
                            creator = artist,
                            collection = album,
                            categoryId = "local",
                            language = "Local",
                            durationSeconds = (durMs / 1000).toInt().coerceAtLeast(1),
                            isDemo = false,
                            titleAr = "",
                            creatorAr = ""
                        )
                    )
                }
                tracks
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
