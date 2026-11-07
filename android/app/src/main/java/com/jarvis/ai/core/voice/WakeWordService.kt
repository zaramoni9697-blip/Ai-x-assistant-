package com.jarvis.ai.core.voice

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.jarvis.ai.MainActivity
import com.jarvis.ai.R
import com.jarvis.ai.core.util.Notifications
import com.jarvis.ai.di.Graph

/**
 * Foreground service that listens for the wake phrase "Hey JARVIS".
 *
 * Android-compliant: requires microphone permission, shows a persistent
 * notification, and never records secretly. User must enable this in Settings.
 *
 * Implementation notes:
 *  - Uses Android SpeechRecognizer in a listen loop (no extra paid SDK).
 *  - Accuracy is lower than a dedicated wake-word engine.
 *  - For production-grade always-on detection consider Picovoice Porcupine
 *    (custom "Hey JARVIS" model, AccessKey required, see docs/WAKE_WORD.md).
 */
class WakeWordService : Service() {
    private var speech: SpeechManager? = null
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundInternal()
        startLoop()
        return START_STICKY
    }

    private fun startForegroundInternal() {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, WakeWordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, Notifications.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.wake_notification_title))
            .setContentText(getString(R.string.wake_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "Stop", stop)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this, 42, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(42, notification)
        }
    }

    private fun startLoop() {
        if (running) return
        running = true
        val sm = SpeechManager(this, Graph.session)
        speech = sm
        sm.onResult = { text ->
            if (SpeechManager.looksLikeWakePhrase(text)) {
                val rest = SpeechManager.stripWake(text)
                val i = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(EXTRA_WAKE, true)
                    putExtra(EXTRA_QUERY, rest)
                }
                startActivity(i)
            }
            if (running) sm.start("auto")
        }
        sm.onError = {
            if (running) {
                android.os.Handler(mainLooper).postDelayed({
                    if (running) sm.start("auto")
                }, 800)
            }
        }
        sm.start("auto")
    }

    override fun onDestroy() {
        running = false
        speech?.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.jarvis.ai.STOP_WAKE"
        const val EXTRA_WAKE = "wake"
        const val EXTRA_QUERY = "query"

        fun start(context: Context) {
            val i = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
    }
}
