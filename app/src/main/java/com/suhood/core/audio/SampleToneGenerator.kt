package com.suhood.core.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

/**
 * Creates short royalty-free sine-wave WAV samples locally.
 * Used so Media3 can play real audio without remote catalogs or DRM content.
 * Each trackId maps to a distinct frequency for demo identity.
 *
 * These are **sample tones**, not commercial nasheed recordings.
 */
object SampleToneGenerator {

    fun fileForTrack(context: Context, trackId: String, durationSec: Int = 8): File {
        val dir = File(context.cacheDir, "sample_audio").also { it.mkdirs() }
        val out = File(dir, "$trackId.wav")
        if (out.exists() && out.length() > 44) return out

        val sampleRate = 22050
        val freq = 220.0 + (trackId.hashCode().and(0x7fffffff) % 400)
        val nSamples = sampleRate * durationSec.coerceIn(3, 15)
        val dataSize = nSamples * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(1) // mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        for (i in 0 until nSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                i < sampleRate / 10 -> i.toDouble() / (sampleRate / 10)
                i > nSamples - sampleRate / 10 -> (nSamples - i).toDouble() / (sampleRate / 10)
                else -> 1.0
            }
            val sample = (sin(2 * Math.PI * freq * t) * 0.25 * envelope * Short.MAX_VALUE).toInt()
            buffer.putShort(sample.toShort())
        }

        FileOutputStream(out).use { it.write(buffer.array()) }
        return out
    }
}
