package com.suhood.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.suhood.core.audio.AudioCatalogProvider
import com.suhood.core.audio.LibraryRepository
import com.suhood.core.audio.LocalMediaStoreCatalog
import com.suhood.core.audio.PlaybackController
import com.suhood.core.audio.online.NasheedRepository
import com.suhood.core.database.SuhoodDatabase
import com.suhood.core.datastore.PreferencesRepository

class SuhoodApplication : Application() {
    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var database: SuhoodDatabase
        private set
    lateinit var playbackController: PlaybackController
        private set
    lateinit var libraryRepository: LibraryRepository
        private set
    lateinit var audioCatalog: AudioCatalogProvider
        private set
    lateinit var nasheedRepository: NasheedRepository
        private set

    override fun onCreate() {
        // Refresh home-screen widgets once app process is up (Glance content needs Application).

        super.onCreate()
        // Deferred so Room/DataStore are ready
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching { com.suhood.widgets.WidgetBridge.refreshAll(this) }
            runCatching { com.suhood.widgets.WidgetBridge.scheduleNextPrayerRefresh(this) }
        }, 1500)
        preferencesRepository = PreferencesRepository(this)
        database = SuhoodDatabase.get(this)
        libraryRepository = LibraryRepository(database.dao())
        audioCatalog = LocalMediaStoreCatalog(this)
        playbackController = PlaybackController(this, audioCatalog)
        nasheedRepository = NasheedRepository(playback = playbackController)
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            listOf(
                CHANNEL_PRAYER to "Prayer reminders",
                CHANNEL_FOCUS to "Focus sessions",
                CHANNEL_ALARM to "Alarms",
                CHANNEL_AUDIO to "Audio playback",
                CHANNEL_QURAN to "Quran Companion"
            ).forEach { (id, name) ->
                val importance = if (id == CHANNEL_ALARM)
                    NotificationManager.IMPORTANCE_HIGH
                else if (id == CHANNEL_AUDIO)
                    NotificationManager.IMPORTANCE_LOW
                else NotificationManager.IMPORTANCE_DEFAULT
                nm.createNotificationChannel(NotificationChannel(id, name, importance))
            }
        }
    }

    companion object {
        const val CHANNEL_PRAYER = "prayer"
        const val CHANNEL_FOCUS = "focus"
        const val CHANNEL_ALARM = "alarm"
        const val CHANNEL_AUDIO = "audio"
        const val CHANNEL_QURAN = "quran"
    }
}
