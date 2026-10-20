package com.suhood.core.dua

import com.suhood.core.model.DuaItem
import java.text.Normalizer

/**
 * Offline multilingual dua search (EN, Roman Urdu/Hinglish, Hindi spellings, Arabic).
 * Diacritic-insensitive for Arabic queries; ranking by match strength.
 */
object DuaSearch {
    fun normalize(input: String): String {
        var s = input.lowercase().trim()
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
            .replace("\\p{M}+".toRegex(), "")
        // common hinglish variants
        s = s.replace("aa", "a").replace("ee", "e").replace("oo", "o")
        return s
    }

    fun search(query: String, items: List<DuaItem>): List<DuaItem> {
        val q = normalize(query)
        if (q.isBlank()) return items
        data class Ranked(val item: DuaItem, val score: Int)
        return items.mapNotNull { item ->
            val hay = normalize(
                listOf(
                    item.title,
                    item.transliteration,
                    item.translation,
                    item.category,
                    item.reference,
                    item.arabic,
                    item.keywords.joinToString(" ")
                ).joinToString(" ")
            )
            var score = 0
            if (hay.contains(q)) score += 10
            q.split(" ").filter { it.length >= 2 }.forEach { token ->
                if (hay.contains(token)) score += 3
            }
            item.keywords.forEach { kw ->
                val n = normalize(kw)
                if (n == q) score += 20
                else if (n.contains(q) || q.contains(n)) score += 8
            }
            if (score > 0) Ranked(item, score) else null
        }.sortedByDescending { it.score }.map { it.item }
    }
}
