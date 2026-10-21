package com.suhood.core.navigation

/**
 * Central navigation routes for Suhood.
 * Top-level destinations: Home, Quran, Nasheedify (Audio).
 * Focus, Worship, Tasbeeh, Alarms are nested from Home quick actions.
 */
object Routes {
    // Top-level (bottom nav)
    const val HOME = "home"
    const val JOURNEY = "journey"
    const val QURAN = "quran"
    const val MUHASABAH = "muhasabah"
    const val AUDIO = "audio"


    // All Features hub
    const val ALL_FEATURES = "all_features"

    // Suhood Books
    const val BOOKS = "books"
    const val BOOKS_SEARCH = "books/search"
    const val BOOKS_DETAIL = "books/detail/{identifier}"
    const val BOOKS_READER = "books/reader/{identifier}"
    const val BOOKS_LIBRARY = "books/library"

    // Find Mosque
    const val MOSQUE = "mosque"
    const val MOSQUE_DETAIL = "mosque/detail/{id}"

    // Worship Quest
    const val QUEST = "quest"
    const val QUEST_HISTORY = "quest/history"

    // Nested (reachable from Home / deep links)
    const val WORSHIP = "worship"
    const val FOCUS = "focus"

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
    const val QURAN_PARAH_LIST = "quran/parahs"
    const val QURAN_PARAH = "quran/parah/{parahId}"
    const val QURAN_SURAH_LIST = "quran/surahs"
    const val QURAN_SURAH = "quran/surah/{surahId}"
    const val QURAN_READER = "quran/reader/{surahId}"
    const val QURAN_BOOKMARKS = "quran/bookmarks"
    const val QURAN_PROGRESS = "quran/progress"
    const val QURAN_MUSHAF = "quran/mushaf"

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

    // Salah tracking
    const val SALAH = "salah"
    const val SALAH_HISTORY = "salah/history"

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

    // Qibla
    const val QIBLA = "qibla"

    // My Journey
    const val JOURNEY_HOME = "journey/home"
    const val JOURNEY_SETUP = "journey/setup"
    const val JOURNEY_DETAIL = "journey/detail/{habitId}"

    // Muhasabah
    const val MUHASABAH_HOME = "muhasabah/home"
    const val MUHASABAH_EDITOR = "muhasabah/editor"
    const val MUHASABAH_EDITOR_DATE = "muhasabah/editor/{dateIso}"

    // Settings
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_PRAYER = "settings/prayer"
    const val SETTINGS_SALAH_TIMES = "settings/salah_times"
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

/** Bottom navigation destinations only. */
enum class TopLevelDestination(val route: String) {
    HOME(Routes.HOME),
    JOURNEY(Routes.JOURNEY),
    QURAN(Routes.QURAN),
    MUHASABAH(Routes.MUHASABAH),
    AUDIO(Routes.AUDIO)
}
