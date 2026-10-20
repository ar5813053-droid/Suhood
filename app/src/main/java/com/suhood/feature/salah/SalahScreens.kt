package com.suhood.feature.salah

import android.app.Application
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suhood.app.SuhoodApplication
import com.suhood.core.database.SalahLogEntity
import com.suhood.core.designsystem.components.GlassCard
import com.suhood.core.designsystem.components.ScreenHeader
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.suhoodColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class SalahStatus(val key: String, val label: String) {
    JAMA_AH("JAMA_AH", "In jamaah"),
    ON_TIME("ON_TIME", "On time"),
    LATE("LATE", "Late"),
    NOT_PRAYED("NOT_PRAYED", "Not prayed");

    companion object {
        fun fromKey(key: String): SalahStatus =
            entries.find { it.key == key } ?: NOT_PRAYED
    }
}

val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

class SalahViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as SuhoodApplication).database.dao()
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayIso(): String = LocalDate.now().format(fmt)

    fun observeToday() = dao.observeSalahForDate(todayIso())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeAll() = dao.observeAllSalahLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setStatus(prayerName: String, status: SalahStatus, dateIso: String = todayIso()) {
        viewModelScope.launch {
            dao.upsertSalahLog(
                SalahLogEntity(
                    id = "$dateIso-$prayerName",
                    dateIso = dateIso,
                    prayerName = prayerName,
                    status = status.key
                )
            )
        }
    }
}

object SalahStats {
    /**
     * Percentages among *recorded* prayers only.
     * Unrecorded prayers are excluded (not counted as Not prayed).
     */
    data class Summary(
        val totalRecorded: Int,
        val jamaahPct: Int,
        val onTimePct: Int,
        val latePct: Int,
        val notPrayedPct: Int,
        val completedOfFiveToday: Int
    )

    fun summarize(logs: List<SalahLogEntity>, todayIso: String): Summary {
        val total = logs.size
        if (total == 0) {
            return Summary(0, 0, 0, 0, 0, 0)
        }
        fun pct(key: String) = ((logs.count { it.status == key } * 100.0) / total).toInt()
        val todayDone = logs.filter { it.dateIso == todayIso && it.status != SalahStatus.NOT_PRAYED.key }
            .map { it.prayerName }.distinct().size
        return Summary(
            totalRecorded = total,
            jamaahPct = pct(SalahStatus.JAMA_AH.key),
            onTimePct = pct(SalahStatus.ON_TIME.key),
            latePct = pct(SalahStatus.LATE.key),
            notPrayedPct = pct(SalahStatus.NOT_PRAYED.key),
            completedOfFiveToday = todayDone
        )
    }
}

object SalahScreens {
    @Composable
    fun Today(onBack: () -> Unit, onHistory: () -> Unit) {
        val context = LocalContext.current
        val vm: SalahViewModel = viewModel(
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return SalahViewModel(context.applicationContext as Application) as T
                }
            }
        )
        val logs by vm.observeToday().collectAsState()
        val byPrayer = logs.associateBy { it.prayerName }
        var sheetPrayer by remember { mutableStateOf<String?>(null) }
        val colors = suhoodColors
        val todayDone = logs.count { it.status != SalahStatus.NOT_PRAYED.key }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ScreenHeader(title = "Salah", onBack = onBack)
            Spacer(Modifier.height(8.dp))
            Text(
                "Mark each prayer. Unrecorded prayers are not counted in history percentages.",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "$todayDone / 5 recorded today",
                    style = SuhoodTypography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    "Tap a prayer to set status",
                    style = SuhoodTypography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(16.dp))
            PRAYER_NAMES.forEach { name ->
                val status = byPrayer[name]?.let { SalahStatus.fromKey(it.status) }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    onClick = { sheetPrayer = name }
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                        Text(
                            status?.label ?: "Not marked",
                            style = SuhoodTypography.labelMedium,
                            color = if (status == null) colors.textMuted else colors.accentLavender
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onHistory) {
                Text("History & summary", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                Text("Week · month · year · all time", style = SuhoodTypography.bodySmall, color = colors.textMuted)
            }
        }

        sheetPrayer?.let { prayer ->
            StatusSheet(
                prayer = prayer,
                onDismiss = { sheetPrayer = null },
                onSelect = { st ->
                    vm.setStatus(prayer, st)
                    sheetPrayer = null
                }
            )
        }
    }

    @Composable
    private fun StatusSheet(
        prayer: String,
        onDismiss: () -> Unit,
        onSelect: (SalahStatus) -> Unit
    ) {
        val colors = suhoodColors
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.72f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}
                    .padding(20.dp)
            ) {
                Text("How did you pray $prayer?", style = SuhoodTypography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(12.dp))
                SalahStatus.entries.forEach { st ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        onClick = { onSelect(st) }
                    ) {
                        Text(st.label, style = SuhoodTypography.titleSmall, color = colors.textPrimary)
                    }
                }
            }
        }
    }

    @Composable
    fun History(onBack: () -> Unit) {
        val context = LocalContext.current
        val vm: SalahViewModel = viewModel(
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return SalahViewModel(context.applicationContext as Application) as T
                }
            }
        )
        val all by vm.observeAll().collectAsState()
        var period by remember { mutableStateOf("Weeks") }
        val colors = suhoodColors
        val today = vm.todayIso()
        val filtered = remember(all, period) {
            val now = LocalDate.now()
            when (period) {
                "Weeks" -> all.filter {
                    runCatching { LocalDate.parse(it.dateIso).isAfter(now.minusDays(7).minusDays(1)) }.getOrDefault(false)
                }
                "Months" -> all.filter {
                    runCatching { LocalDate.parse(it.dateIso).isAfter(now.minusDays(30).minusDays(1)) }.getOrDefault(false)
                }
                "Years" -> all.filter {
                    runCatching { LocalDate.parse(it.dateIso).isAfter(now.minusDays(365).minusDays(1)) }.getOrDefault(false)
                }
                else -> all
            }
        }
        val summary = SalahStats.summarize(filtered, today)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ScreenHeader(title = "Salah history", onBack = onBack)
            Spacer(Modifier.height(8.dp))
            Text(
                "Percentages use recorded prayers only (${summary.totalRecorded} logs in range).",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Weeks", "Months", "Years", "All time").forEach { p ->
                    val sel = period == p
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) colors.accentLavender.copy(alpha = 0.2f) else colors.surface)
                            .border(1.dp, if (sel) colors.accentLavender else colors.borderSubtle, RoundedCornerShape(20.dp))
                            .clickable { period = p }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(p, style = SuhoodTypography.labelMedium, color = if (sel) colors.accentLavender else colors.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("STATUS SUMMARY", style = SuhoodTypography.labelSmall, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("In jamaah", "${summary.jamaahPct}%", Modifier.weight(1f))
                StatCard("On time", "${summary.onTimePct}%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Late", "${summary.latePct}%", Modifier.weight(1f))
                StatCard("Not prayed", "${summary.notPrayedPct}%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Text("Recent logs", style = SuhoodTypography.titleSmall, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Text("No records yet — mark prayers on the Salah screen.", color = colors.textMuted)
            } else {
                filtered.sortedByDescending { it.dateIso }.take(40).forEach { log ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(
                            "${log.dateIso} · ${log.prayerName}",
                            style = SuhoodTypography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            SalahStatus.fromKey(log.status).label,
                            style = SuhoodTypography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
        val colors = suhoodColors
        GlassCard(modifier = modifier) {
            Text(title, style = SuhoodTypography.labelSmall, color = colors.textMuted)
            Text(value, style = SuhoodTypography.headlineSmall, color = colors.textPrimary)
        }
    }
}
