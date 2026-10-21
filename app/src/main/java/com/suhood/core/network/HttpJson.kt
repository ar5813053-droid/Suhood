package com.suhood.core.network

import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object HttpJson {
    fun get(url: String, timeoutMs: Int = 20_000): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Suhood/1.0 (Islamic companion; +https://github.com/ar5813053-droid/Suhood)")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charset.forName("UTF-8"))?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("HTTP $code: ${body.take(200)}")
            return body
        } finally {
            conn.disconnect()
        }
    }
}
