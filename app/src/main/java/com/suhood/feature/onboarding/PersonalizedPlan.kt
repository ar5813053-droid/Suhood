package com.suhood.feature.onboarding

/**
 * Deterministic, offline personalization engine.
 * Same answers always produce the same plan; different answers change relevant sections.
 */
data class PlanAction(
    val id: String,
    val title: String,
    val detail: String,
    val feature: String, // salah | focus | quran | dhikr | setup | habit
    val reason: String
)

data class PersonalizedPlan(
    val mainFocus: String,
    val mainFocusReason: String,
    val dailyRoutine: List<String>,
    val salahSupport: List<String>,
    val distractionStrategy: List<String>,
    val worshipRoutine: List<String>,
    val firstDayActions: List<PlanAction>,
    val weeklyProgression: List<String>
) {
    fun toSummaryLines(): List<String> = buildList {
        add(mainFocus)
        addAll(dailyRoutine.take(3))
        addAll(firstDayActions.map { it.title }.take(3))
    }
}

object PersonalizedPlanEngine {

    fun generate(answers: OnboardingAnswers): PersonalizedPlan {
        val goals = answers.goals
        val mainGoal = pickMainGoal(goals)
        val mainFocus = when (mainGoal) {
            "salah" -> "Primary focus: build a steadier Salah rhythm"
            "quran" -> "Primary focus: a small, daily Quran habit"
            "dhikr" -> "Primary focus: short, frequent Dhikr"
            "focus" -> "Primary focus: protect attention around Salah"
            "routine" -> "Primary focus: one calm daily routine you can keep"
            else -> "Primary focus: show up for one small act of worship each day"
        }
        val mainReason = when {
            goals.isEmpty() -> "No goal selected — starting with the gentlest default."
            goals.size == 1 -> "You chose a single goal: ${labelGoal(mainGoal)}."
            else -> "Based on your goals: ${goals.joinToString { labelGoal(it) }}."
        }

        val intensity = when (answers.reminderStyle.lowercase()) {
            "gentle", "soft" -> "gentle"
            "strong", "firm" -> "strong"
            else -> "balanced"
        }

        val salahSupport = buildList {
            if ("salah" in goals || goals.isEmpty()) {
                val hard = answers.hardestPrayer.ifBlank { "one difficult prayer" }
                add("Protect $hard first — the prayer you marked as hardest.")
                when (intensity) {
                    "gentle" -> add("Use soft reminders a few minutes before Salah, without pressure.")
                    "strong" -> add("Use clear, timely Salah alerts so the window is hard to miss.")
                    else -> add("Use balanced Salah reminders tied to your real prayer times.")
                }
                when (answers.consistency.lowercase()) {
                    "rarely", "struggling", "inconsistent" ->
                        add("This week: aim for on-time only for $hard; let other prayers stay flexible.")
                    "often", "mostly" ->
                        add("Keep your current streak and add a 2-minute buffer before each Salah.")
                    else ->
                        add("Anchor one prayer to a fixed daily cue (after waking, after work, or before bed).")
                }
            } else {
                add("Salah is not your top selected goal — keep existing prayer times, no extra pressure.")
            }
        }

        val distraction = buildList {
            val apps = answers.selectedAppLabels
            val highUse = answers.phoneUsage.contains("6") || answers.phoneUsage.contains("8") ||
                answers.phoneUsage.contains("heavy") || answers.disttime.contains("evening") ||
                answers.disttime.contains("night")
            if ("focus" in goals || apps.isNotEmpty()) {
                if (apps.isNotEmpty()) {
                    add(
                        "During Salah windows, limit: ${apps.take(4).joinToString()}" +
                            if (apps.size > 4) " +${apps.size - 4} more" else ""
                    )
                } else {
                    add("Choose 1–3 apps in Focus → Apps so Salah lock can protect your prayer window.")
                }
                if (highUse) {
                    add("You reported high phone pull — block only during Salah first, not all day.")
                } else {
                    add("Salah-time app limits only; full-day blocking is optional later.")
                }
                add("Enable Accessibility + Usage Access when you are ready — never forced silently.")
            } else {
                add("No distraction goal selected — Focus blocking stays optional.")
            }
        }

        val worship = buildList {
            val wantsQuran = "quran" in goals || answers.practice == "quran" || answers.practice == "both"
            val wantsDhikr = "dhikr" in goals || answers.practice == "dhikr" || answers.practice == "both"
            val mins = answers.dailyMinutes.coerceIn(5, 60)
            when {
                wantsQuran && wantsDhikr -> {
                    add("Daily target: about $mins minutes split between Quran and Dhikr.")
                    add("Example: ${mins / 2} min Quran + ${mins - mins / 2} min Dhikr after a fixed Salah.")
                }
                wantsQuran -> add("Daily target: $mins minutes of Quran — stop when the timer ends.")
                wantsDhikr -> add("Daily target: $mins minutes of Dhikr / tasbeeh, even if only 33 counts.")
                else -> add("No Quran/Dhikr goal selected — worship extras stay optional.")
            }
            if (answers.lifestyle.contains("Student", true) || answers.lifestyle.contains("student", true)) {
                add("Fit the target after class or between study blocks, not late at night.")
            }
            if (answers.lifestyle.contains("Work", true) || answers.lifestyle.contains("job", true)) {
                add("Attach the target to a commute or post-work Salah so it survives busy days.")
            }
        }

        val routine = buildList {
            add("Morning: one intentional prayer with phone away for the first minutes.")
            if ("focus" in goals || answers.selectedAppLabels.isNotEmpty()) {
                add("Around Salah: leave selected apps until the prayer window ends.")
            }
            if ("quran" in goals || answers.practice == "quran" || answers.practice == "both") {
                add("Open Quran Companion for your daily minutes — same slot each day.")
            }
            if ("dhikr" in goals || answers.practice == "dhikr" || answers.practice == "both") {
                add("Close the day with a short Dhikr count before sleep.")
            }
            if (answers.consistency.lowercase() in setOf("rarely", "struggling", "inconsistent", "")) {
                add("Keep the routine under 20 minutes total until it feels automatic.")
            }
        }

        val firstDay = buildList {
            add(
                PlanAction(
                    id = "day1_salah",
                    title = "Pray your next Salah with phone in another room",
                    detail = "One prayer, full presence.",
                    feature = "salah",
                    reason = "Builds the core habit without a large commitment."
                )
            )
            if (answers.selectedAppLabels.isNotEmpty() || "focus" in goals) {
                add(
                    PlanAction(
                        id = "day1_focus_setup",
                        title = "Review Focus → Apps list",
                        detail = "Confirm which apps should pause during Salah.",
                        feature = "focus",
                        reason = "Uses your selected distracting apps."
                    )
                )
            }
            if ("quran" in goals || answers.practice == "quran" || answers.practice == "both") {
                add(
                    PlanAction(
                        id = "day1_quran",
                        title = "Read Quran for ${minOf(answers.dailyMinutes, 10)} minutes",
                        detail = "Stop when the time ends — consistency over volume.",
                        feature = "quran",
                        reason = "Matches your Quran goal and time target."
                    )
                )
            }
            if ("dhikr" in goals || answers.practice == "dhikr" || answers.practice == "both") {
                add(
                    PlanAction(
                        id = "day1_dhikr",
                        title = "Complete one tasbeeh of 33",
                        detail = "Any quiet moment after Salah.",
                        feature = "dhikr",
                        reason = "Matches your Dhikr goal."
                    )
                )
            }
            add(
                PlanAction(
                    id = "day1_permission",
                    title = "Turn on notifications for Salah",
                    detail = "Required for reliable reminders; other permissions stay optional until needed.",
                    feature = "setup",
                    reason = "Reminders need notification permission on modern Android."
                )
            )
        }

        val weekly = buildList {
            add("Days 1–2: only first-day actions — no new features.")
            add("Days 3–4: keep Salah reminders on; protect your hardest prayer.")
            if ("focus" in goals || answers.selectedAppLabels.isNotEmpty()) {
                add("Days 5–6: try Salah-time app limits if Accessibility is enabled.")
            }
            add("Day 7: review what felt heavy; reduce targets before adding more.")
        }

        return PersonalizedPlan(
            mainFocus = mainFocus,
            mainFocusReason = mainReason,
            dailyRoutine = routine,
            salahSupport = salahSupport,
            distractionStrategy = distraction,
            worshipRoutine = worship,
            firstDayActions = firstDay,
            weeklyProgression = weekly
        )
    }

    private fun pickMainGoal(goals: Set<String>): String {
        val order = listOf("salah", "focus", "quran", "dhikr", "routine")
        return order.firstOrNull { it in goals } ?: goals.firstOrNull().orEmpty()
    }

    private fun labelGoal(id: String): String = when (id) {
        "salah" -> "Consistent Salah"
        "quran" -> "More Quran"
        "dhikr" -> "Daily Dhikr"
        "focus" -> "Reduce distractions"
        "routine" -> "Daily routine"
        else -> id
    }
}

/** Snapshot of answers used by the plan engine (testable, no Android deps). */
data class OnboardingAnswers(
    val goals: Set<String> = emptySet(),
    val hardestPrayer: String = "",
    val consistency: String = "",
    val reminderStyle: String = "Balanced",
    val phoneUsage: String = "",
    val disttime: String = "",
    val selectedAppLabels: List<String> = emptyList(),
    val practice: String = "both", // quran | dhikr | both | none
    val dailyMinutes: Int = 10,
    val lifestyle: String = "",
    val language: String = "English"
)

object OnboardingValidation {
    fun goalsValid(goals: Set<String>) = goals.isNotEmpty()
    fun aboutValid(age: String, lifestyle: String) = age.isNotBlank() && lifestyle.isNotBlank()
    fun habitsValid(phone: String, dist: String) = phone.isNotBlank() && dist.isNotBlank()
    fun salahValid(consistency: String, prayer: String) = consistency.isNotBlank() && prayer.isNotBlank()
    fun practiceValid(practice: String, minutes: Int) =
        practice.isNotBlank() && minutes in 5..60
}
