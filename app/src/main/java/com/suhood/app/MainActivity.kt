package com.suhood.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suhood.core.designsystem.theme.SuhoodTheme
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors
import com.suhood.core.navigation.SuhoodNavGraph
import com.suhood.feature.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as SuhoodApplication
            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.preferencesRepository)
            )
            val themeMode by settingsVm.themeMode.collectAsStateWithLifecycle(ThemeMode.DARK)
            val onboardingDone by settingsVm.onboardingCompleted.collectAsStateWithLifecycle(false)

            SuhoodTheme(themeMode = themeMode) {
                SuhoodAppRoot(
                    onboardingCompleted = onboardingDone,
                    onOnboardingComplete = { settingsVm.completeOnboarding() },
                    settingsViewModel = settingsVm
                )
            }
        }
    }
}

@Composable
fun SuhoodAppRoot(
    onboardingCompleted: Boolean,
    onOnboardingComplete: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val colors = suhoodColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        SuhoodNavGraph(
            onboardingCompleted = onboardingCompleted,
            onOnboardingComplete = onOnboardingComplete,
            settingsViewModel = settingsViewModel
        )
    }
}
