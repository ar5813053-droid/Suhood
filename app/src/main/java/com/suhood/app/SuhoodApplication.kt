package com.suhood.app

import android.app.Application
import android.net.Uri
import android.media.AudioAttributes
import android.app.Notification
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
            runCatching { com.suhood.core.prayer.SalahReminderScheduler.rescheduleAll(this) }
            runCatching { com.suhood.feature.focus.SalahLockStore.get(this).reconcileNow() }
        }, 1500)
        preferencesRepository = PreferencesRepository(this)
        database = SuhoodDatabase.get(this)
        libraryRepository = LibraryRepository(database.dao())
        audioCatalog = LocalMediaStoreCatalog(this)
        playbackController = PlaybackController(this, audioCatalog)
        nasheedRepository = NasheedRepository(playback = playbackController)
        createChannels()
        // Seed prayer cache so widgets work before Home opens (no DataStore)
        runCatching {
            // Prefer last-known device location so widgets/Home use real local times
            val loc = com.suhood.core.prayer.LocationHelper.lastKnownLocation(this)
            val lat = loc?.latitude ?: 21.4225
            val lng = loc?.longitude ?: 39.8262
            val label = if (loc != null) "Device location" else "Makkah (default)"
            val result = com.suhood.core.prayer.PrayerTimesCalculator.calculate(lat, lng)
            com.suhood.core.prayer.PrayerTimesCache.save(
                this, lat, lng,
                com.suhood.core.prayer.PrayerTimesCalculator.Method.MWL,
                label,
                result
            )
            com.suhood.widgets.WidgetBridge.refreshAll(this)
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Legacy generic prayer channel (no custom sound — keep for old installs)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_PRAYER, "Prayer reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )

            // New Salah lock channel with Adhan sound + vibration (v2 ID so OS picks up new settings)
            val soundUri = Uri.parse("android.resource://$packageName/${R.raw.allahu_akbar_short}")
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val salahCh = NotificationChannel(
                CHANNEL_SALAH_LOCK,
                "Salah time & app lock",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Salah time alerts with Adhan and 10-minute app lock"
                setSound(soundUri, attrs)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(salahCh)

            listOf(
                CHANNEL_FOCUS to ("Focus sessions" to NotificationManager.IMPORTANCE_DEFAULT),
                CHANNEL_ALARM to ("Alarms" to NotificationManager.IMPORTANCE_HIGH),
                CHANNEL_AUDIO to ("Audio playback" to NotificationManager.IMPORTANCE_LOW),
                CHANNEL_QURAN to ("Quran Companion" to NotificationManager.IMPORTANCE_DEFAULT)
            ).forEach { (id, pair) ->
                nm.createNotificationChannel(NotificationChannel(id, pair.first, pair.second))
            }
        }
    }

    companion object {
        const val CHANNEL_PRAYER = "prayer"
        /** High-importance channel with custom Adhan; new ID so channel settings apply on upgrade. */
        const val CHANNEL_SALAH_LOCK = "salah_lock_v2"
        const val CHANNEL_FOCUS = "focus"
        const val CHANNEL_ALARM = "alarm"
        const val CHANNEL_AUDIO = "audio"
        const val CHANNEL_QURAN = "quran"
    }
}
