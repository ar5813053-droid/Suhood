package com.suhood.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.suhood.core.designsystem.components.FloatingBottomNavigation
import com.suhood.core.designsystem.components.AppBackground
import com.suhood.core.designsystem.components.SolidDarkBackground
import com.suhood.core.designsystem.theme.SuhoodSpacing
import com.suhood.feature.alarms.AlarmScreens
import com.suhood.feature.focus.FocusScreens
import com.suhood.feature.home.HomeScreen
import com.suhood.feature.nasheedify.NasheedifyScreens
import com.suhood.feature.onboarding.OnboardingFlow
import com.suhood.feature.quran.QuranScreens
import com.suhood.feature.settings.SettingsScreens
import com.suhood.feature.salah.SalahScreens
import com.suhood.feature.settings.SettingsViewModel
import com.suhood.feature.worship.WorshipScreens
import com.suhood.app.SuhoodApplication
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SuhoodNavGraph(
    onboardingCompleted: Boolean,
    onOnboardingComplete: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val startDestination = if (onboardingCompleted) Routes.HOME else Routes.ONBOARDING

    val app = LocalContext.current.applicationContext as SuhoodApplication
    val playbackState by app.playbackController.state.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = currentRoute in listOf(
        Routes.HOME, Routes.QURAN, Routes.AUDIO
    )

    val selectedTop = when {
        currentRoute == Routes.HOME -> TopLevelDestination.HOME
        currentRoute == Routes.QURAN || currentRoute?.startsWith("quran") == true -> TopLevelDestination.QURAN
        currentRoute == Routes.AUDIO || currentRoute?.startsWith("audio") == true -> TopLevelDestination.AUDIO
        else -> TopLevelDestination.HOME
    }

    val isNasheedifyRoute = currentRoute == Routes.AUDIO ||
        (currentRoute?.startsWith("audio/") == true)

    Box(modifier = Modifier.fillMaxSize()) {
        if (isNasheedifyRoute) {
            SolidDarkBackground()
        } else {
            AppBackground()
        }
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            // Onboarding
            composable(Routes.ONBOARDING) {
                OnboardingFlow(
                    onComplete = {
                        onOnboardingComplete()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                    settingsViewModel = settingsViewModel
                )
            }

            // Top-level
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    bottomContentPadding = if (showBottomNav) SuhoodSpacing.aboveBottomNav else 0.dp
                )
            }
            composable(Routes.WORSHIP) {
                WorshipScreens.Dashboard(
                    onNavigate = { route -> navController.navigate(route) },
                    bottomContentPadding = SuhoodSpacing.aboveBottomNav
                )
            }
            composable(Routes.FOCUS) {
                FocusScreens.Dashboard(
                    onNavigate = { route -> navController.navigate(route) },
                    bottomContentPadding = SuhoodSpacing.aboveBottomNav
                )
            }
            composable(Routes.AUDIO) {
                NasheedifyScreens.Home(
                    onNavigate = { route -> navController.navigate(route) },
                    playbackState = playbackState,
                    onPlaybackChange = { /* shared controller owns state */ },
                    bottomContentPadding = SuhoodSpacing.aboveBottomNav
                )
            }

            // Focus sub-screens
            composable(Routes.FOCUS_SELECT_APPS) {
                FocusScreens.SelectApps(onBack = { navController.popBackStack() })
            }
            composable(Routes.FOCUS_DURATION) {
                FocusScreens.DurationSelect(
                    onBack = { navController.popBackStack() },
                    onConfirm = { mins ->
                        navController.navigate(Routes.FOCUS_ACTIVE + "?mins=$mins")
                    }
                )
            }
            composable(
                route = Routes.FOCUS_ACTIVE + "?mins={mins}",
                arguments = listOf(navArgument("mins") { type = NavType.IntType; defaultValue = 15 })
            ) { entry ->
                val mins = entry.arguments?.getInt("mins") ?: 15
                FocusScreens.ActiveSession(
                    durationMinutes = mins,
                    onComplete = { navController.navigate(Routes.FOCUS_COMPLETE) { popUpTo(Routes.FOCUS) } },
                    onCancel = { navController.popBackStack(Routes.FOCUS, false) }
                )
            }
            composable(Routes.FOCUS_COMPLETE) {
                FocusScreens.SessionComplete(onDone = { navController.popBackStack(Routes.FOCUS, false) })
            }
            composable(Routes.FOCUS_HISTORY) {
                FocusScreens.History(onBack = { navController.popBackStack() })
            }
            composable(Routes.FOCUS_TIME_FOR_SALAH) {
                FocusScreens.TimeForSalah(onDismiss = { navController.popBackStack() })
            }

            // Alarms
            composable(Routes.ALARMS) {
                AlarmScreens.Dashboard(
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(Routes.ALARM_CREATE) },
                    onEdit = { id -> navController.navigate("alarms/edit/$id") },
                    onSimulateRing = { id -> navController.navigate("alarms/ringing/$id") }
                )
            }
            composable(Routes.ALARM_CREATE) {
                AlarmScreens.CreateEdit(
                    alarmId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                "alarms/edit/{alarmId}",
                arguments = listOf(navArgument("alarmId") { type = NavType.StringType })
            ) { entry ->
                AlarmScreens.CreateEdit(
                    alarmId = entry.arguments?.getString("alarmId"),
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                "alarms/ringing/{alarmId}",
                arguments = listOf(navArgument("alarmId") { type = NavType.StringType })
            ) {
                AlarmScreens.Ringing(
                    onQuiz = { navController.navigate(Routes.ALARM_QUIZ.replace("{alarmId}", "demo")) },
                    onDhikr = { navController.navigate(Routes.ALARM_DHIKR.replace("{alarmId}", "demo")) },
                    onDismiss = { navController.navigate(Routes.ALARM_DISMISSED) { popUpTo(Routes.ALARMS) } }
                )
            }
            composable(Routes.ALARM_QUIZ.replace("{alarmId}", "{alarmId}")) {
                AlarmScreens.QuizChallenge(onComplete = {
                    navController.navigate(Routes.ALARM_DISMISSED) { popUpTo(Routes.ALARMS) }
                })
            }
            composable(Routes.ALARM_DHIKR.replace("{alarmId}", "{alarmId}")) {
                AlarmScreens.DhikrChallenge(onComplete = {
                    navController.navigate(Routes.ALARM_DISMISSED) { popUpTo(Routes.ALARMS) }
                })
            }
            composable(Routes.ALARM_DISMISSED) {
                AlarmScreens.Dismissed(onDone = { navController.popBackStack(Routes.ALARMS, false) })
            }

            // Quran
            composable(Routes.QURAN) {
                QuranScreens.SurahList(
                    onBack = { navController.popBackStack() },
                    onSurah = { id -> navController.navigate("quran/reader/$id") },
                    onBookmarks = { navController.navigate(Routes.QURAN_BOOKMARKS) },
                    onJuz = { navController.navigate(Routes.QURAN_PARAH_LIST) },
                    onMushaf = { navController.navigate(Routes.QURAN_MUSHAF) },
                    onLastRead = { navController.navigate(Routes.QURAN_MUSHAF) }
                )
            }
            composable(
                "quran/parah/{parahId}",
                arguments = listOf(navArgument("parahId") { type = NavType.IntType })
            ) { entry ->
                val id = entry.arguments?.getInt("parahId") ?: 1
                QuranScreens.ParahDetail(
                    parahId = id,
                    onBack = { navController.popBackStack() },
                    onSurah = { sid -> navController.navigate("quran/reader/$sid") }
                )
            }
            composable(Routes.QURAN_MUSHAF) {
                QuranScreens.MushafReader(onBack = { navController.popBackStack() })
            }
            composable(Routes.QURAN_SURAH_LIST) {
                QuranScreens.SurahList(
                    onBack = { navController.popBackStack() },
                    onSurah = { id -> navController.navigate("quran/reader/$id") },
                    onBookmarks = { navController.navigate(Routes.QURAN_BOOKMARKS) },
                    onJuz = { navController.navigate(Routes.QURAN_PARAH_LIST) },
                    onMushaf = { navController.navigate(Routes.QURAN_MUSHAF) },
                    onLastRead = { navController.navigate(Routes.QURAN_MUSHAF) }
                )
            }
            composable(
                "quran/surah/{surahId}",
                arguments = listOf(navArgument("surahId") { type = NavType.IntType })
            ) { entry ->
                val id = entry.arguments?.getInt("surahId") ?: 1
                QuranScreens.SurahDetail(
                    surahId = id,
                    onBack = { navController.popBackStack() },
                    onRead = { navController.navigate("quran/reader/$id") }
                )
            }
            composable(
                "quran/reader/{surahId}",
                arguments = listOf(navArgument("surahId") { type = NavType.IntType })
            ) { entry ->
                QuranScreens.Reader(
                    surahId = entry.arguments?.getInt("surahId") ?: 1,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.QURAN_BOOKMARKS) {
                QuranScreens.Bookmarks(onBack = { navController.popBackStack() })
            }

            // Worship sub
            composable(Routes.TASBEEH) {
                WorshipScreens.Tasbeeh(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADHKAR) {
                WorshipScreens.Adhkar(
                    onBack = { navController.popBackStack() },
                    onMorning = { navController.navigate(Routes.ADHKAR_MORNING) },
                    onEvening = { navController.navigate(Routes.ADHKAR_EVENING) }
                )
            }
            composable(Routes.ADHKAR_MORNING) {
                WorshipScreens.AdhkarDetail(title = "Morning Adhkar", onBack = { navController.popBackStack() })
            }
            composable(Routes.ADHKAR_EVENING) {
                WorshipScreens.AdhkarDetail(title = "Evening Adhkar", onBack = { navController.popBackStack() })
            }
            composable(Routes.DUA_LIBRARY) {
                WorshipScreens.DuaLibrary(
                    onBack = { navController.popBackStack() },
                    onDua = { id -> navController.navigate("worship/dua/$id") }
                )
            }
            composable(
                "worship/dua/{duaId}",
                arguments = listOf(navArgument("duaId") { type = NavType.StringType })
            ) { entry ->
                WorshipScreens.DuaDetail(
                    duaId = entry.arguments?.getString("duaId") ?: "1",
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.QUIZ) {
                WorshipScreens.QuizHome(
                    onBack = { navController.popBackStack() },
                    onStart = { navController.navigate(Routes.QUIZ_DIFFICULTY) }
                )
            }
            composable(Routes.QUIZ_DIFFICULTY) {
                WorshipScreens.QuizDifficulty(
                    onBack = { navController.popBackStack() },
                    onSelect = { navController.navigate(Routes.QUIZ_PLAY) }
                )
            }
            composable(Routes.QUIZ_PLAY) {
                WorshipScreens.QuizPlay(
                    onFinish = { result ->
                        // Store result in savedStateHandle would be better; simple navigate
                        navController.navigate(Routes.QUIZ_RESULTS)
                    }
                )
            }
            composable(Routes.QUIZ_RESULTS) {
                WorshipScreens.QuizResults(
                    onRetry = { navController.navigate(Routes.QUIZ_PLAY) { popUpTo(Routes.QUIZ) } },
                    onDone = { navController.popBackStack(Routes.WORSHIP, false) }
                )
            }
            composable(Routes.WORSHIP_CHECKLIST) {
                WorshipScreens.Checklist(onBack = { navController.popBackStack() })
            }

            // Nasheedify
            composable(Routes.NASHEEDIFY_SEARCH) {
                NasheedifyScreens.Search(
                    onBack = { navController.popBackStack() },
                    onTrack = { track ->
                        app.playbackController.play(track)
                        navController.navigate(Routes.NASHEEDIFY_PLAYER)
                    }
                )
            }
            composable(
                "audio/category/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { entry ->
                NasheedifyScreens.CategoryDetail(
                    categoryId = entry.arguments?.getString("categoryId") ?: "",
                    onBack = { navController.popBackStack() },
                    onTrack = { track ->
                        app.playbackController.play(track)
                        navController.navigate(Routes.NASHEEDIFY_PLAYER)
                    }
                )
            }
            composable(Routes.NASHEEDIFY_FAVORITES) {
                NasheedifyScreens.Favorites(
                    onBack = { navController.popBackStack() },
                    onTrack = { track ->
                        app.playbackController.play(track)
                        navController.navigate(Routes.NASHEEDIFY_PLAYER)
                    }
                )
            }
            composable(Routes.NASHEEDIFY_PLAYER) {
                NasheedifyScreens.FullPlayer(
                    state = playbackState,
                    onStateChange = { /* shared controller owns state */ },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.NASHEEDIFY_QUEUE) {
                NasheedifyScreens.Queue(
                    state = playbackState,
                    onBack = { navController.popBackStack() },
                    onTrack = { track ->
                        app.playbackController.play(track)
                        navController.navigate(Routes.NASHEEDIFY_PLAYER)
                    }
                )
            }
            composable(Routes.NASHEEDIFY_CREATE_PLAYLIST) {
                NasheedifyScreens.CreatePlaylist(onBack = { navController.popBackStack() })
            }

            // Settings
            composable(Routes.SETTINGS) {
                SettingsScreens.Home(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(Routes.SETTINGS_APPEARANCE) {
                SettingsScreens.Appearance(
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_LANGUAGE) {
                SettingsScreens.Language(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_ABOUT) {
                SettingsScreens.About(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_TIP) {
                SettingsScreens.TipSupport(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_PERMISSIONS) {
                SettingsScreens.PermissionsGuide(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_PRIVACY) {
                SettingsScreens.Privacy(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_PRAYER) {
                SettingsScreens.PrayerPrefs(settingsViewModel = settingsViewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_SALAH_TIMES) {
                SettingsScreens.SalahTimes(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_NOTIFICATIONS) {
                SettingsScreens.NotificationPrefs(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_FOCUS) {
                SettingsScreens.FocusPrefs(onBack = { navController.popBackStack() })
            }
            composable(Routes.SALAH) {
                SalahScreens.Today(
                    onBack = { navController.popBackStack() },
                    onHistory = { navController.navigate(Routes.SALAH_HISTORY) }
                )
            }
            composable(Routes.SALAH_HISTORY) {
                SalahScreens.History(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_AUDIO) {
                SettingsScreens.AudioPrefs(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_ACCESSIBILITY) {
                SettingsScreens.Accessibility(onBack = { navController.popBackStack() })
            }
        }

        if (showBottomNav && currentRoute != Routes.ONBOARDING) {
            FloatingBottomNavigation(
                selected = selectedTop,
                onSelect = { dest ->
                    navController.navigate(dest.route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
