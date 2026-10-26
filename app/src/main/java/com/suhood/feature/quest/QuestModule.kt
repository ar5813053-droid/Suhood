package com.suhood.feature.quest

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.SuhoodApplication
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.feature.onboarding.OnboardingProfileStore
import com.suhood.feature.salah.SalahStatus
import com.suhood.widgets.WidgetBridge
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Legacy XP constants kept for Salah/Juz awards. */
object QuestRules {
    const val XP_SALAH = 3
    const val XP_JUZ = 15
    const val TIER_XP = QuestTierRules.TIER_XP_TARGET
}

data class RankDef(val arabic: String, val latin: String, val meaning: String, val minTier: Int)

object Ranks {
    val list = listOf(
        RankDef("مبتدئ", "Mubtadi’", "Beginner", 0),
        RankDef("مجتهد", "Mujtahid", "Striving", 1),
        RankDef("مثابر", "Muthābir", "Persistent", 2),
        RankDef("متقدم", "Mutaqaddim", "Advanced", 3),
        RankDef("متميز", "Mutamayyiz", "Distinguished", 4)
    )

    fun forTier(tier: Int): RankDef {
        return if (tier >= list.size) {
            RankDef("متميز+", "Mutamayyiz+", "Distinguished+", tier)
        } else list[tier.coerceIn(0, list.lastIndex)]
    }
}

data class QuestMission(
    val id: String,
    val title: String,
    val detail: String,
    val target: Int,
    val progress: Int,
    val xpReward: Int,
    val monthKey: String
)

data class XpEvent(val id: String, val reason: String, val amount: Int, val day: String)

class QuestStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("suhood_quest", Context.MODE_PRIVATE)

    var totalXp: Int
        get() = prefs.getInt("total_xp", 0)
        set(v) = prefs.edit().putInt("total_xp", v).apply()

    private fun awardedIds(): MutableSet<String> =
        prefs.getStringSet("awarded_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

    fun awardSalahIfNew(dateIso: String, prayerName: String): Boolean =
        tryAward("salah-$dateIso-$prayerName", "Salah $prayerName", QuestRules.XP_SALAH)

    fun awardJuzIfNew(juzNumber: Int, day: String = LocalDate.now().toString()): Boolean =
        tryAward("juz-$day-$juzNumber", "Completed Juz $juzNumber", QuestRules.XP_JUZ)

    fun tryAward(eventId: String, reason: String, amount: Int): Boolean {
        val ids = awardedIds()
        if (eventId in ids) return false
        ids += eventId
        totalXp += amount
        prefs.edit().putStringSet("awarded_ids", ids).commit()
        appendLedger(XpEvent(eventId, reason, amount, LocalDate.now().toString()))
        return true
    }

    private fun appendLedger(e: XpEvent) {
        val arr = JSONArray(prefs.getString("ledger", "[]"))
        arr.put(JSONObject().put("id", e.id).put("reason", e.reason).put("amount", e.amount).put("day", e.day))
        while (arr.length() > 200) arr.remove(0)
        prefs.edit().putString("ledger", arr.toString()).apply()
    }

    fun ledger(): List<XpEvent> {
        val arr = JSONArray(prefs.getString("ledger", "[]"))
        return buildList {
            for (i in arr.length() - 1 downTo 0) {
                val o = arr.getJSONObject(i)
                add(XpEvent(o.getString("id"), o.getString("reason"), o.getInt("amount"), o.getString("day")))
            }
        }
    }

    fun monthKey(): String = YearMonth.now().toString()

    fun missions(): List<QuestMission> {
        val key = monthKey()
        val raw = prefs.getString("missions_$key", null)
        if (raw != null) {
            val arr = JSONArray(raw)
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        QuestMission(
                            o.getString("id"),
                            o.getString("title"),
                            o.getString("detail"),
                            o.getInt("target"),
                            o.getInt("progress"),
                            o.getInt("xp"),
                            key
                        )
                    )
                }
            }
        }
        val defaults = listOf(
            QuestMission(UUID(), "Pray on time this month", "Mark Salah as on time", 20, 0, 30, key),
            QuestMission(UUID(), "Steady Quran", "Complete Juz sittings", 4, 0, 40, key)
        )
        saveMissions(defaults)
        return defaults
    }

    private fun UUID() = java.util.UUID.randomUUID().toString()

    private fun saveMissions(list: List<QuestMission>) {
        val key = monthKey()
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("detail", it.detail)
                    .put("target", it.target)
                    .put("progress", it.progress)
                    .put("xp", it.xpReward)
            )
        }
        prefs.edit().putString("missions_$key", arr.toString()).apply()
    }

    fun updateMissionProgress(id: String, progress: Int) {
        val list = missions().map {
            if (it.id == id) it.copy(progress = progress.coerceAtMost(it.target)) else it
        }
        saveMissions(list)
    }

    // ─── 7-day tier attempt ───────────────────────────────────────────

    fun ensureTierAttempt(today: LocalDate = LocalDate.now()): TierAttemptState {
        val todayEp = today.toEpochDay()
        val existing = readAttempt()
        val next = when {
            existing == null ->
                TierAttemptLogic.startAttempt(prefs.getInt("tier_index", 0), todayEp)
            existing.completed ->
                // Fresh 7-day window at the unlocked tier index
                TierAttemptLogic.startAttempt(prefs.getInt("tier_index", existing.tierIndex), todayEp)
            else ->
                TierAttemptLogic.onDayRollover(existing, todayEp)
        }
        writeAttempt(next)
        return next
    }

    private fun readAttempt(): TierAttemptState? {
        if (!prefs.contains("attempt_start")) return null
        return TierAttemptState(
            tierIndex = prefs.getInt("tier_index", 0),
            startEpochDay = prefs.getLong("attempt_start", 0L),
            deadlineEpochDay = prefs.getLong("attempt_deadline", 0L),
            creditedXp = prefs.getInt("attempt_xp", 0),
            completed = prefs.getBoolean("attempt_done", false)
        )
    }

    private fun writeAttempt(s: TierAttemptState) {
        prefs.edit()
            .putInt("tier_index", s.tierIndex)
            .putLong("attempt_start", s.startEpochDay)
            .putLong("attempt_deadline", s.deadlineEpochDay)
            .putInt("attempt_xp", s.creditedXp)
            .putBoolean("attempt_done", s.completed)
            .commit()
    }

    fun dailyXp(date: LocalDate = LocalDate.now()): Int =
        prefs.getInt("daily_xp_${date}", 0)

    private fun setDailyXp(date: LocalDate, value: Int) {
        prefs.edit().putInt("daily_xp_$date", value).commit()
    }

    fun recentTemplateIds(limit: Int = 12): List<String> {
        val arr = JSONArray(prefs.getString("recent_templates", "[]"))
        return buildList {
            for (i in 0 until arr.length()) add(arr.getString(i))
        }.takeLast(limit)
    }

    private fun pushRecentTemplates(ids: List<String>) {
        val cur = recentTemplateIds(20).toMutableList()
        ids.forEach {
            cur.remove(it)
            cur.add(it)
        }
        val arr = JSONArray()
        cur.takeLast(20).forEach { arr.put(it) }
        prefs.edit().putString("recent_templates", arr.toString()).apply()
    }

    fun todayTasks(profile: QuestProfileInput, today: LocalDate = LocalDate.now()): List<DailyQuestTask> {
        ensureTierAttempt(today)
        val key = "tasks_$today"
        val raw = prefs.getString(key, null)
        if (raw != null) {
            return parseTasks(raw)
        }
        val generated = DailyQuestEngine.generate(profile, today, recentTemplateIds())
        saveTasks(today, generated)
        pushRecentTemplates(generated.map { it.templateId })
        return generated
    }

    private fun parseTasks(raw: String): List<DailyQuestTask> {
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    DailyQuestTask(
                        id = o.getString("id"),
                        templateId = o.getString("templateId"),
                        category = QuestCategory.valueOf(o.getString("category")),
                        difficulty = QuestDifficulty.valueOf(o.getString("difficulty")),
                        minutes = o.getInt("minutes"),
                        title = o.getString("title"),
                        detail = o.getString("detail"),
                        xpReward = o.getInt("xp"),
                        reason = o.optString("reason"),
                        activeDate = o.getString("date"),
                        completed = o.optBoolean("completed", false)
                    )
                )
            }
        }
    }

    private fun saveTasks(date: LocalDate, tasks: List<DailyQuestTask>) {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("templateId", t.templateId)
                    .put("category", t.category.name)
                    .put("difficulty", t.difficulty.name)
                    .put("minutes", t.minutes)
                    .put("title", t.title)
                    .put("detail", t.detail)
                    .put("xp", t.xpReward)
                    .put("reason", t.reason)
                    .put("date", t.activeDate)
                    .put("completed", t.completed)
            )
        }
        prefs.edit().putString("tasks_$date", arr.toString()).commit()
    }

    /**
     * Complete a daily task once. Returns credited tier XP (0 if duplicate or capped).
     */
    @Synchronized
    fun completeDailyTask(taskId: String, today: LocalDate = LocalDate.now()): Int {
        var attempt = ensureTierAttempt(today)
        if (attempt.completed) return 0
        if (QuestTierRules.isExpired(today.toEpochDay(), attempt.deadlineEpochDay)) {
            attempt = TierAttemptLogic.onDayRollover(attempt, today.toEpochDay())
            writeAttempt(attempt)
        }
        val tasks = todayTasks(
            // Profile only needed if regenerating; tasks already persisted
            QuestProfileInput(),
            today
        ).toMutableList()
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx < 0) return 0
        val task = tasks[idx]
        if (task.completed) return 0
        if (task.activeDate != today.toString()) return 0

        val daily = dailyXp(today)
        val credit = QuestTierRules.creditAllowed(task.xpReward, daily)
        tasks[idx] = task.copy(completed = true)
        saveTasks(today, tasks)

        // Always mark completion; tier XP only if room under daily cap
        // Persist completion first
        val ids = awardedIds()
        ids += "daily-$taskId"
        prefs.edit().putStringSet("awarded_ids", ids).commit()

        if (credit > 0) {
            setDailyXp(today, daily + credit)
            val updated = TierAttemptLogic.afterCredit(attempt, credit)
            writeAttempt(updated)
            if (updated.completed && !attempt.completed) {
                // Advance tier index; next open starts a fresh 7-day attempt via ensureTierAttempt
                prefs.edit()
                    .putInt("tier_index", updated.tierIndex + 1)
                    .remove("attempt_start")
                    .commit()
            }
            totalXp += credit
            appendLedger(XpEvent("daily-$taskId", task.title, credit, today.toString()))
        }
        return credit
    }

    /** True for each day of the current 7-day attempt that earned any daily XP. */
    fun dayActivityFlags(attempt: TierAttemptState, today: LocalDate = LocalDate.now()): List<Boolean> {
        return (0 until QuestTierRules.ATTEMPT_DAYS).map { offset ->
            val d = LocalDate.ofEpochDay(attempt.startEpochDay + offset)
            when {
                d.isAfter(today) -> false
                d == today -> dailyXp(d) > 0 || todayTasks(QuestProfileInput(), d).any { it.completed }
                else -> dailyXp(d) > 0
            }
        }
    }

    fun profileFromOnboarding(context: Context): QuestProfileInput {
        val o = OnboardingProfileStore.get(context)
        return QuestProfileInput(
            ageGroup = o.ageGroup,
            lifestyle = o.dailyLife,
            goals = o.goals,
            consistency = o.consistency,
            hardestPrayer = o.hardestPrayer,
            practice = o.practice,
            dailyMinutes = o.dailyMinutes.takeIf { it > 0 } ?: o.quranMinutes,
            reminderStyle = o.reminderStyle,
            language = o.language,
            phoneUsage = o.phoneUsage,
            freeTimeHint = o.socialUsage
        )
    }
}

fun syncQuestFromSalah(context: Context) {
    val store = QuestStore(context)
    val app = context.applicationContext as? SuhoodApplication
    // Best-effort: if Salah statuses exist on disk via onboarding prefs keys, no-op safe
    store.ensureTierAttempt()
}

@Composable
fun WorshipQuestScreen(onBack: () -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    val store = remember { QuestStore(context) }
    val today = remember { LocalDate.now() }
    var attempt by remember { mutableStateOf(store.ensureTierAttempt(today)) }
    var tasks by remember {
        mutableStateOf(store.todayTasks(store.profileFromOnboarding(context), today))
    }
    var dailyXp by remember { mutableIntStateOf(store.dailyXp(today)) }
    var message by remember { mutableStateOf<String?>(null) }
    val dayFlags = remember(attempt, tasks, dailyXp) { store.dayActivityFlags(attempt, today) }
    val dayIndex = (today.toEpochDay() - attempt.startEpochDay).toInt().coerceIn(0, 6)
    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

    LaunchedEffect(Unit) {
        syncQuestFromSalah(context)
        QuestReminderScheduler.schedule(context)
        attempt = store.ensureTierAttempt(today)
        tasks = store.todayTasks(store.profileFromOnboarding(context), today)
        dailyXp = store.dailyXp(today)
    }

    val progressTarget = (attempt.creditedXp.toFloat() / QuestTierRules.TIER_XP_TARGET).coerceIn(0f, 1f)
    val progressAnim by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "tierProgress"
    )
    val accent = Color(0xFF8B7CF8)
    val green = Color(0xFF5DE0A0)
    val glass = colors.surfaceElevated.copy(alpha = 0.92f)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text("Worship Quest", style = SuhoodTypography.titleLarge, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                Text("Small steps · Steady growth", color = colors.textSecondary, fontSize = 13.sp)
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tier card — HTML liquid glass
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(glass)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        buildAnnotatedStringSafe("Tier ${attempt.tierIndex + 1}", " · Consistency"),
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accent.copy(alpha = 0.18f))
                            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("Day ${dayIndex + 1} of 7", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Weekly tier progress", color = colors.textMuted, fontSize = 12.sp)
                    Text(
                        "${attempt.creditedXp} / ${QuestTierRules.TIER_XP_TARGET} XP",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF8B7CF8), Color(0xFFC084FC))))
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    dayLetters.forEachIndexed { i, letter ->
                        val past = i < dayIndex && dayFlags.getOrElse(i) { false }
                        val isToday = i == dayIndex
                        val bg = when {
                            past -> green.copy(alpha = 0.1f)
                            isToday -> accent.copy(alpha = 0.14f)
                            else -> Color.White.copy(alpha = 0.04f)
                        }
                        val borderC = when {
                            past -> green.copy(alpha = 0.25f)
                            isToday -> accent.copy(alpha = 0.35f)
                            else -> Color.White.copy(alpha = 0.08f)
                        }
                        val letterC = when {
                            past -> green
                            isToday -> accent
                            else -> colors.textMuted
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(1.dp, borderC, RoundedCornerShape(12.dp))
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(letter, color = letterC, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when {
                                    past -> "✓"
                                    isToday -> "●"
                                    else -> "·"
                                },
                                color = letterC,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Text("TODAY'S QUESTS", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)

            tasks.forEach { task ->
                val done = task.completed
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(glass)
                        .border(
                            1.dp,
                            if (done) green.copy(alpha = 0.2f) else colors.borderSubtle,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable(enabled = !done) {
                            val credit = store.completeDailyTask(task.id, today)
                            tasks = store.todayTasks(store.profileFromOnboarding(context), today)
                            attempt = store.ensureTierAttempt(today)
                            dailyXp = store.dailyXp(today)
                            val allDone = tasks.all { it.completed }
                            QuestNotify.onTaskCompleted(context, task.title, credit, allDone)
                            if (attempt.completed) {
                                QuestNotify.onTierCompleted(context, attempt.tierIndex)
                            }
                            message = when {
                                credit > 0 -> "+$credit XP toward this tier"
                                else -> "Marked complete"
                            }
                            WidgetBridge.refreshAll(context)
                        }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (done) green.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                .border(
                                    1.5.dp,
                                    if (done) green.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done) {
                                Icon(Icons.Filled.Check, null, tint = green, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetaChip(task.category.name.lowercase().replace('_', ' '))
                                MetaChip("${task.minutes} min")
                                MetaChip("+${task.xpReward} XP", accent = true)
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(task.detail, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                            if (task.reason.isNotBlank()) {
                                Text(task.reason, color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            message?.let { Text(it, color = accent, fontSize = 13.sp) }
            Text(
                "XP tracks habit consistency inside Suhood — not spiritual rank.",
                color = colors.textMuted,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MetaChip(text: String, accent: Boolean = false) {
    val colors = suhoodColors
    val accentC = Color(0xFF8B7CF8)
    Text(
        text,
        color = if (accent) accentC else colors.textMuted,
        fontSize = 11.sp,
        fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (accent) accentC.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (accent) accentC.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

private fun buildAnnotatedStringSafe(bold: String, rest: String): String = bold + rest

@Composable
fun WorshipQuestHomeCard(onOpen: () -> Unit) {
    val context = LocalContext.current
    val store = remember { QuestStore(context) }
    val today = remember { LocalDate.now() }
    val attempt = store.ensureTierAttempt(today)
    val dayFlags = store.dayActivityFlags(attempt, today)
    val dayIndex = (today.toEpochDay() - attempt.startEpochDay).toInt().coerceIn(0, 6)
    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
    val progress = (attempt.creditedXp.toFloat() / QuestTierRules.TIER_XP_TARGET).coerceIn(0f, 1f)
    val badgePurple = Color(0xFF9B8CFF)
    val green = Color(0xFF5DE0A0)
    // Screenshot-matched palette: dark glass card, grey/white progress glow
    val cardTop = Color(0xFF25262D)
    val cardBottom = Color(0xFF1A1B20)
    val titleWhite = Color(0xFFF0F0F2)
    val titleMuted = Color(0xFF9A9BA3)
    val labelMuted = Color(0xFF7A7B84)
    val track = Color(0xFF3A3B42)
    val barStart = Color(0xFF9A9BA3)
    val barEnd = Color(0xFFE8E9EF)
    val knob = Color(0xFFF5F5F7)

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.65f),
                spotColor = Color.White.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(cardTop, cardBottom)))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tier ${attempt.tierIndex + 1}",
                    color = titleWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    " · Consistency",
                    color = titleMuted,
                    fontWeight = FontWeight.Normal,
                    fontSize = 19.sp,
                    letterSpacing = (-0.2).sp
                )
            }
            Box(
                Modifier
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = badgePurple.copy(alpha = 0.40f),
                        spotColor = badgePurple.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgePurple.copy(alpha = 0.18f))
                    .border(1.dp, badgePurple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "Day ${dayIndex + 1} of 7",
                    color = badgePurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Weekly tier progress", color = labelMuted, fontSize = 12.5.sp)
            Text(
                "${attempt.creditedXp} / ${QuestTierRules.TIER_XP_TARGET} XP",
                color = titleWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = (-0.2).sp
            )
        }
        Spacer(Modifier.height(10.dp))

        // Grey + white glow progress (matches screenshot structure, grey fill per request)
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(track)
            )
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceAtLeast(0.02f))
                    .height(7.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = Color.White.copy(alpha = 0.35f),
                        spotColor = Color.White.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(barStart, barEnd)))
            )
            // Glowing knob at tip
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceAtLeast(0.02f))
                    .height(14.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier
                        .size(14.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = Color.White.copy(alpha = 0.55f),
                            spotColor = Color.White.copy(alpha = 0.85f)
                        )
                        .clip(CircleShape)
                        .background(knob)
                        .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            dayLetters.forEachIndexed { i, letter ->
                val past = i < dayIndex && dayFlags.getOrElse(i) { false }
                val isToday = i == dayIndex
                val bg = when {
                    past -> green.copy(alpha = 0.12f)
                    isToday -> badgePurple.copy(alpha = 0.14f)
                    else -> Color.White.copy(alpha = 0.04f)
                }
                val borderC = when {
                    past -> green.copy(alpha = 0.28f)
                    isToday -> badgePurple.copy(alpha = 0.40f)
                    else -> Color.White.copy(alpha = 0.08f)
                }
                val letterC = when {
                    past -> green
                    isToday -> badgePurple
                    else -> titleMuted.copy(alpha = 0.55f)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .then(
                            if (isToday) Modifier.shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = badgePurple.copy(alpha = 0.35f),
                                spotColor = badgePurple.copy(alpha = 0.45f)
                            ) else Modifier
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(1.dp, borderC, RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(letter, color = letterC, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            past -> "✓"
                            isToday -> "●"
                            else -> "·"
                        },
                        color = letterC,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


/** Local notifications for quest milestones (emoji + gentle copy). */
object QuestNotify {
    fun onTaskCompleted(context: Context, title: String, credit: Int, allDone: Boolean) {
        if (allDone) {
            post(context, 92001, "✨ All set for today", "Every quest is done. Rest well — you showed up.")
        } else if (credit > 0) {
            post(context, 92002, "✅ Quest complete", "$title · +$credit XP")
        }
    }

    fun onTierCompleted(context: Context, tierIndex: Int) {
        post(context, 92003, "🏆 Tier ${tierIndex + 1} complete", "Consistency built this. A fresh tier awaits when you're ready.")
    }

    fun onTierReset(context: Context) {
        post(context, 92004, "🔄 New week, fresh intention", "Your tier attempt reset. Begin again at your own pace.")
    }

    fun post(context: Context, id: Int, title: String, body: String) {
        val app = context.applicationContext
        val n = NotificationCompat.Builder(app, SuhoodApplication.CHANNEL_FOCUS)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(app).notify(id, n) }
    }
}

/** Gentle quest reminders — independent of Salah alarms. */
object QuestReminderScheduler {
    private const val REQ_MORNING = 91001
    private const val REQ_EVENING = 91002
    private const val REQ_WEEKEND = 91003

    fun schedule(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduleAt(app, am, REQ_MORNING, 8, 0, QuestReminderReceiver.ACTION_MORNING)
        scheduleAt(app, am, REQ_EVENING, 19, 30, QuestReminderReceiver.ACTION_EVENING)
        // Sunday evening gentle reset reminder
        scheduleWeekend(app, am)
    }

    private fun scheduleWeekend(app: Context, am: AlarmManager) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 20)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.WEEK_OF_YEAR, 1)
        }
        val pi = PendingIntent.getBroadcast(
            app, REQ_WEEKEND,
            Intent(app, QuestReminderReceiver::class.java).setAction(QuestReminderReceiver.ACTION_WEEKEND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                @Suppress("DEPRECATION")
                am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        }
    }

    private fun scheduleAt(
        app: Context,
        am: AlarmManager,
        req: Int,
        hour: Int,
        minute: Int,
        action: String
    ) {
        val pi = PendingIntent.getBroadcast(
            app,
            req,
            Intent(app, QuestReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                @Suppress("DEPRECATION")
                am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        }
    }
}

class QuestReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val store = QuestStore(app)
        val attempt = store.ensureTierAttempt()
        when (intent?.action) {
            ACTION_EVENING -> {
                val tasks = store.todayTasks(store.profileFromOnboarding(app))
                if (tasks.any { !it.completed }) {
                    QuestNotify.post(app, 92010, "🌙 Still time today", "A gentle reminder: you still have time for today's chosen quest.")
                }
            }
            ACTION_WEEKEND -> {
                QuestNotify.onTierReset(app)
            }
            ACTION_DEADLINE -> {
                QuestNotify.post(app, 92011, "⏳ Tier attempt ending", "Your tier attempt ends soon. Continue at your own pace.")
            }
            else -> {
                QuestNotify.post(app, 92012, "🌅 Quest ready", "Your daily Worship Quest is ready. Start with one small step.")
            }
        }
        QuestReminderScheduler.schedule(app)
        WidgetBridge.refreshAll(app)
    }

    companion object {
        const val ACTION_MORNING = "com.suhood.quest.MORNING"
        const val ACTION_EVENING = "com.suhood.quest.EVENING"
        const val ACTION_DEADLINE = "com.suhood.quest.DEADLINE"
        const val ACTION_WEEKEND = "com.suhood.quest.WEEKEND"
    }
}

