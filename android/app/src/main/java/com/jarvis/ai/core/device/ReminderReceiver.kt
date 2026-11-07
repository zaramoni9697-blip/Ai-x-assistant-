package com.jarvis.ai.core.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.ai.core.util.Notifications

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: "JARVIS reminder"
        val body = intent?.getStringExtra("body") ?: ""
        Notifications.reminder(context, title, body, (System.currentTimeMillis() % 100000).toInt())
    }
}
