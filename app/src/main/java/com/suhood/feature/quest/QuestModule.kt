package com.suhood.feature.quest

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suhood.app.SuhoodApplication
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.feature.onboarding.OnboardingProfileStore
import com.suhood.feature.salah.SalahStatus
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

    LaunchedEffect(Unit) {
        syncQuestFromSalah(context)
        QuestReminderScheduler.schedule(context)
        attempt = store.ensureTierAttempt(today)
        tasks = store.todayTasks(store.profileFromOnboarding(context), today)
        dailyXp = store.dailyXp(today)
    }

    val rank = Ranks.forTier(attempt.tierIndex)
    val daysLeft = QuestTierRules.daysRemaining(today.toEpochDay(), attempt.deadlineEpochDay)
    val progress = (attempt.creditedXp.toFloat() / QuestTierRules.TIER_XP_TARGET).coerceIn(0f, 1f)
    val capReached = dailyXp >= QuestTierRules.DAILY_XP_CAP

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onBack)
            )
            Spacer(Modifier.size(12.dp))
            Text("Worship Quest", style = SuhoodTypography.titleLarge, color = colors.textPrimary)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Your daily plan — small steps, steady growth.",
                color = colors.textSecondary,
                fontSize = 14.sp
            )

            // Tier card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = colors.accentLavender)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${rank.latin} · ${rank.arabic}",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tier ${attempt.tierIndex + 1} · ${rank.meaning}",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = colors.accentLavender,
                    trackColor = colors.borderSubtle
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${attempt.creditedXp} / ${QuestTierRules.TIER_XP_TARGET} XP this attempt",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (attempt.completed) "Tier complete — next tier unlocks on a fresh attempt."
                    else "$daysLeft day(s) left in this 7-day attempt · Daily cap ${dailyXp}/${QuestTierRules.DAILY_XP_CAP}",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                if (capReached) {
                    Text(
                        "Daily XP cap reached — you can still complete tasks for the habit, without extra tier XP today.",
                        color = colors.accentMint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (attempt.completed) {
                    Text(
                        "Well done. Consistency built this tier — not speed.",
                        color = colors.accentMint,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Text("TODAY'S QUESTS", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

            tasks.forEach { task ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                        .clickable(enabled = !task.completed) {
                            val credit = store.completeDailyTask(task.id, today)
                            tasks = store.todayTasks(store.profileFromOnboarding(context), today)
                            attempt = store.ensureTierAttempt(today)
                            // Re-read after possible tier complete side effects
                            attempt = store.ensureTierAttempt(today)
                            dailyXp = store.dailyXp(today)
                            message = when {
                                credit > 0 -> "+$credit XP toward this tier"
                                task.completed -> null
                                else -> "Marked complete. Daily tier XP cap may apply."
                            }
                            // refresh task completed state
                            tasks = store.todayTasks(store.profileFromOnboarding(context), today)
                        }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (task.completed) colors.accentMint else Color.Transparent)
                                .border(1.5.dp, if (task.completed) colors.accentMint else colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.completed) {
                                Icon(Icons.Filled.Check, null, tint = Color(0xFF0A0E12), modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                "${task.category.name.lowercase().replace('_', ' ')} · ${task.minutes} min · ${task.xpReward} XP",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(task.detail, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    if (task.reason.isNotBlank()) {
                        Text(task.reason, color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            message?.let {
                Text(it, color = colors.accentLavender, fontSize = 13.sp)
            }

            Text(
                "XP here tracks habit consistency inside Suhood — not spiritual rank.",
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun WorshipQuestHomeCard(onOpen: () -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    val store = remember { QuestStore(context) }
    val attempt = store.ensureTierAttempt()
    val rank = Ranks.forTier(attempt.tierIndex)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp)
    ) {
        Text("Worship Quest", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
        Text(
            "${rank.latin} · ${attempt.creditedXp}/${QuestTierRules.TIER_XP_TARGET} XP · 7-day attempt",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}

/** Gentle quest reminders — independent of Salah alarms. */
object QuestReminderScheduler {
    private const val REQ_MORNING = 91001
    private const val REQ_EVENING = 91002

    fun schedule(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduleAt(app, am, REQ_MORNING, 8, 0, QuestReminderReceiver.ACTION_MORNING)
        scheduleAt(app, am, REQ_EVENING, 19, 30, QuestReminderReceiver.ACTION_EVENING)
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
        val text = when (intent?.action) {
            ACTION_EVENING -> "A gentle reminder: you still have time for today's chosen quest."
            ACTION_DEADLINE -> "Your tier attempt ends soon. Continue at your own pace."
            else -> "Your daily Worship Quest is ready. Start with one small step."
        }
        val n = NotificationCompat.Builder(app, SuhoodApplication.CHANNEL_FOCUS)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Worship Quest")
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(app).notify(text.hashCode() and 0xFFFF, n) }
        // Reschedule next day
        QuestReminderScheduler.schedule(app)
    }

    companion object {
        const val ACTION_MORNING = "com.suhood.quest.MORNING"
        const val ACTION_EVENING = "com.suhood.quest.EVENING"
        const val ACTION_DEADLINE = "com.suhood.quest.DEADLINE"
    }
}
