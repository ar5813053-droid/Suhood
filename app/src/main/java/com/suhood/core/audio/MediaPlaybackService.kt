package com.suhood.core.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.suhood.app.MainActivity
import com.suhood.app.SuhoodApplication

/**
 * Media3 session service — system media notification with play/pause/next/prev + progress
 * (Android equivalent of Apple Now Playing / Dynamic Island compact player).
 */
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        runCatching {
            ensureNotificationChannel()
            setMediaNotificationProvider(
                DefaultMediaNotificationProvider.Builder(this)
                    .setChannelId(CHANNEL_ID)
                    .setNotificationId(NOTIFICATION_ID)
                    .build()
            )

            val app = application as SuhoodApplication
            val player: ExoPlayer = app.playbackController.player

            runCatching {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                player.setHandleAudioBecomingNoisy(true)
            }

            val sessionActivity = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("open", "nasheedify")
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            mediaSession?.release()
            mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(sessionActivity)
                .setId("suhood_nasheedify")
                .build()
        }.onFailure { stopSelf() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        runCatching { mediaSession?.release(); mediaSession = null }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        runCatching {
            val player = mediaSession?.player
            if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
                player.playbackState == Player.STATE_ENDED
            ) stopSelf()
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Nasheedify Playback",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Now playing controls for Nasheedify"
                        setShowBadge(false)
                        setSound(null, null)
                    }
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "suhood_playback"
        private const val NOTIFICATION_ID = 4401
    }
}
