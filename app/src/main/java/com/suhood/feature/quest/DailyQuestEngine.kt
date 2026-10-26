package com.suhood.feature.quest

import java.time.LocalDate
import java.util.Locale

/** Difficulty bands for fixed XP (habit points — not spiritual rank). */
enum class QuestDifficulty(val xp: Int) {
    EASY(10),
    MEDIUM(20),
    CHALLENGING(30)
}

enum class QuestCategory {
    SALAH, QURAN, DHIKR, LEARNING, CHARACTER, SERVICE, REFLECTION, PHONE_HABITS
}

/**
 * Template for a possible daily task. Text is non-claiming and never invents scripture.
 */
data class QuestTemplate(
    val templateId: String,
    val category: QuestCategory,
    val difficulty: QuestDifficulty,
    val minutes: Int,
    val title: String,
    val detail: String,
    /** Goal keys this task supports: salah, quran, dhikr, focus, routine */
    val goalTags: Set<String> = emptySet()
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
    val completed: Boolean = false
)

data class QuestProfileInput(
    val ageGroup: String = "",
    val lifestyle: String = "",
    val goals: Set<String> = emptySet(),
    val consistency: String = "",
    val hardestPrayer: String = "",
    val practice: String = "both",
    val dailyMinutes: Int = 10,
    val reminderStyle: String = "Gentle",
    val language: String = "English",
    val phoneUsage: String = "",
    val freeTimeHint: String = "" // morning / afternoon / evening / night
)

data class TierAttemptState(
    val tierIndex: Int,
    val startEpochDay: Long,
    val deadlineEpochDay: Long,
    val creditedXp: Int,
    val completed: Boolean
)

/** Central, tunable constants for the 7-day tier system. */
object QuestTierRules {
    const val TIER_XP_TARGET = 700
    const val DAILY_XP_CAP = 100
    const val ATTEMPT_DAYS = 7
    const val XP_EASY = 10
    const val XP_MEDIUM = 20
    const val XP_CHALLENGING = 30

    fun deadlineEpochDay(startEpochDay: Long): Long = startEpochDay + (ATTEMPT_DAYS - 1)

    fun daysRemaining(todayEpochDay: Long, deadlineEpochDay: Long): Int =
        (deadlineEpochDay - todayEpochDay + 1).toInt().coerceAtLeast(0)

    fun isExpired(todayEpochDay: Long, deadlineEpochDay: Long): Boolean =
        todayEpochDay > deadlineEpochDay

    /** How much of [taskXp] can still be credited today given [dailyCredited]. */
    fun creditAllowed(taskXp: Int, dailyCredited: Int): Int {
        val room = (DAILY_XP_CAP - dailyCredited).coerceAtLeast(0)
        return taskXp.coerceAtMost(room)
    }
}

/**
 * Deterministic, local-first daily quest generator.
 * Same profile + date + recentTemplateIds ⇒ same plan.
 */
object DailyQuestEngine {

    private val catalog: List<QuestTemplate> = listOf(
        QuestTemplate("salah_prepare", QuestCategory.SALAH, QuestDifficulty.EASY, 5,
            "Prepare for the next Salah", "Set a calm space and make wudu before the next prayer window.", setOf("salah", "routine")),
        QuestTemplate("salah_on_time", QuestCategory.SALAH, QuestDifficulty.MEDIUM, 10,
            "Pray one Salah with presence", "Choose one prayer today and give it full attention from start to end.", setOf("salah")),
        QuestTemplate("salah_hardest", QuestCategory.SALAH, QuestDifficulty.CHALLENGING, 15,
            "Protect your hardest prayer", "Plan ahead for the prayer you find most difficult and protect that time.", setOf("salah")),
        QuestTemplate("quran_short", QuestCategory.QURAN, QuestDifficulty.EASY, 5,
            "Read Quran for a few minutes", "Open the Mushaf or app and read steadily for a short sitting.", setOf("quran")),
        QuestTemplate("quran_focus", QuestCategory.QURAN, QuestDifficulty.MEDIUM, 10,
            "Focused Quran sitting", "Read without switching apps — one short, intentional session.", setOf("quran", "focus")),
        QuestTemplate("quran_review", QuestCategory.QURAN, QuestDifficulty.MEDIUM, 10,
            "Review a familiar passage", "Re-read a short portion you already know and notice one meaning.", setOf("quran")),
        QuestTemplate("dhikr_morning", QuestCategory.DHIKR, QuestDifficulty.EASY, 5,
            "Morning remembrance", "Say a short set of dhikr after waking or after Fajr.", setOf("dhikr", "routine")),
        QuestTemplate("dhikr_count", QuestCategory.DHIKR, QuestDifficulty.MEDIUM, 8,
            "One full tasbeeh", "Complete one count of 33 with focus using the Tasbeeh tool if you like.", setOf("dhikr")),
        QuestTemplate("learn_hadith", QuestCategory.LEARNING, QuestDifficulty.EASY, 5,
            "Read one Hadith selection", "Open Hadith in Suhood and read one narration carefully.", setOf("routine")),
        QuestTemplate("learn_reflect", QuestCategory.LEARNING, QuestDifficulty.MEDIUM, 10,
            "Learn one small lesson", "From Quran, Hadith, or a Suhood Book page — note one takeaway.", setOf("quran")),
        QuestTemplate("character_kind", QuestCategory.CHARACTER, QuestDifficulty.EASY, 5,
            "One intentional kindness", "Offer a kind word, patience, or help to someone today.", setOf("routine")),
        QuestTemplate("character_truth", QuestCategory.CHARACTER, QuestDifficulty.MEDIUM, 5,
            "Guard your speech", "Avoid one unnecessary harsh or idle remark today.", setOf("routine")),
        QuestTemplate("service_home", QuestCategory.SERVICE, QuestDifficulty.EASY, 10,
            "Help at home", "Do one helpful task at home without being asked.", setOf("routine")),
        QuestTemplate("reflect_gratitude", QuestCategory.REFLECTION, QuestDifficulty.EASY, 5,
            "Three gratitude notes", "Quietly name three blessings from today.", setOf("routine", "dhikr")),
        QuestTemplate("reflect_intention", QuestCategory.REFLECTION, QuestDifficulty.EASY, 3,
            "Renew your intention", "Pause once today and renew why you seek consistency.", setOf("salah", "routine")),
        QuestTemplate("phone_pause", QuestCategory.PHONE_HABITS, QuestDifficulty.MEDIUM, 10,
            "Phone-free prayer window", "Keep distracting apps closed around one Salah window.", setOf("focus", "salah")),
        QuestTemplate("phone_limit", QuestCategory.PHONE_HABITS, QuestDifficulty.CHALLENGING, 15,
            "Short focus block", "Take a 15-minute break from social apps and return with intention.", setOf("focus"))
    )

    fun generate(
        profile: QuestProfileInput,
        date: LocalDate,
        recentTemplateIds: List<String> = emptyList()
    ): List<DailyQuestTask> {
        val daySeed = date.toEpochDay()
        val goals = profile.goals.map { it.lowercase(Locale.US) }.toSet().ifEmpty {
            setOf("salah", "routine")
        }
        val budgetMinutes = when {
            profile.dailyMinutes <= 5 -> 15
            profile.dailyMinutes <= 10 -> 25
            profile.dailyMinutes <= 20 -> 35
            else -> 45
        }
        val beginner = profile.consistency in setOf("starting", "working", "Just starting", "Working on it") ||
            profile.ageGroup in setOf("under18", "Under 18")

        val scored = catalog.map { t ->
            var score = 1
            // Goal alignment
            score += t.goalTags.count { it in goals } * 3
            // Lifestyle / time
            if (beginner && t.difficulty == QuestDifficulty.CHALLENGING) score -= 3
            if (!beginner && t.difficulty == QuestDifficulty.CHALLENGING) score += 1
            if (profile.practice == "quran" && t.category == QuestCategory.QURAN) score += 2
            if (profile.practice == "dhikr" && t.category == QuestCategory.DHIKR) score += 2
            if ("focus" in goals && t.category == QuestCategory.PHONE_HABITS) score += 2
            if (profile.hardestPrayer.isNotBlank() && t.templateId == "salah_hardest") score += 3
            // Avoid recent
            if (t.templateId in recentTemplateIds) score -= 4
            // Deterministic jitter from date + template
            score += (((daySeed * 31) + t.templateId.hashCode()) % 5).toInt()
            t to score
        }.sortedByDescending { it.second }

        val chosen = mutableListOf<QuestTemplate>()
        var minutes = 0
        val categoriesUsed = mutableSetOf<QuestCategory>()
        for ((t, _) in scored) {
            if (chosen.size >= 5) break
            if (minutes + t.minutes > budgetMinutes && chosen.size >= 3) break
            if (t.category in categoriesUsed && chosen.size >= 3) continue
            chosen += t
            categoriesUsed += t.category
            minutes += t.minutes
            if (chosen.size >= 3 && minutes >= budgetMinutes.coerceAtMost(30)) break
        }
        while (chosen.size < 3) {
            val next = catalog.firstOrNull { c -> chosen.none { it.templateId == c.templateId } } ?: break
            chosen += next
        }

        val dateIso = date.toString()
        return chosen.mapIndexed { index, t ->
            val reason = buildReason(t, profile)
            DailyQuestTask(
                id = "$dateIso-${t.templateId}",
                templateId = t.templateId,
                category = t.category,
                difficulty = t.difficulty,
                minutes = t.minutes,
                title = t.title,
                detail = t.detail,
                xpReward = t.difficulty.xp,
                reason = reason,
                activeDate = dateIso,
                completed = false
            )
        }
    }

    private fun buildReason(t: QuestTemplate, profile: QuestProfileInput): String {
        return when {
            t.templateId == "salah_hardest" && profile.hardestPrayer.isNotBlank() ->
                "Suggested because you marked ${profile.hardestPrayer} as hardest."
            "focus" in profile.goals.map { it.lowercase(Locale.US) } && t.category == QuestCategory.PHONE_HABITS ->
                "Suggested to support your focus goals."
            t.category == QuestCategory.QURAN && profile.practice in setOf("quran", "both") ->
                "Aligned with your Quran practice preference."
            t.category == QuestCategory.DHIKR && profile.practice in setOf("dhikr", "both") ->
                "Aligned with your Dhikr preference."
            else -> "A balanced step for today's plan."
        }
    }
}

/**
 * Pure helpers for tier attempt transitions (testable).
 */
object TierAttemptLogic {
    fun startAttempt(tierIndex: Int, todayEpochDay: Long): TierAttemptState =
        TierAttemptState(
            tierIndex = tierIndex,
            startEpochDay = todayEpochDay,
            deadlineEpochDay = QuestTierRules.deadlineEpochDay(todayEpochDay),
            creditedXp = 0,
            completed = false
        )

    fun afterCredit(
        state: TierAttemptState,
        credit: Int
    ): TierAttemptState {
        if (state.completed || credit <= 0) return state
        val next = (state.creditedXp + credit).coerceAtMost(QuestTierRules.TIER_XP_TARGET)
        return state.copy(
            creditedXp = next,
            completed = next >= QuestTierRules.TIER_XP_TARGET
        )
    }

    fun onDayRollover(state: TierAttemptState, todayEpochDay: Long): TierAttemptState {
        if (state.completed) return state
        return if (QuestTierRules.isExpired(todayEpochDay, state.deadlineEpochDay)) {
            startAttempt(state.tierIndex, todayEpochDay)
        } else state
    }
}
