package com.suhood.feature.phone

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.suhood.app.R

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
    KAABA_NIGHT("Kaaba Night", "Sacred Mosques"),
    MOSQUE_NIGHT("Mosque Night", "Sacred Mosques"),
    MOSQUE_DUSK("Mosque Dusk", "Sacred Mosques"),
    MADINAH_GLOW("Madinah Glow", "Sacred Mosques"),
    DESERT_NIGHT("Desert Night", "Landscapes"),
    TAJ_INDIA("Taj · India", "Architecture"),
    LUXURY_SILVER("Black & Silver Luxury", "Islamic Luxury"),
    GEOMETRY("Sacred Geometry", "Patterns"),
    PALESTINE_FLAG("Palestine Flag", "Flags"),
    SAUDI_FLAG("Saudi Arabia Flag", "Flags"),
    PAKISTAN_FLAG("Pakistan Flag", "Flags"),
    INDIA_FLAG("India Flag", "Flags"),
    IRAN_FLAG("Iran Flag", "Flags"),
    INDIA_PALESTINE("India–Palestine", "Flags"),
    SILVER_GLASS("Silver Glass", "Minimal"),
    EMERALD_GLASS("Emerald Glass", "Minimal")
}

data class CallThemeColors(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val text: Color,
    val secondary: Color,
    @DrawableRes val wallpaperRes: Int?
)

object CallThemes {
    fun colors(id: CallThemeId): CallThemeColors = when (id) {
        CallThemeId.KAABA_NIGHT -> CallThemeColors(Color(0xFF08090B), Color(0xCC111316), Color(0xFFC7CBD1), Color(0xFFF5F6F7), Color(0xFFA8ADB5), R.drawable.wp_kaaba_night)
        CallThemeId.MOSQUE_NIGHT -> CallThemeColors(Color(0xFF08090B), Color(0xCC111316), Color(0xFF7EB8D4), Color(0xFFF5F6F7), Color(0xFFA8ADB5), R.drawable.wp_mosque_night)
        CallThemeId.MOSQUE_DUSK -> CallThemeColors(Color(0xFF120E0A), Color(0xCC1E1812), Color(0xFFE0A96D), Color(0xFFF5F6F7), Color(0xFFC7B299), R.drawable.wp_mosque_dusk)
        CallThemeId.MADINAH_GLOW -> CallThemeColors(Color(0xFF0B1218), Color(0xCC152028), Color(0xFFD4AF37), Color(0xFFF5F6F7), Color(0xFFC7CBD1), R.drawable.wp_madinah_style)
        CallThemeId.DESERT_NIGHT -> CallThemeColors(Color(0xFF120E0A), Color(0xCC1E1812), Color(0xFFE0A96D), Color(0xFFF5F6F7), Color(0xFFC7B299), R.drawable.wp_desert_night)
        CallThemeId.TAJ_INDIA -> CallThemeColors(Color(0xFF0A0B12), Color(0xCC16182A), Color(0xFFB9A7FF), Color(0xFFF5F6F7), Color(0xFFAAA5B8), R.drawable.wp_taj_india)
        CallThemeId.LUXURY_SILVER -> CallThemeColors(Color(0xFF08090B), Color(0xCC111316), Color(0xFFC7CBD1), Color(0xFFF5F6F7), Color(0xFFA8ADB5), R.drawable.wp_luxury_silver)
        CallThemeId.GEOMETRY -> CallThemeColors(Color(0xFF0A0B12), Color(0xCC16182A), Color(0xFFB9A7FF), Color(0xFFF5F6F7), Color(0xFFAAA5B8), R.drawable.wp_geometry)
        CallThemeId.PALESTINE_FLAG -> CallThemeColors(Color(0xFF0A0A0A), Color(0xCC151515), Color(0xFF009736), Color(0xFFF5F6F7), Color(0xFFCE1126), R.drawable.wp_flag_palestine)
        CallThemeId.SAUDI_FLAG -> CallThemeColors(Color(0xFF06140F), Color(0xCC0E2219), Color(0xFF2ECC71), Color(0xFFF5F6F7), Color(0xFFA8D5B5), R.drawable.wp_flag_saudi)
        CallThemeId.PAKISTAN_FLAG -> CallThemeColors(Color(0xFF04140A), Color(0xCC0A2214), Color(0xFF2ECC71), Color(0xFFF5F6F7), Color(0xFFFFFFFF), R.drawable.wp_flag_pakistan)
        CallThemeId.INDIA_FLAG -> CallThemeColors(Color(0xFF0B0A10), Color(0xCC14131B), Color(0xFFFF9933), Color(0xFFF5F6F7), Color(0xFF138808), R.drawable.wp_flag_india)
        CallThemeId.IRAN_FLAG -> CallThemeColors(Color(0xFF0A1A12), Color(0xCC142820), Color(0xFFDA0000), Color(0xFFF5F6F7), Color(0xFFFFFFFF), R.drawable.wp_flag_iran)
        CallThemeId.INDIA_PALESTINE -> CallThemeColors(Color(0xFF0A0A0A), Color(0xCC151515), Color(0xFF009736), Color(0xFFF5F6F7), Color(0xFFFF9933), R.drawable.wp_india_palestine)
        CallThemeId.SILVER_GLASS -> CallThemeColors(Color(0xFF0C0D10), Color(0xCC1A1C20), Color(0xFFE4E6EA), Color(0xFFF5F6F7), Color(0xFFA8ADB5), null)
        CallThemeId.EMERALD_GLASS -> CallThemeColors(Color(0xFF06140F), Color(0xCC0E2219), Color(0xFF2ECC71), Color(0xFFF5F6F7), Color(0xFFA8D5B5), null)
    }

    fun allCategories(): List<String> = CallThemeId.entries.map { it.category }.distinct()
}

class PhonePrefs(context: Context) {
    private val p: SharedPreferences =
        context.applicationContext.getSharedPreferences("suhood_phone_prefs", Context.MODE_PRIVATE)

    var setupComplete: Boolean
        get() = p.getBoolean("setup_complete", false)
        set(v) = p.edit().putBoolean("setup_complete", v).apply()

    var phoneEnabled: Boolean
        get() = p.getBoolean("phone_enabled", true)
        set(v) = p.edit().putBoolean("phone_enabled", v).apply()

    var numeralStyle: NumeralStyle
        get() = runCatching { NumeralStyle.valueOf(p.getString("numeral", NumeralStyle.WESTERN.name)!!) }
            .getOrDefault(NumeralStyle.WESTERN)
        set(v) = p.edit().putString("numeral", v.name).apply()

    var incomingTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_in", CallThemeId.KAABA_NIGHT.name)!!) }
            .getOrDefault(CallThemeId.KAABA_NIGHT)
        set(v) = p.edit().putString("theme_in", v.name).apply()

    var dialPadTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_pad", CallThemeId.SILVER_GLASS.name)!!) }
            .getOrDefault(CallThemeId.SILVER_GLASS)
        set(v) = p.edit().putString("theme_pad", v.name).apply()

    var ongoingTheme: CallThemeId
        get() = runCatching { CallThemeId.valueOf(p.getString("theme_on", CallThemeId.MOSQUE_NIGHT.name)!!) }
            .getOrDefault(CallThemeId.MOSQUE_NIGHT)
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
        (p.getStringSet("blocked", emptySet()) ?: emptySet()).toMutableSet()

    fun block(number: String) {
        val n = PhoneNumberUtils.normalize(number)
        if (n.isBlank()) return
        val s = blocked(); s.add(n); p.edit().putStringSet("blocked", s).apply()
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

    var preferredSimId: Int
        get() = p.getInt("sim_id", -1)
        set(v) = p.edit().putInt("sim_id", v).apply()

    /** Per-contact theme override: key = normalized number */
    fun contactTheme(number: String): CallThemeId? {
        val key = "ct_" + PhoneNumberUtils.normalize(number)
        val name = p.getString(key, null) ?: return null
        return runCatching { CallThemeId.valueOf(name) }.getOrNull()
    }

    fun setContactTheme(number: String, id: CallThemeId?) {
        val key = "ct_" + PhoneNumberUtils.normalize(number)
        if (id == null) p.edit().remove(key).apply()
        else p.edit().putString(key, id.name).apply()
    }

    /** Gallery URI for custom wallpaper (string) */
    var callButtonStyle: CallButtonStyle
        get() = runCatching { CallButtonStyle.valueOf(p.getString("btn_style", CallButtonStyle.IOS_GLASS.name)!!) }
            .getOrDefault(CallButtonStyle.IOS_GLASS)
        set(v) = p.edit().putString("btn_style", v.name).apply()

    /** Asset file name under phone_wallpapers/ */
    var wallpaperAsset: String?
        get() = p.getString("wp_asset", null)
        set(v) = p.edit().putString("wp_asset", v).apply()

    var ringtoneUri: String?
        get() = p.getString("ringtone_uri", null)
        set(v) = p.edit().putString("ringtone_uri", v).apply()

    var ringtoneTitle: String?
        get() = p.getString("ringtone_title", null)
        set(v) = p.edit().putString("ringtone_title", v).apply()

    fun contactEmoji(number: String): String? =
        p.getString("emoji_" + PhoneNumberUtils.normalize(number), null)

    fun contactAvatarAsset(number: String): String? =
        p.getString("avatar_" + PhoneNumberUtils.normalize(number), null)

    fun contactPhotoUri(number: String): String? =
        p.getString("photo_" + PhoneNumberUtils.normalize(number), null)

    fun setContactAvatarAsset(number: String, asset: String?) {
        val n = PhoneNumberUtils.normalize(number)
        if (n.isBlank()) return
        val ed = p.edit()
        if (asset.isNullOrBlank()) ed.remove("avatar_$n")
        else {
            ed.putString("avatar_$n", asset)
            ed.remove("emoji_$n")
            ed.remove("photo_$n")
        }
        ed.commit()
    }

    fun setContactEmoji(number: String, emoji: String?) {
        val n = PhoneNumberUtils.normalize(number)
        if (n.isBlank()) return
        val ed = p.edit()
        if (emoji.isNullOrBlank()) ed.remove("emoji_$n")
        else {
            ed.putString("emoji_$n", emoji)
            ed.remove("avatar_$n")
            ed.remove("photo_$n")
        }
        ed.commit()
    }

    fun setContactPhotoUri(number: String, uri: String?) {
        val n = PhoneNumberUtils.normalize(number)
        if (n.isBlank()) return
        val ed = p.edit()
        if (uri.isNullOrBlank()) ed.remove("photo_$n")
        else {
            ed.putString("photo_$n", uri)
            ed.remove("avatar_$n")
            ed.remove("emoji_$n")
        }
        ed.commit()
    }

    fun migrateContactAvatar(fromNumber: String, toNumber: String) {
        val a = PhoneNumberUtils.normalize(fromNumber)
        val b = PhoneNumberUtils.normalize(toNumber)
        if (a.isBlank() || b.isBlank() || a == b) return
        val ed = p.edit()
        listOf("emoji_", "avatar_", "photo_").forEach { prefix ->
            val v = p.getString(prefix + a, null)
            if (v != null) {
                ed.putString(prefix + b, v)
                ed.remove(prefix + a)
            }
        }
        ed.commit()
    }

    var customWallpaperUri: String?
        get() = p.getString("custom_wp", null)
        set(v) = p.edit().putString("custom_wp", v).apply()
}

object PhoneAssetManifest {
    const val NOTE =
        "Wallpapers: Unsplash photos (Unsplash License) + original flag/geometry illustrations. " +
            "See docs/PHONE_ASSETS.md for attribution. Offline after install."
}


enum class CallButtonStyle { IOS_GLASS, PILL_RED, CIRCLE_CLASSIC, MINIMAL_DOTS }

object PhoneWallpapers {
    private const val DIR = "phone_wallpapers"
    fun list(context: android.content.Context): List<String> {
        return try {
            context.assets.list(DIR)?.filter { it.endsWith(".jpg") || it.endsWith(".png") }?.sorted().orEmpty()
        } catch (_: Exception) { emptyList() }
    }
    fun open(context: android.content.Context, fileName: String): java.io.InputStream? =
        runCatching { context.assets.open("$DIR/$fileName") }.getOrNull()
}
