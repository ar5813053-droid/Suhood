package com.suhood.feature.books

import android.content.Context
import com.suhood.feature.hadith.HadithScreens

/**
 * Unified catalog entry for the Suhood bookstore.
 * Source is either a local Hadith collection or Internet Archive listing.
 */
enum class BookSourceType { HADITH, ARCHIVE }

data class CatalogBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val description: String,
    val coverUrl: String?,
    val sourceType: BookSourceType,
    /** For ARCHIVE: Internet Archive identifier. For HADITH: book id used by HadithScreens. */
    val openKey: String
)

object BooksCatalog {
    val categories = listOf(
        "All",
        "Hadith",
        "Tafseer",
        "Seerah",
        "Fiqh",
        "Aqeedah",
        "Tazkiyah",
        "Islamic History",
        "Other"
    )

    /** Local Hadith collections already shipped in assets — always available offline. */
    fun hadithBooks(): List<CatalogBook> = HadithScreens.catalog.map { b ->
        CatalogBook(
            id = "hadith:${b.id}",
            title = b.title,
            author = b.arabicTitle,
            category = "Hadith",
            description = b.subtitle,
            coverUrl = null,
            sourceType = BookSourceType.HADITH,
            openKey = b.id
        )
    }

    fun archiveQueryForCategory(category: String): String = when (category) {
        "Hadith" -> "Hadith OR Sunnah OR Bukhari OR Muslim"
        "Tafseer" -> "Tafsir OR Tafseer OR Quran commentary"
        "Seerah" -> "Seerah OR Sirah OR Prophet Muhammad biography"
        "Fiqh" -> "Fiqh OR Islamic jurisprudence"
        "Aqeedah" -> "Aqeedah OR Aqida OR Islamic creed"
        "Tazkiyah" -> "Tazkiyah OR purification soul OR spiritual"
        "Islamic History" -> "Islamic history OR Caliphate OR Muslim civilization"
        "Other" -> "Islam"
        else -> "Islam"
    }

    fun toCatalog(ia: IaBook, categoryHint: String = "Other"): CatalogBook {
        val cat = when {
            categoryHint != "All" && categoryHint != "Other" -> categoryHint
            ia.title.contains("hadith", true) || ia.title.contains("bukhari", true) -> "Hadith"
            ia.title.contains("tafsir", true) || ia.title.contains("tafseer", true) -> "Tafseer"
            ia.title.contains("seerah", true) || ia.title.contains("sirah", true) -> "Seerah"
            ia.title.contains("fiqh", true) -> "Fiqh"
            else -> categoryHint
        }
        return CatalogBook(
            id = "ia:${ia.identifier}",
            title = ia.title,
            author = ia.creator,
            category = cat,
            description = ia.description,
            coverUrl = ia.coverUrl,
            sourceType = BookSourceType.ARCHIVE,
            openKey = ia.identifier
        )
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
