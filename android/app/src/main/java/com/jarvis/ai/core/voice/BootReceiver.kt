package com.jarvis.ai.core.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.ai.core.security.SessionStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val session = SessionStore(context)
        if (session.wakeWordEnabled && session.loggedIn.value) {
            WakeWordService.start(context)
        }
    }
}
