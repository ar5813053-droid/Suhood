package com.suhood.feature.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.suhood.core.designsystem.theme.SuhoodTheme
import com.suhood.core.designsystem.theme.ThemeMode

class PhoneLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuhoodTheme(themeMode = ThemeMode.DARK) {
                PhoneRoot(onBack = { finish() })
            }
        }
    }
}
