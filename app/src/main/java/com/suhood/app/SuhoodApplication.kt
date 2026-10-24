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
            runCatching {
                val store = com.suhood.feature.focus.SalahLockStore.get(this)
                val snap = store.reconcileNow()
                if (com.suhood.feature.focus.SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
                    com.suhood.feature.focus.SalahLockWatchdog.schedule(this, snap.expiryWallMs)
                    com.suhood.feature.focus.SalahLockMonitorService.start(this)
                }
            }
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
            val soundUri = Uri.parse("android.resource://$packageName/${R.raw.allahu_akbar_short}")
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val vibe = longArrayOf(0, 400, 200, 400, 200, 600)

            fun channel(id: String, name: String, importance: Int): NotificationChannel {
                return NotificationChannel(id, name, importance).apply {
                    description = name
                    setSound(soundUri, attrs)
                    enableVibration(true)
                    vibrationPattern = vibe
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            }

            // Adhan for Salah / prayer / focus / quran — NOT for alarms
            nm.createNotificationChannel(channel(CHANNEL_SALAH_LOCK, "Salah time & app lock", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(channel(CHANNEL_PRAYER, "Prayer reminders", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(channel(CHANNEL_FOCUS, "Focus sessions", NotificationManager.IMPORTANCE_DEFAULT))
            nm.createNotificationChannel(channel(CHANNEL_QURAN, "Quran Companion", NotificationManager.IMPORTANCE_DEFAULT))
            // Alarms: no fixed channel sound — AlarmRingActivity plays the user-selected music
            val alarmAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alarm alerts — uses your selected alarm sound"
                    setSound(null, alarmAttrs) // silent channel; ring activity plays chosen track
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_AUDIO, "Audio playback", NotificationManager.IMPORTANCE_LOW).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    companion object {
        const val CHANNEL_PRAYER = "prayer_v2"
        /** High-importance channel with custom Adhan; new ID so channel settings apply on upgrade. */
        const val CHANNEL_SALAH_LOCK = "salah_lock_v2"
        const val CHANNEL_FOCUS = "focus_v2"
        const val CHANNEL_ALARM = "alarm_v3"
        const val CHANNEL_AUDIO = "audio"
        const val CHANNEL_QURAN = "quran_v2"
    }
}
