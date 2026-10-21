package com.suhood.feature.salah
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.upsertSalahLog(
                SalahLogEntity(
                    id = "$dateIso-$prayerName",
                    dateIso = dateIso,
                    prayerName = prayerName,
                    status = status.key
                )
            )
            runCatching { com.suhood.widgets.WidgetBridge.refreshAll(getApplication()) }
        }
    }

    fun clearStatus(prayerName: String, dateIso: String = todayIso()) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { dao.deleteSalahLog("$dateIso-$prayerName") }
            runCatching { com.suhood.widgets.WidgetBridge.refreshAll(getApplication()) }
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
        // Local UI state is source of truth while interacting. Only merge non-empty DB emissions
        // so transient empty Flow emissions cannot wipe the list (fixes flicker in the video).
        var localStatus by remember { mutableStateOf<Map<String, SalahStatus?>>(emptyMap()) }
        var hasDbSnapshot by remember { mutableStateOf(false) }
        LaunchedEffect(logs) {
            if (logs.isEmpty() && hasDbSnapshot) return@LaunchedEffect
            if (logs.isNotEmpty()) {
                localStatus = logs.associate { it.prayerName to SalahStatus.fromKey(it.status) }
                hasDbSnapshot = true
            }
        }
        val colors = suhoodColors
        // Cycle: unmarked -> on time -> late -> not prayed -> unmarked
        val cycle = listOf(null, SalahStatus.ON_TIME, SalahStatus.LATE, SalahStatus.NOT_PRAYED)
        val prayerTimes = remember {
            // display-only placeholders; real times come from prayer calculator when wired
            mapOf(
                "Fajr" to "Fajr",
                "Dhuhr" to "Dhuhr",
                "Asr" to "Asr",
                "Maghrib" to "Maghrib",
                "Isha" to "Isha"
            )
        }
        val icons = mapOf(
            "Fajr" to Icons.Filled.WbSunny,
            "Dhuhr" to Icons.Filled.LightMode,
            "Asr" to Icons.Filled.WbTwilight,
            "Maghrib" to Icons.Filled.WbTwilight,
            "Isha" to Icons.Filled.NightsStay
        )
        val recorded = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").count { name ->
            val s = localStatus[name]
            s != null && s != SalahStatus.NOT_PRAYED
        }
        val progress = recorded / 5f

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "Salah",
                    style = SuhoodTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
                    color = colors.textPrimary
                )
            }

            Text(
                "Tap a prayer to cycle its status. Unrecorded prayers aren't counted in history percentages.",
                style = SuhoodTypography.bodySmall,
                color = colors.textMuted,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(18.dp))

            // Ring card
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val animated by animateFloatAsState(progress, tween(1000), label = "salahRing")
                    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(84.dp)) {
                            val stroke = 8.dp.toPx()
                            drawCircle(color = colors.surfaceElevated, style = Stroke(stroke))
                            drawArc(
                                color = colors.accentLavender,
                                startAngle = -90f,
                                sweepAngle = 360f * animated,
                                useCenter = false,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$recorded",
                                style = SuhoodTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                                color = colors.textPrimary
                            )
                            Text("OF 5", style = SuhoodTypography.labelSmall.copy(fontSize = 9.sp), color = colors.textMuted)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text("Today's prayers", style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                recorded == 0 -> "Nothing recorded yet — tap below to begin."
                                recorded == 5 -> "All five prayers recorded. May it be accepted."
                                else -> "$recorded of 5 recorded so far today."
                            },
                            style = SuhoodTypography.bodySmall,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            PRAYER_NAMES.forEach { name ->
                val status = localStatus[name]
                val chipLabel = status?.label ?: "Not marked"
                val chipColor = when (status) {
                    SalahStatus.ON_TIME, SalahStatus.JAMA_AH -> colors.accentLavender
                    SalahStatus.LATE -> colors.accentYellow
                    SalahStatus.NOT_PRAYED -> colors.error
                    null -> colors.textMuted
                }
                val chipBg = when (status) {
                    SalahStatus.ON_TIME, SalahStatus.JAMA_AH -> colors.accentLavender.copy(alpha = 0.15f)
                    SalahStatus.LATE -> colors.accentYellow.copy(alpha = 0.15f)
                    SalahStatus.NOT_PRAYED -> colors.error.copy(alpha = 0.14f)
                    null -> colors.surfaceElevated
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    onClick = {
                        val cur = status
                        val idx = cycle.indexOf(cur).let { if (it < 0) 0 else it }
                        val next = cycle[(idx + 1) % cycle.size]
                        // Instant UI update — never wait for Room
                        localStatus = if (next == null) {
                            localStatus - name
                        } else {
                            localStatus + (name to next)
                        }
                        if (next == null) {
                            vm.clearStatus(name)
                        } else {
                            vm.setStatus(name, next)
                        }
                    },
                    contentPadding = PaddingValues(15.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (status != null && status != SalahStatus.NOT_PRAYED)
                                        colors.accentLavender.copy(alpha = 0.16f)
                                    else colors.surfaceElevated
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icons[name] ?: Icons.Filled.LightMode,
                                null,
                                tint = if (status != null && status != SalahStatus.NOT_PRAYED) colors.accentLavender else colors.accentYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            Text("Tap to cycle status", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(chipBg)
                                .border(
                                    1.dp,
                                    if (status == null) colors.borderSubtle else Color.Transparent,
                                    RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 13.dp, vertical = 7.dp)
                        ) {
                            Text(
                                chipLabel,
                                style = SuhoodTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                color = chipColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onHistory,
                contentPadding = PaddingValues(17.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(colors.accentLavender.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.BarChart, null, tint = colors.accentLavender, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("History & summary", style = SuhoodTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                        Text("Week · month · year · all time", style = SuhoodTypography.labelSmall, color = colors.textMuted)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                }
            }
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
