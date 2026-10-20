package com.suhood.core.dua

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.suhood.core.model.DuaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads Hisn al-Muslim style adhkar online (Arabic + source).
 * Source: public JSON — https://github.com/asellam/HisnElMuslim (MIT).
 * Falls back to caller-provided offline list when offline or on error.
 */
object OnlineDuaRepository {

    private const val HISN_URL =
        "https://raw.githubusercontent.com/asellam/HisnElMuslim/main/hisn.json"

    @Volatile
    private var cached: List<DuaItem> = emptyList()

    fun cachedOrEmpty(): List<DuaItem> = cached

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun fetchHisnAlMuslim(): Result<List<DuaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(HISN_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            conn.inputStream.bufferedReader().use { reader ->
                val root = JSONObject(reader.readText())
                val out = mutableListOf<DuaItem>()
                var index = 0
                val keys = root.keys()
                while (keys.hasNext()) {
                    val categoryAr = keys.next()
                    val chapter = root.optJSONObject(categoryAr) ?: continue
                    val adhkar = chapter.optJSONArray("Adhkar") ?: continue
                    for (i in 0 until adhkar.length()) {
                        val item = adhkar.optJSONObject(i) ?: continue
                        val text = item.optString("Text").trim()
                        if (text.isBlank()) continue
                        val ref = item.optString("Reference").trim()
                        val count = item.optInt("Count", 1)
                        index++
                        val titleEn = categoryAr.take(48)
                        out.add(
                            DuaItem(
                                id = "online-$index",
                                title = titleEn,
                                arabic = text,
                                transliteration = if (count > 1) "Repeat $count times" else "",
                                translation = "",
                                category = categoryAr.take(40),
                                keywords = listOf(
                                    categoryAr,
                                    text.take(40),
                                    "hisn",
                                    "adhkar",
                                    "online"
                                ),
                                reference = ref.ifBlank { "Hisn al-Muslim" }
                            )
                        )
                    }
                }
                if (out.isEmpty()) error("Empty online dua catalogue")
                cached = out
                out
            }
        }
    }
}
