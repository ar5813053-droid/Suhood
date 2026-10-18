package com.suhood.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.datastore.PreferencesRepository
import com.suhood.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: PreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val onboardingCompleted: StateFlow<Boolean> = prefs.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingCompleted(true) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    fun resetDemoData() {
        viewModelScope.launch { prefs.resetDemoData() }
    }

    class Factory(private val prefs: PreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(prefs) as T
        }
    }
}
