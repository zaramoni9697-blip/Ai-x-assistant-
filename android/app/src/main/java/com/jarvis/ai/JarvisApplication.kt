package com.jarvis.ai

import android.app.Application
import com.jarvis.ai.core.util.Notifications
import com.jarvis.ai.core.voice.WakeWordService
import com.jarvis.ai.di.Graph

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Notifications.ensureChannels(this)
        if (Graph.session.wakeWordEnabled && Graph.session.accessToken.isNotBlank()) {
            WakeWordService.start(this)
        }
    }
}
