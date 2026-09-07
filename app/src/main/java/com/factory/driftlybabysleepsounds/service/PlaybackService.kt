package com.factory.driftlybabysleepsounds.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.factory.driftlybabysleepsounds.DriftlyApplication
import com.factory.driftlybabysleepsounds.MainActivity
import com.factory.driftlybabysleepsounds.R
import com.factory.driftlybabysleepsounds.audio.AudioEngine
import com.factory.driftlybabysleepsounds.data.model.SoundCatalog
import kotlinx.coroutines.launch

/**
 * Keeps the process alive and shows an ongoing notification while [AudioEngine] has active
 * sounds. The engine itself does the actual audio generation and is not owned by this service,
 * so playback state survives even if the service is briefly recreated.
 */
class PlaybackService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        AudioEngine.onAllStopped = { stopPlaybackForeground() }
        observeActiveSounds()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            AudioEngine.stopAll()
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(activeSoundNames()))
        }
        return START_NOT_STICKY
    }

    private fun observeActiveSounds() {
        lifecycleScope.launch {
            AudioEngine.activeVolumes.collect { volumes ->
                if (volumes.isNotEmpty()) {
                    val notification = buildNotification(activeSoundNames())
                    ContextCompat.getSystemService(this@PlaybackService, android.app.NotificationManager::class.java)
                        ?.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun activeSoundNames(): String =
        AudioEngine.activeVolumes.value.keys
            .mapNotNull { SoundCatalog.byId(it)?.name }
            .joinToString(", ")

    private fun buildNotification(soundNames: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, DriftlyApplication.PLAYBACK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_moon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(soundNames.ifBlank { getString(R.string.app_name) })
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .build()
    }

    private fun stopPlaybackForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        private const val ACTION_STOP = "com.factory.driftlybabysleepsounds.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, PlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, PlaybackService::class.java).setAction(ACTION_STOP))
        }
    }
}
