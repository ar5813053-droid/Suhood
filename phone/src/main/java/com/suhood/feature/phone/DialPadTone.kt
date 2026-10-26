package com.suhood.feature.phone

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/** Short DTMF-style feedback when tapping dial pad keys. */
object DialPadTone {
    private var tone: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun gen(): ToneGenerator {
        return tone ?: ToneGenerator(AudioManager.STREAM_DTMF, 80).also { tone = it }
    }

    fun play(digit: Char) {
        val type = when (digit) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        runCatching {
            val g = gen()
            g.startTone(type, 120)
        }
    }

    fun release() {
        runCatching { tone?.release() }
        tone = null
    }
}
