package com.suhood.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suhood.core.datastore.PreferencesRepository
import com.suhood.core.designsystem.theme.ThemeMode
import com.suhood.core.prayer.PrayerTimesCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: PreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DARK)

    val onboardingCompleted: StateFlow<Boolean> = repo.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val latitude: StateFlow<Double> = repo.latitude
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 21.4225)

    val longitude: StateFlow<Double> = repo.longitude
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 39.8262)

    val locationLabel: StateFlow<String> = repo.locationLabel
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "Fallback: Makkah (not your local mosque)"
        )

    val prayerMethod: StateFlow<PrayerTimesCalculator.Method> = repo.prayerMethod
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PrayerTimesCalculator.Method.MWL
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repo.setOnboardingCompleted(true) }
    }

    fun setPrayerMethod(method: PrayerTimesCalculator.Method) {
        viewModelScope.launch { repo.setPrayerMethod(method) }
    }

    fun setManualLocation(lat: Double, lng: Double, label: String) {
        viewModelScope.launch { repo.setLocation(lat, lng, label) }
    }

    class Factory(private val repo: PreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repo) as T
    }
}
