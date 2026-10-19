package com.suhood.feature.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suhood.core.designsystem.components.GlassButton
import com.suhood.core.designsystem.theme.SuhoodTheme
import com.suhood.core.designsystem.theme.SuhoodTypography
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.designsystem.theme.suhoodColors

/**
 * Shown when a restricted app is detected in the foreground during a focus session.
 * Does not kill the other app — guides the user back to focus.
 */
class FocusBlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: "App"
        setContent {
            SuhoodTheme(themeMode = ThemeMode.DARK) {
                val colors = suhoodColors
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Stay focused", style = SuhoodTypography.headlineMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "$pkg is restricted during your focus session.\nReturn to Suhood to continue.",
                        style = SuhoodTypography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(24.dp))
                    GlassButton(text = "Back to focus", onClick = { finish() })
                    Spacer(Modifier.height(8.dp))
                    GlassButton(
                        text = "End session",
                        onClick = {
                            com.suhood.feature.focus.FocusSessionController.get(this@FocusBlockActivity).cancel()
                            finish()
                        },
                        primary = false
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
    }
}
