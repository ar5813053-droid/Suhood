package com.suhood.feature.quest

import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

/** Difficulty bands for fixed XP (habit points — not spiritual rank). */
enum class QuestDifficulty(val xp: Int) {
    EASY(10),
    MEDIUM(20),
    CHALLENGING(30)
}

enum class QuestCategory {
    SALAH, QURAN, DHIKR, LEARNING, CHARACTER, SERVICE, REFLECTION, DUA, MILESTONE
}

/**
 * Template for a possible daily task. Text is non-claiming and never invents scripture.
 * Farz Salah tasks stay clearly labeled as obligations the user confirms — never auto-marked.
 */
data class QuestTemplate(
    val templateId: String,
    val category: QuestCategory,
    val difficulty: QuestDifficulty,
    val minutes: Int,
    val title: String,
    val detail: String,
    /** Goal keys: salah, quran, dhikr, learning, deeds, worship */
    val goalTags: Set<String> = emptySet(),
    val optional: Boolean = false,
    val isFarz: Boolean = false
)

data class DailyQuestTask(
    val id: String,
    val templateId: String,
    val category: QuestCategory,
    val difficulty: QuestDifficulty,
    val minutes: Int,
    val title: String,
    val detail: String,
    val xpReward: Int,
    val reason: String,
    val activeDate: String,
    val completed: Boolean = false,
    val optional: Boolean = false,
    val isFarz: Boolean = false
)

data class QuestProfileInput(
    val ageGroup: String = "",
    val lifestyle: String = "",
    val goals: Set<String> = emptySet(),
    val consistency: String = "",
    val hardestPrayer: String = "",
    val practice: String = "both",
    val dailyMinutes: Int = 15,
    val reminderStyle: String = "Gentle",
    val language: String = "English",
    val phoneUsage: String = "",
    val freeTimeHint: String = "",
    // 30-day challenge personalization
    val quranLevel: String = "beginner", // beginner | intermediate | advanced
    val challengeDifficulty: String = "normal", // easy | normal | advanced
    val salahConsistency: String = "mixed", // rarely | mixed | mostly | always
    val preferredMinutes: Int = 15
)

data class TierAttemptState(
    val tierIndex: Int,
    val startEpochDay: Long,
    val deadlineEpochDay: Long,
    val creditedXp: Int,
    val completed: Boolean
)

/** 30-day monthly challenge rules. */
object QuestTierRules {
    const val TIER_XP_TARGET = 2500
    const val DAILY_XP_CAP = 100
    const val ATTEMPT_DAYS = 30
    const val XP_EASY = 10
    const val XP_MEDIUM = 20
    const val XP_CHALLENGING = 30

    val MILESTONE_DAYS = setOf(7, 14, 21, 28, 30)

    fun deadlineEpochDay(startEpochDay: Long): Long = startEpochDay + (ATTEMPT_DAYS - 1)

    fun daysRemaining(todayEpochDay: Long, deadlineEpochDay: Long): Int =
        (deadlineEpochDay - todayEpochDay + 1).toInt().coerceAtLeast(0)

    fun isExpired(todayEpochDay: Long, deadlineEpochDay: Long): Boolean =
        todayEpochDay > deadlineEpochDay

    fun creditAllowed(reward: Int, alreadyToday: Int): Int {
        val room = (DAILY_XP_CAP - alreadyToday).coerceAtLeast(0)
        return reward.coerceAtMost(room)
    }

    fun dayOfChallenge(startEpochDay: Long, todayEpochDay: Long): Int =
        (todayEpochDay - startEpochDay + 1).toInt().coerceIn(1, ATTEMPT_DAYS)

    fun weekOf(dayOfChallenge: Int): Int = ((dayOfChallenge - 1) / 7).coerceIn(0, 3) + 1
}

object TierAttemptLogic {
    fun open(tierIndex: Int, today: LocalDate): TierAttemptState {
        val start = today.toEpochDay()
        return TierAttemptState(
            tierIndex = tierIndex,
            startEpochDay = start,
            deadlineEpochDay = QuestTierRules.deadlineEpochDay(start),
            creditedXp = 0,
            completed = false
        )
    }

    fun afterCredit(state: TierAttemptState, credit: Int): TierAttemptState {
        if (state.completed || credit <= 0) return state
        val next = (state.creditedXp + credit).coerceAtMost(QuestTierRules.TIER_XP_TARGET)
        return state.copy(
            creditedXp = next,
            completed = next >= QuestTierRules.TIER_XP_TARGET
        )
    }

    fun resetIfExpired(state: TierAttemptState, today: LocalDate, nextTierIndex: Int): TierAttemptState {
        if (!QuestTierRules.isExpired(today.toEpochDay(), state.deadlineEpochDay)) return state
        return open(nextTierIndex, today)
    }
}

/** Rank display — habit consistency labels, not spiritual rank. */
object Ranks {
    data class Rank(val latin: String, val arabic: String)

    private val names = listOf(
        Rank("Mubtadi'", "مبتدئ"),
        Rank("Consistency", "استقامة"),
        Rank("Steadiness", "ثبات"),
        Rank("Devotion", "خشوع"),
        Rank("Excellence", "إحسان")
    )

    fun forTier(tierIndex: Int): Rank = names[tierIndex.coerceIn(0, names.lastIndex)]
}

/** Legacy alias kept for widgets. */
object QuestRules {
    const val TIER_XP = QuestTierRules.TIER_XP_TARGET
}

// ─── Task library ────────────────────────────────────────────────────────────

object QuestLibrary {
    val all: List<QuestTemplate> = listOf(
        // Salah — farz (user-confirmed only)
        QuestTemplate("salah_fajr", QuestCategory.SALAH, QuestDifficulty.EASY, 10,
            "Pray Fajr", "Confirm that you prayed Fajr today. Farz remains an obligation — XP only tracks your confirmation habit.", setOf("salah", "worship"), isFarz = true),
        QuestTemplate("salah_dhuhr", QuestCategory.SALAH, QuestDifficulty.EASY, 10,
            "Pray Dhuhr", "Confirm that you prayed Dhuhr today.", setOf("salah", "worship"), isFarz = true),
        QuestTemplate("salah_asr", QuestCategory.SALAH, QuestDifficulty.EASY, 10,
            "Pray Asr", "Confirm that you prayed Asr today.", setOf("salah", "worship"), isFarz = true),
        QuestTemplate("salah_maghrib", QuestCategory.SALAH, QuestDifficulty.EASY, 10,
            "Pray Maghrib", "Confirm that you prayed Maghrib today.", setOf("salah", "worship"), isFarz = true),
        QuestTemplate("salah_isha", QuestCategory.SALAH, QuestDifficulty.EASY, 10,
            "Pray Isha", "Confirm that you prayed Isha today.", setOf("salah", "worship"), isFarz = true),
        QuestTemplate("salah_all_five", QuestCategory.SALAH, QuestDifficulty.MEDIUM, 25,
            "Complete all five Salah", "Confirm you offered all five daily prayers today.", setOf("salah", "worship"), isFarz = true),
        QuestTemplate("salah_on_time", QuestCategory.SALAH, QuestDifficulty.MEDIUM, 15,
            "Pray Salah on time", "Aim to offer today's prayers within their time windows. Confirm honestly.", setOf("salah"), isFarz = true),

        // Quran
        QuestTemplate("quran_1p", QuestCategory.QURAN, QuestDifficulty.EASY, 5,
            "Read 1 Quran page", "Read one page carefully in Arabic or your preferred script.", setOf("quran", "worship")),
        QuestTemplate("quran_2p", QuestCategory.QURAN, QuestDifficulty.EASY, 10,
            "Read 2 Quran pages", "Read two pages with presence of heart.", setOf("quran", "worship")),
        QuestTemplate("quran_4p", QuestCategory.QURAN, QuestDifficulty.MEDIUM, 15,
            "Read 4 Quran pages", "A steady intermediate reading target for today.", setOf("quran")),
        QuestTemplate("quran_5p", QuestCategory.QURAN, QuestDifficulty.CHALLENGING, 20,
            "Read 5 Quran pages", "A stronger daily reading target when you have time.", setOf("quran")),
        QuestTemplate("quran_streak", QuestCategory.QURAN, QuestDifficulty.EASY, 5,
            "Continue Quran reading streak", "Open the Mushaf and read even a few lines to keep the habit alive.", setOf("quran")),
        QuestTemplate("quran_translation", QuestCategory.QURAN, QuestDifficulty.MEDIUM, 15,
            "Read translation or tafsir", "Read a short translation or reliable tafsir portion of what you recited.", setOf("quran", "learning")),
        QuestTemplate("quran_ayah", QuestCategory.QURAN, QuestDifficulty.MEDIUM, 10,
            "Memorize or revise an Ayah", "Review one ayah you are memorizing, or learn a short new one carefully.", setOf("quran", "learning")),

        // Dhikr / Adhkar
        QuestTemplate("adhkar_morning", QuestCategory.DHIKR, QuestDifficulty.EASY, 8,
            "Morning Adhkar", "Complete your morning remembrance after Fajr or upon waking.", setOf("dhikr", "worship")),
        QuestTemplate("adhkar_evening", QuestCategory.DHIKR, QuestDifficulty.EASY, 8,
            "Evening Adhkar", "Complete your evening remembrance before sleep.", setOf("dhikr", "worship")),
        QuestTemplate("dhikr_tasbeeh", QuestCategory.DHIKR, QuestDifficulty.EASY, 5,
            "Complete a Dhikr / Tasbeeh target", "Use Suhood Tasbeeh or your beads for a short, focused set.", setOf("dhikr")),
        QuestTemplate("dua_learn", QuestCategory.DUA, QuestDifficulty.EASY, 5,
            "Read and understand a Dua", "Open one authentic dua, read it slowly, and note its meaning.", setOf("learning", "dhikr")),

        // Learning / character / deeds
        QuestTemplate("learn_fact", QuestCategory.LEARNING, QuestDifficulty.EASY, 5,
            "Learn one Islamic fact", "From Quran, Hadith, or a trusted lesson — note one takeaway only.", setOf("learning")),
        QuestTemplate("good_deed", QuestCategory.SERVICE, QuestDifficulty.EASY, 10,
            "Perform one good deed", "Help someone, give charity, or do a quiet act of kindness today.", setOf("deeds", "worship")),
        QuestTemplate("tawbah_reflect", QuestCategory.REFLECTION, QuestDifficulty.EASY, 5,
            "Make sincere Tawbah and reflect", "A private moment of repentance and honest reflection — no score of sin, only return to Allah.", setOf("worship")),
        QuestTemplate("review_progress", QuestCategory.REFLECTION, QuestDifficulty.EASY, 5,
            "Review daily worship progress", "Glance at today's quests and note one thing to improve tomorrow — gently.", setOf("worship")),
        QuestTemplate("help_family", QuestCategory.SERVICE, QuestDifficulty.MEDIUM, 15,
            "Help family or someone in need", "Offer practical help without being asked.", setOf("deeds")),
        QuestTemplate("charity_kindness", QuestCategory.SERVICE, QuestDifficulty.MEDIUM, 10,
            "Charity or kindness challenge", "Give what you can, or extend a sincere kindness.", setOf("deeds")),
        QuestTemplate("revise_lessons", QuestCategory.LEARNING, QuestDifficulty.MEDIUM, 10,
            "Review learned Duas and Ayat", "Revisit something you learned earlier this month.", setOf("learning", "quran")),
        QuestTemplate("character_reflect", QuestCategory.CHARACTER, QuestDifficulty.EASY, 5,
            "Reflect on character and habits", "Name one habit to soften and one to strengthen.", setOf("worship")),

        // Weekly milestones (generated on milestone days)
        QuestTemplate("milestone_week", QuestCategory.MILESTONE, QuestDifficulty.MEDIUM, 5,
            "Weekly milestone check-in", "You reached a weekly checkpoint. Review progress with gratitude — not comparison.", setOf("worship"), optional = false),
        QuestTemplate("milestone_final", QuestCategory.MILESTONE, QuestDifficulty.MEDIUM, 10,
            "30-day review", "Look back on this month's confirmed efforts. Celebrate consistency, not perfection.", setOf("worship"))
    )

    fun byId(id: String): QuestTemplate? = all.find { it.templateId == id }
}

object DailyQuestEngine {
    private fun seed(dayKey: String, profile: QuestProfileInput): Int {
        var h = dayKey.hashCode()
        h = 31 * h + profile.goals.hashCode()
        h = 31 * h + profile.challengeDifficulty.hashCode()
        h = 31 * h + profile.quranLevel.hashCode()
        return abs(h)
    }

    private fun quranTemplate(profile: QuestProfileInput): QuestTemplate {
        return when (profile.challengeDifficulty to profile.quranLevel) {
            "advanced" to "advanced" -> QuestLibrary.byId("quran_5p")!!
            "advanced" to "intermediate", "normal" to "advanced" -> QuestLibrary.byId("quran_4p")!!
            "easy" to "beginner" -> QuestLibrary.byId("quran_1p")!!
            else -> QuestLibrary.byId("quran_2p")!!
        }
    }

    /**
     * Build unique daily quest set for day-of-challenge 1..30.
     * Difficulty and mix adapt to profile + week phase + completion rate hint.
     */
    fun generateForDay(
        profile: QuestProfileInput,
        today: LocalDate,
        dayOfChallenge: Int,
        completionRateHint: Float = 0.5f
    ): List<QuestTemplate> {
        val day = dayOfChallenge.coerceIn(1, 30)
        val week = QuestTierRules.weekOf(day)
        val goals = profile.goals.ifEmpty { setOf("salah", "quran", "dhikr") }
        val hard = profile.challengeDifficulty == "advanced" ||
            (profile.challengeDifficulty == "normal" && completionRateHint > 0.7f)
        val easy = profile.challengeDifficulty == "easy" || completionRateHint < 0.3f

        val picks = mutableListOf<QuestTemplate>()

        // Core Salah focus
        if ("salah" in goals || "worship" in goals) {
            when {
                day % 5 == 0 || week >= 3 -> picks += QuestLibrary.byId("salah_all_five")!!
                day % 3 == 0 -> picks += QuestLibrary.byId("salah_on_time")!!
                else -> {
                    val salahIds = listOf("salah_fajr", "salah_dhuhr", "salah_asr", "salah_maghrib", "salah_isha")
                    val idx = (day + seed(today.toString(), profile)) % salahIds.size
                    picks += QuestLibrary.byId(salahIds[idx])!!
                }
            }
        }

        // Quran
        if ("quran" in goals || "worship" in goals) {
            picks += when {
                day in listOf(7, 14, 21, 28) -> QuestLibrary.byId("quran_streak")!!
                week >= 3 && !easy -> QuestLibrary.byId("quran_translation")!!
                day % 4 == 0 -> QuestLibrary.byId("quran_ayah")!!
                else -> quranTemplate(profile)
            }
        }

        // Dhikr / adhkar
        if ("dhikr" in goals || "worship" in goals) {
            picks += when {
                day % 2 == 0 -> QuestLibrary.byId("adhkar_morning")!!
                else -> QuestLibrary.byId("adhkar_evening")!!
            }
            if (week >= 2 || hard) picks += QuestLibrary.byId("dhikr_tasbeeh")!!
        }

        // Week-specific extras
        when (week) {
            1 -> { // Build habits
                if ("deeds" in goals || day % 2 == 1) picks += QuestLibrary.byId("good_deed")!!
                picks += QuestLibrary.byId("review_progress")!!
            }
            2 -> { // Consistency
                if ("learning" in goals || day % 3 == 0) picks += QuestLibrary.byId("learn_fact")!!
                if (day % 2 == 0) picks += QuestLibrary.byId("quran_ayah")!!
            }
            3 -> { // Deepen
                picks += QuestLibrary.byId("help_family")!!
                picks += QuestLibrary.byId("character_reflect")!!
                if ("learning" in goals) picks += QuestLibrary.byId("dua_learn")!!
            }
            4 -> { // Strengthen
                picks += QuestLibrary.byId("revise_lessons")!!
                if (day >= 25) picks += QuestLibrary.byId("charity_kindness")!!
                picks += QuestLibrary.byId("tawbah_reflect")!!
            }
        }

        // Milestone days
        when (day) {
            7, 14, 21, 28 -> picks += QuestLibrary.byId("milestone_week")!!
            30 -> picks += QuestLibrary.byId("milestone_final")!!
        }

        // Deduplicate by templateId, cap count by difficulty
        val maxTasks = when {
            easy -> 4
            hard -> 6
            else -> 5
        }
        val unique = picks.distinctBy { it.templateId }.toMutableList()
        // Deterministic shuffle lite
        val s = seed(today.toString() + day, profile)
        unique.sortBy { (it.templateId.hashCode() xor s) }
        return unique.take(maxTasks)
    }

    fun materialize(
        templates: List<QuestTemplate>,
        today: LocalDate,
        profile: QuestProfileInput
    ): List<DailyQuestTask> {
        return templates.map { t ->
            val reason = when {
                t.isFarz -> "Farz — confirm only if you offered it. XP tracks honesty, not worth with Allah."
                t.category == QuestCategory.MILESTONE -> "Weekly checkpoint for habit consistency."
                t.goalTags.any { it in profile.goals } -> "Matched to your selected goals."
                else -> "A balanced step for today's plan."
            }
            DailyQuestTask(
                id = "${today}-$t.templateId".replace(".", ""),
                templateId = t.templateId,
                category = t.category,
                difficulty = t.difficulty,
                minutes = t.minutes,
                title = t.title,
                detail = t.detail,
                xpReward = t.difficulty.xp,
                reason = reason,
                activeDate = today.toString(),
                optional = t.optional,
                isFarz = t.isFarz
            )
        }.map { task ->
            // Fix id construction
            task.copy(id = "${today}-${task.templateId}")
        }
    }
}
