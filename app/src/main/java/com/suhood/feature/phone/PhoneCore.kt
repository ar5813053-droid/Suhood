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
                in arabicIndic -> sb.append('0' + arabicIndic.indexOf(c))
                in persian -> sb.append('0' + persian.indexOf(c))
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
        CallThemeId.SILVER_GLASS -> CallThemeColors(Color(0xFF0C0D10), Color(0xFF1A1C20), Color(0xFFE8EAED), Color(0xFFF5F6F7), Color(0xFFB0B4BA))
        CallThemeId.EMERALD_HERITAGE -> CallThemeColors(Color(0xFF06120C), Color(0xFF0E1F16), Color(0xFF3DDC97), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.SACRED_GEOMETRY -> CallThemeColors(Color(0xFF0A0B12), Color(0xFF15182A), Color(0xFFB9A7FF), Color(0xFFF5F6F7), Color(0xFFA8ADB5))
        CallThemeId.ISLAMIC_ARCH -> CallThemeColors(Color(0xFF0B1014), Color(0xFF162028), Color(0xFFD4AF37), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.PALESTINE_SOLIDARITY -> CallThemeColors(Color(0xFF0A0F0A), Color(0xFF121A12), Color(0xFFCE1126), Color(0xFFF5F6F7), Color(0xFFA8ADB5))
        CallThemeId.MASJID_SERENITY -> CallThemeColors(Color(0xFF0A1218), Color(0xFF142430), Color(0xFF5B9BD5), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.DESERT_NIGHT -> CallThemeColors(Color(0xFF120E0A), Color(0xFF1E1810), Color(0xFFE0A84A), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.MODERN_ARABESQUE -> CallThemeColors(Color(0xFF0C0A12), Color(0xFF18141F), Color(0xFF9A82F5), Color(0xFFF5F6F7), Color(0xFFA8ADB5))
        CallThemeId.MINIMAL_MONOCHROME -> CallThemeColors(Color(0xFF000000), Color(0xFF141414), Color(0xFFFFFFFF), Color(0xFFF5F6F7), Color(0xFF888888))
        CallThemeId.RAMADAN_NIGHT -> CallThemeColors(Color(0xFF0A0C18), Color(0xFF141828), Color(0xFFE0C36A), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.HERITAGE_MOSAIC -> CallThemeColors(Color(0xFF0E1014), Color(0xFF1A1E28), Color(0xFF6FCF97), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.SAUDI_GREEN -> CallThemeColors(Color(0xFF003D29), Color(0xFF005C3A), Color(0xFFFFFFFF), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.PAKISTAN_CRESCENT -> CallThemeColors(Color(0xFF01411C), Color(0xFF0A5C2E), Color(0xFFFFFFFF), Color(0xFFF5F6F7), Color(0xFFC7CBD1))
        CallThemeId.INDIA_TRICOLOUR -> CallThemeColors(Color(0xFF0B0A10), Color(0xFF1A1520), Color(0xFFFF9933), Color(0xFFF5F6F7), Color(0xFF138808))
        CallThemeId.IRAN_GEOMETRIC -> CallThemeColors(Color(0xFF0A1210), Color(0xFF142018), Color(0xFFDA0000), Color(0xFFF5F6F7), Color(0xFF239F40))
    }
}

class PhonePrefs(context: Context) {
    private val p: SharedPreferences =
        context.getSharedPreferences("suhood_phone", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = p.getBoolean("enabled", false)
        set(v) = p.edit().putBoolean("enabled", v).apply()

    var numeralStyle: NumeralStyle
        get() = runCatching { NumeralStyle.valueOf(p.getString("numerals", NumeralStyle.WESTERN.name)!!) }.getOrDefault(NumeralStyle.WESTERN)
        set(v) = p.edit().putString("numerals", v.name).apply()

    var incomingTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_in", CallThemeId.MIDNIGHT_CRESCENT.name)!!) }.getOrDefault(CallThemeId.MIDNIGHT_CRESCENT)
        set(v) = p.edit().putString("theme_in", v.name).apply()

    var dialPadTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_pad", CallThemeId.SILVER_GLASS.name)!!) }.getOrDefault(CallThemeId.SILVER_GLASS)
        set(v) = p.edit().putString("theme_pad", v.name).apply()

    var ongoingTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_on", CallThemeId.EMERALD_HERITAGE.name)!!) }.getOrDefault(CallThemeId.EMERALD_HERITAGE)
        set(v) = p.edit().putString("theme_on", v.name).apply()

    fun blocked(): MutableSet<String> =
        p.getStringSet("blocked", emptySet())?.map { PhoneNumberUtils.normalize(it) }?.toMutableSet() ?: mutableSetOf()

    fun block(number: String) {
        val n = PhoneNumberUtils.normalize(number)
        if (n.isBlank()) return
        val s = blocked(); s += n; p.edit().putStringSet("blocked", s).apply()
    }

    fun unblock(number: String) {
        val n = PhoneNumberUtils.normalize(number)
        val s = blocked(); s.remove(n); p.edit().putStringSet("blocked", s).apply()
    }

    fun isBlocked(number: String): Boolean = PhoneNumberUtils.normalize(number) in blocked()

    var quietHoursEnabled: Boolean
        get() = p.getBoolean("quiet", false)
        set(v) = p.edit().putBoolean("quiet", v).apply()

    var quietStartHour: Int
        get() = p.getInt("quiet_start", 22)
        set(v) = p.edit().putInt("quiet_start", v).apply()

    var quietEndHour: Int
        get() = p.getInt("quiet_end", 6)
        set(v) = p.edit().putInt("quiet_end", v).apply()
}

object PhoneAssetManifest {
    const val NOTE =
        "Themes are original Suhood Hybrid Glass colours and geometric motifs. " +
        "Flag themes use public national colours as gradients only — not official emblems. " +
        "No third-party wallpapers bundled. No Quranic text on call controls."
}
