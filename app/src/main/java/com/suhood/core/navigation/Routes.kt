package com.suhood.core.navigation

/**
 * Central navigation routes for Suhood.
 */
object Routes {
    // Top-level
    const val HOME = "home"
    const val WORSHIP = "worship"
    const val FOCUS = "focus"
    const val AUDIO = "audio"

    // Onboarding
    const val ONBOARDING = "onboarding"
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_MISSION = "onboarding/mission"
    const val ONBOARDING_LANGUAGE = "onboarding/language"
    const val ONBOARDING_APPEARANCE = "onboarding/appearance"
    const val ONBOARDING_PRAYER = "onboarding/prayer"
    const val ONBOARDING_NOTIFICATIONS = "onboarding/notifications"
    const val ONBOARDING_COMPLETE = "onboarding/complete"

    // Focus
    const val FOCUS_DASHBOARD = "focus/dashboard"
    const val FOCUS_SELECT_APPS = "focus/select_apps"
    const val FOCUS_DURATION = "focus/duration"
    const val FOCUS_PRAYER_TRIGGER = "focus/prayer_trigger"
    const val FOCUS_CONFIRM = "focus/confirm"
    const val FOCUS_ACTIVE = "focus/active"
    const val FOCUS_TIME_FOR_SALAH = "focus/time_for_salah"
    const val FOCUS_COMPLETE = "focus/complete"
    const val FOCUS_HISTORY = "focus/history"
    const val FOCUS_STATS = "focus/stats"

    // Alarms
    const val ALARMS = "alarms"
    const val ALARM_CREATE = "alarms/create"
    const val ALARM_EDIT = "alarms/edit/{alarmId}"
    const val ALARM_RINGING = "alarms/ringing/{alarmId}"
    const val ALARM_QUIZ = "alarms/quiz/{alarmId}"
    const val ALARM_DHIKR = "alarms/dhikr/{alarmId}"
    const val ALARM_DISMISSED = "alarms/dismissed"

    // Quran
    const val QURAN = "quran"
    const val QURAN_SURAH_LIST = "quran/surahs"
    const val QURAN_SURAH = "quran/surah/{surahId}"
    const val QURAN_READER = "quran/reader/{surahId}"
    const val QURAN_BOOKMARKS = "quran/bookmarks"
    const val QURAN_PROGRESS = "quran/progress"

    // Worship / Tasbeeh / Adhkar / Dua / Quiz
    const val WORSHIP_DASHBOARD = "worship/dashboard"
    const val TASBEEH = "worship/tasbeeh"
    const val ADHKAR = "worship/adhkar"
    const val ADHKAR_MORNING = "worship/adhkar/morning"
    const val ADHKAR_EVENING = "worship/adhkar/evening"
    const val DUA_LIBRARY = "worship/dua"
    const val DUA_DETAIL = "worship/dua/{duaId}"
    const val WORSHIP_CHECKLIST = "worship/checklist"
    const val QUIZ = "worship/quiz"
    const val QUIZ_DIFFICULTY = "worship/quiz/difficulty"
    const val QUIZ_PLAY = "worship/quiz/play"
    const val QUIZ_RESULTS = "worship/quiz/results"

    // Nasheedify
    const val NASHEEDIFY_HOME = "audio/home"
    const val NASHEEDIFY_DISCOVER = "audio/discover"
    const val NASHEEDIFY_SEARCH = "audio/search"
    const val NASHEEDIFY_CATEGORY = "audio/category/{categoryId}"
    const val NASHEEDIFY_ARTIST = "audio/artist/{artistId}"
    const val NASHEEDIFY_ALBUM = "audio/album/{albumId}"
    const val NASHEEDIFY_PLAYLIST = "audio/playlist/{playlistId}"
    const val NASHEEDIFY_CREATE_PLAYLIST = "audio/playlist/create"
    const val NASHEEDIFY_FAVORITES = "audio/favorites"
    const val NASHEEDIFY_RECENT = "audio/recent"
    const val NASHEEDIFY_QUEUE = "audio/queue"
    const val NASHEEDIFY_PLAYER = "audio/player"
    const val NASHEEDIFY_PREFERENCES = "audio/preferences"

    // Settings
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_PRAYER = "settings/prayer"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_ALARMS = "settings/alarms"
    const val SETTINGS_FOCUS = "settings/focus"
    const val SETTINGS_AUDIO = "settings/audio"
    const val SETTINGS_ACCESSIBILITY = "settings/accessibility"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_ABOUT = "settings/about"
    const val SETTINGS_TIP = "settings/tip"
    const val SETTINGS_PERMISSIONS = "settings/permissions"
}

enum class TopLevelDestination(val route: String) {
    HOME(Routes.HOME),
    WORSHIP(Routes.WORSHIP),
    FOCUS(Routes.FOCUS),
    AUDIO(Routes.AUDIO)
}
