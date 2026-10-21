package com.suhood.feature.quest

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suhood.app.SuhoodApplication
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.feature.salah.SalahStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Configurable XP rules — not a measure of faith. */
object QuestRules {
    const val XP_SALAH = 3
    const val XP_JUZ = 15
    const val TIER_XP = 50
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
        val idx = tier.coerceIn(0, list.lastIndex)
        return if (tier >= list.size) {
            RankDef("متميز+", "Mutamayyiz+", "Distinguished+", tier)
        } else list[idx]
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
    private val prefs = context.getSharedPreferences("suhood_quest", Context.MODE_PRIVATE)

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
        prefs.edit().putStringSet("awarded_ids", ids).apply()
        appendLedger(XpEvent(eventId, reason, amount, LocalDate.now().toString()))
        return true
    }

    private fun appendLedger(e: XpEvent) {
        val arr = JSONArray(prefs.getString("ledger", "[]"))
        arr.put(JSONObject().put("id", e.id).put("reason", e.reason).put("amount", e.amount).put("day", e.day))
        // keep last 200
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
        // Deterministic generation for the month
        val generated = listOf(
            QuestMission(UUID.nameUUIDFromBytes(("$key-salah").toByteArray()).toString(), "Salah consistency", "Record 20 prayer completions this month", 20, 0, 30, key),
            QuestMission(UUID.nameUUIDFromBytes(("$key-quran").toByteArray()).toString(), "Quran sessions", "Complete 10 reading sessions", 10, 0, 25, key),
            QuestMission(UUID.nameUUIDFromBytes(("$key-dhikr").toByteArray()).toString(), "Dhikr days", "Use Tasbeeh on 7 different days", 7, 0, 20, key)
        )
        val arr = JSONArray()
        generated.forEach {
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
        return generated
    }

    fun updateMissionProgress(id: String, progress: Int) {
        val key = monthKey()
        val list = missions().map {
            if (it.id == id) it.copy(progress = progress.coerceAtMost(it.target)) else it
        }
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
        list.find { it.id == id }?.let { m ->
            if (m.progress >= m.target) {
                tryAward("mission-${m.id}", "Mission: ${m.title}", m.xpReward)
            }
        }
    }
}

suspend fun syncQuestFromSalah(context: Context) {
    val app = context.applicationContext as SuhoodApplication
    val store = QuestStore(context)
    val from = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
    val to = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val logs = withContext(Dispatchers.IO) { app.database.dao().getSalahBetween(from, to) }
    var completed = 0
    logs.forEach { log ->
        if (log.status != SalahStatus.NOT_PRAYED.key) {
            completed++
            store.tryAward(
                eventId = "salah-${log.id}",
                reason = "Salah ${log.prayerName} (${log.dateIso})",
                amount = QuestRules.XP_SALAH
            )
        }
    }
    // Update first mission progress
    val missions = store.missions()
    missions.firstOrNull()?.let { store.updateMissionProgress(it.id, completed) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorshipQuestScreen(onBack: () -> Unit) {
    val colors = suhoodColors
    val context = LocalContext.current
    val store = remember { QuestStore(context) }
    var totalXp by remember { mutableIntStateOf(store.totalXp) }
    var missions by remember { mutableStateOf(store.missions()) }
    var ledger by remember { mutableStateOf(store.ledger()) }

    LaunchedEffect(Unit) {
        syncQuestFromSalah(context)
        totalXp = store.totalXp
        missions = store.missions()
        ledger = store.ledger()
    }

    val tier = totalXp / QuestRules.TIER_XP
    val tierProgress = totalXp % QuestRules.TIER_XP
    val rank = Ranks.forTier(tier)

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Worship Quest", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "XP tracks recorded app activity only. It does not measure faith or acceptance of worship.",
                style = SuhoodTypography.labelSmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = colors.accentLavender, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(rank.arabic, style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        Text("${rank.latin} — ${rank.meaning}", color = colors.textSecondary)
                        Text("Tier $tier · $totalXp XP", style = SuhoodTypography.labelMedium, color = colors.accentMint)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { tierProgress / QuestRules.TIER_XP.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = colors.accentLavender,
                    trackColor = colors.surfaceElevated
                )
                Text(
                    "$tierProgress / ${QuestRules.TIER_XP} XP to next tier",
                    style = SuhoodTypography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Rules", style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Text("Salah completion +${QuestRules.XP_SALAH} XP · Juz +${QuestRules.XP_JUZ} XP · ${QuestRules.TIER_XP} XP per tier",
                style = SuhoodTypography.labelSmall, color = colors.textMuted)
            Spacer(Modifier.height(16.dp))
            Text("Monthly missions (${store.monthKey()})", style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            missions.forEach { m ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), contentPadding = PaddingValues(14.dp)) {
                    Text(m.title, style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    Text(m.detail, style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    LinearProgressIndicator(
                        progress = { m.progress / m.target.toFloat().coerceAtLeast(1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                        color = colors.accentMint,
                        trackColor = colors.surfaceElevated
                    )
                    Text("${m.progress}/${m.target} · +${m.xpReward} XP", style = SuhoodTypography.labelSmall, color = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Recent XP", style = SuhoodTypography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            ledger.take(15).forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(e.reason, style = SuhoodTypography.labelSmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
                    Text("+${e.amount}", style = SuhoodTypography.labelSmall, color = colors.accentMint)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}


@Composable
fun WorshipQuestHomeCard(onOpen: () -> Unit) {
    val context = LocalContext.current
    val colors = suhoodColors
    val store = remember { QuestStore(context) }
    var xp by remember { mutableIntStateOf(store.totalXp) }
    var tierProgress by remember { mutableFloatStateOf(0f) }
    var rankLatin by remember { mutableStateOf("Mubtadi’") }
    var rankAr by remember { mutableStateOf("مبتدئ") }
    LaunchedEffect(Unit) {
        while (true) {
            xp = store.totalXp
            val tier = xp / QuestRules.TIER_XP
            val into = xp % QuestRules.TIER_XP
            tierProgress = into / QuestRules.TIER_XP.toFloat()
            val r = Ranks.forTier(tier)
            rankLatin = r.latin
            rankAr = r.arabic
            kotlinx.coroutines.delay(2000)
        }
    }
    GlassCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, null, tint = colors.accentMint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Worship Quest", style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                Text(
                    "$rankAr · $rankLatin · $xp XP",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { tierProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = colors.accentMint,
                    trackColor = colors.borderSubtle
                )
            }
        }
    }
}
