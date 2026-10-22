package com.suhood.feature.phone

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

enum class NumeralStyle { WESTERN, ARABIC_INDIC, PERSIAN }

object PhoneNumberUtils {
    private val arabicIndic = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
    private val persian = charArrayOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')

    fun normalize(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            when (c) {
                in '0'..'9' -> sb.append(c)
                in arabicIndic -> sb.append(('0'.code + arabicIndic.indexOf(c)).toChar())
                in persian -> sb.append(('0'.code + persian.indexOf(c)).toChar())
                '+', '*', '#', ',' -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun display(normalized: String, style: NumeralStyle): String {
        if (style == NumeralStyle.WESTERN) return normalized
        val map = if (style == NumeralStyle.ARABIC_INDIC) arabicIndic else persian
        return buildString {
            for (c in normalized) {
                if (c in '0'..'9') append(map[c - '0']) else append(c)
            }
        }
    }

    fun isValidForDial(normalized: String): Boolean =
        normalized.isNotBlank() && normalized.any { it.isDigit() }

    /** Compare two numbers ignoring spaces and leading zeros after country code heuristic. */
    fun matches(a: String, b: String): Boolean {
        val na = normalize(a).filter { it.isDigit() || it == '+' }
        val nb = normalize(b).filter { it.isDigit() || it == '+' }
        if (na.isEmpty() || nb.isEmpty()) return false
        if (na == nb) return true
        val da = na.filter { it.isDigit() }
        val db = nb.filter { it.isDigit() }
        return da.isNotEmpty() && db.isNotEmpty() && (da.endsWith(db) || db.endsWith(da))
    }
}

enum class CallThemeId(val label: String, val category: String) {
    MIDNIGHT_CRESCENT("Midnight Crescent", "Incoming"),
    SILVER_GLASS("Silver Glass", "Incoming"),
    EMERALD_HERITAGE("Emerald Heritage", "Incoming"),
    SACRED_GEOMETRY("Sacred Geometry", "Patterns"),
    ISLAMIC_ARCH("Islamic Arch", "Architecture"),
    PALESTINE_SOLIDARITY("Palestine Solidarity", "Flags"),
    MASJID_SERENITY("Masjid Serenity", "Architecture"),
    DESERT_NIGHT("Desert Night", "Landscapes"),
    MODERN_ARABESQUE("Modern Arabesque", "Patterns"),
    MINIMAL_MONOCHROME("Minimal Monochrome", "Minimal"),
    RAMADAN_NIGHT("Ramadan Night", "Seasonal"),
    HERITAGE_MOSAIC("Heritage Mosaic", "Patterns"),
    SAUDI_GREEN("Saudi Green", "Flags"),
    PAKISTAN_CRESCENT("Pakistan Crescent", "Flags"),
    INDIA_TRICOLOUR("India Tricolour Glass", "Flags"),
    IRAN_GEOMETRIC("Iran Geometric", "Flags")
}

data class CallThemeColors(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val text: Color,
    val secondary: Color
)

object CallThemes {
    fun colors(id: CallThemeId): CallThemeColors = when (id) {
        CallThemeId.MIDNIGHT_CRESCENT -> CallThemeColors(Color(0xFF08090B), Color(0xFF111316), Color(0xFFC7CBD1), Color(0xFFF5F6F7), Color(0xFFA8ADB5))
        CallThemeId.SILVER_GLASS -> CallThemeColors(Color(0xFF0C0D10), Color(0xFF1A1C20), Color(0xFFE4E6EA), Color(0xFFF5F6F7), Color(0xFFA8ADB5))
        CallThemeId.EMERALD_HERITAGE -> CallThemeColors(Color(0xFF06140F), Color(0xFF0E2219), Color(0xFF2ECC71), Color(0xFFF5F6F7), Color(0xFFA8D5B5))
        CallThemeId.SACRED_GEOMETRY -> CallThemeColors(Color(0xFF0A0B12), Color(0xFF16182A), Color(0xFFB9A7FF), Color(0xFFF5F6F7), Color(0xFFAAA5B8))
        CallThemeId.ISLAMIC_ARCH -> CallThemeColors(Color(0xFF101218), Color(0xFF1C2030), Color(0xFFD4AF37), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.PALESTINE_SOLIDARITY -> CallThemeColors(Color(0xFF0A0A0A), Color(0xFF151515), Color(0xFF009736), Color(0xFFF5F6F7), Color(0xFFCE1126))
        CallThemeId.MASJID_SERENITY -> CallThemeColors(Color(0xFF0B1218), Color(0xFF152028), Color(0xFF7EB8D4), Color(0xFFF5F6F7), Color(0xFFA8ADB5))
        CallThemeId.DESERT_NIGHT -> CallThemeColors(Color(0xFF120E0A), Color(0xFF1E1812), Color(0xFFE0A96D), Color(0xFFF5F6F7), Color(0xFFC7B299))
        CallThemeId.MODERN_ARABESQUE -> CallThemeColors(Color(0xFF0D0D12), Color(0xFF1A1A24), Color(0xFF9A82F5), Color(0xFFF5F6F7), Color(0xFFAAA5B8))
        CallThemeId.MINIMAL_MONOCHROME -> CallThemeColors(Color(0xFF000000), Color(0xFF141414), Color(0xFFFFFFFF), Color(0xFFF5F6F7), Color(0xFF888888))
        CallThemeId.RAMADAN_NIGHT -> CallThemeColors(Color(0xFF0A0F1A), Color(0xFF152030), Color(0xFFE0C36A), Color(0xFFF5F6F7), Color(0xFFB9A7FF))
        CallThemeId.HERITAGE_MOSAIC -> CallThemeColors(Color(0xFF101018), Color(0xFF1C1C28), Color(0xFF4ECDC4), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.SAUDI_GREEN -> CallThemeColors(Color(0xFF004D28), Color(0xFF0A3D28), Color(0xFFFFFFFF), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.PAKISTAN_CRESCENT -> CallThemeColors(Color(0xFF01411C), Color(0xFF0A2E1A), Color(0xFFFFFFFF), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.INDIA_TRICOLOUR -> CallThemeColors(Color(0xFF0B0B0B), Color(0xFF1A1A1A), Color(0xFFFF9933), Color(0xFFF5F6F7), Color(0xFF138808))
        CallThemeId.IRAN_GEOMETRIC -> CallThemeColors(Color(0xFF0A1A12), Color(0xFF142820), Color(0xFFDA0000), Color(0xFFF5F6F7), Color(0xFFFFFFFF))
    }

    fun allCategories(): List<String> = CallThemeId.entries.map { it.category }.distinct()
}

class PhonePrefs(context: Context) {
    private val p: SharedPreferences =
        context.applicationContext.getSharedPreferences("suhood_phone", Context.MODE_PRIVATE)

    var phoneEnabled: Boolean
        get() = p.getBoolean("phone_enabled", true)
        set(v) = p.edit().putBoolean("phone_enabled", v).apply()

    var numeralStyle: NumeralStyle
        get() = runCatching { NumeralStyle.valueOf(p.getString("numerals", NumeralStyle.WESTERN.name)!!) }
            .getOrDefault(NumeralStyle.WESTERN)
        set(v) = p.edit().putString("numerals", v.name).apply()

    var incomingTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_in", CallThemeId.MIDNIGHT_CRESCENT.name)!!) }
            .getOrDefault(CallThemeId.MIDNIGHT_CRESCENT)
        set(v) = p.edit().putString("theme_in", v.name).apply()

    var dialPadTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_pad", CallThemeId.SILVER_GLASS.name)!!) }
            .getOrDefault(CallThemeId.SILVER_GLASS)
        set(v) = p.edit().putString("theme_pad", v.name).apply()

    var ongoingTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_on", CallThemeId.EMERALD_HERITAGE.name)!!) }
            .getOrDefault(CallThemeId.EMERALD_HERITAGE)
        set(v) = p.edit().putString("theme_on", v.name).apply()

    var favoriteThemes: Set<String>
        get() = p.getStringSet("fav_themes", emptySet()) ?: emptySet()
        set(v) = p.edit().putStringSet("fav_themes", v).apply()

    fun toggleFavoriteTheme(id: CallThemeId) {
        val s = favoriteThemes.toMutableSet()
        if (!s.add(id.name)) s.remove(id.name)
        favoriteThemes = s
    }

    fun blocked(): MutableSet<String> =
        p.getStringSet("blocked", emptySet())?.map { PhoneNumberUtils.normalize(it) }?.toMutableSet()
            ?: mutableSetOf()

    fun block(number: String) {
        val n = PhoneNumberUtils.normalize(number)
        if (n.isBlank()) return
        val s = blocked(); s += n; p.edit().putStringSet("blocked", s).apply()
    }

    fun unblock(number: String) {
        val n = PhoneNumberUtils.normalize(number)
        val s = blocked(); s.remove(n); p.edit().putStringSet("blocked", s).apply()
    }

    fun isBlocked(number: String): Boolean {
        val n = PhoneNumberUtils.normalize(number)
        return blocked().any { PhoneNumberUtils.matches(it, n) }
    }

    var quietHoursEnabled: Boolean
        get() = p.getBoolean("quiet", false)
        set(v) = p.edit().putBoolean("quiet", v).apply()

    var quietStartHour: Int
        get() = p.getInt("quiet_start", 22)
        set(v) = p.edit().putInt("quiet_start", v).apply()

    var quietEndHour: Int
        get() = p.getInt("quiet_end", 6)
        set(v) = p.edit().putInt("quiet_end", v).apply()

    fun isQuietNow(hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): Boolean {
        if (!quietHoursEnabled) return false
        val s = quietStartHour
        val e = quietEndHour
        return if (s <= e) hour in s until e else hour >= s || hour < e
    }

    var appLockEnabled: Boolean
        get() = p.getBoolean("app_lock", false)
        set(v) = p.edit().putBoolean("app_lock", v).apply()

    var appLockPin: String
        get() = p.getString("app_lock_pin", "") ?: ""
        set(v) = p.edit().putString("app_lock_pin", v).apply()

    /** Preferred subscription id for dual-SIM dial, -1 = system default */
    var preferredSimId: Int
        get() = p.getInt("sim_id", -1)
        set(v) = p.edit().putInt("sim_id", v).apply()

    /** Per-contact theme overrides: normalized number -> theme name */
    fun contactTheme(number: String): CallThemeId? {
        val key = "ct_${PhoneNumberUtils.normalize(number)}"
        val name = p.getString(key, null) ?: return null
        return runCatching { CallThemeId.valueOf(name) }.getOrNull()
    }

    fun setContactTheme(number: String, theme: CallThemeId?) {
        val key = "ct_${PhoneNumberUtils.normalize(number)}"
        if (theme == null) p.edit().remove(key).apply()
        else p.edit().putString(key, theme.name).apply()
    }
}

object PhoneAssetManifest {
    const val NOTE =
        "Themes are original Suhood Hybrid Glass colours and geometric motifs. " +
            "Flag themes use public national colours as abstract gradients only — not official emblems. " +
            "No third-party wallpapers bundled. No Quranic text on call controls."
}
