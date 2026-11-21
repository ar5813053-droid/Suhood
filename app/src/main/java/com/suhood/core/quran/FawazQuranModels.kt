package com.suhood.core.quran

/**
 * Catalogue models for the Fawaz Ahmed Quran API (jsDelivr CDN).
 * Source: https://github.com/fawazahmed0/quran-api
 *
 * Underlying translation copyright remains with original authors/publishers.
 * Catalogue metadata is loaded dynamically — nothing is invented client-side.
 */
data class FawazEdition(
    val id: String,
    val name: String,
    val author: String,
    val language: String,
    val direction: String,
    val source: String,
    val comments: String,
    val linkMin: String,
    val linkFull: String
) {
    val isLatinScript: Boolean
        get() = id.endsWith("_la") || id.endsWith("_lad") ||
            id.contains("_la_") || direction.equals("ltr", true) &&
            (id.endsWith("la") && id.contains("_"))

    /** Heuristic: Fawaz marks many Latin transliterations with _la / _lad suffix. */
    val isLikelyRomanScript: Boolean
        get() = id.endsWith("_la") || id.endsWith("_lad") ||
            id.contains("_la") && !id.contains("arabic")

    val displayTitle: String
        get() = name.ifBlank { id }.replace('-', ' ')
}

data class FawazLanguageGroup(
    val language: String,
    val editions: List<FawazEdition>
) {
    val count: Int get() = editions.size
    val hasRoman: Boolean get() = editions.any { it.isLikelyRomanScript }
}

data class FawazVerse(
    val chapter: Int,
    val verse: Int,
    val text: String
)

enum class EditionDownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

data class LibraryEditionStatus(
    val editionId: String,
    val inMyLibrary: Boolean,
    val downloadState: EditionDownloadState,
    val progressPercent: Int = 0
)
