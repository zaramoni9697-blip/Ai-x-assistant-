package com.jarvis.ai.core.device

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent

/**
 * Supported Android actions. Sensitive operations require confirmation in the UI.
 * This class never claims unrestricted control of every app or system setting.
 */
class DeviceActionHandler(private val context: Context) {
    data class Result(val message: String, val needsConfirmation: Boolean = false, val pending: Pending? = null)
    data class Pending(val type: String, val payload: String)

    private var torchOn = false

    fun interpret(raw: String): Result {
        val q = raw.trim().lowercase()
        return when {
            looks(q, "flashlight", "torch", "flash", "টর্চ", "ফ্ল্যাশলাইট") ->
                Result("Toggle flashlight?", true, Pending("flashlight", ""))
            looks(q, "battery", "charge", "ব্যাটারি") -> battery()
            looks(q, "device info", "phone info", "system info", "ডিভাইস") -> deviceInfo()
            looks(q, "volume up", "increase volume", "আওয়াজ বাড়া") -> volume(1)
            looks(q, "volume down", "decrease volume", "আওয়াজ কমা") -> volume(-1)
            looks(q, "mute", "unmute") -> volume(0)
            looks(q, "wifi settings", "wi-fi") ->
                Result("Open Wi-Fi settings?", true, Pending("settings", Settings.ACTION_WIFI_SETTINGS))
            looks(q, "bluetooth") ->
                Result("Open Bluetooth settings?", true, Pending("settings", Settings.ACTION_BLUETOOTH_SETTINGS))
            looks(q, "open settings", "settings", "সেটিংস") ->
                Result("Open system settings?", true, Pending("settings", Settings.ACTION_SETTINGS))
            looks(q, "alarm", "wake me", "অ্যালার্ম") ->
                Result("Create an alarm using the system clock app?", true, Pending("alarm", raw))
            looks(q, "remind", "reminder", "রিমাইন্ড") ->
                Result("Create a reminder notification in 10 minutes?", true, Pending("reminder", raw))
            looks(q, "play", "pause", "next song", "previous song", "music") ->
                Result("Send a media play/pause command?", true, Pending("media", q))
            looks(q, "open ") || q.startsWith("খোল") -> {
                val name = raw.replace(Regex("(?i)(open|launch|start|খোল[োো]?)"), "").trim()
                Result("Open app \"$name\"?", true, Pending("open_app", name))
            }
            else -> Result(
                "I can open apps, toggle flashlight, check battery, change volume, open settings, " +
                    "and create alarms/reminders through Android system interfaces. " +
                    "I cannot bypass permissions or control every system setting."
            )
        }
    }

    fun execute(pending: Pending): String {
        return when (pending.type) {
            "flashlight" -> toggleFlashlight()
            "settings" -> openSettings(pending.payload)
            "alarm" -> setAlarm()
            "reminder" -> scheduleReminder(pending.payload)
            "media" -> media(pending.payload)
            "open_app" -> openApp(pending.payload)
            else -> "Unknown action"
        }
    }

    fun toggleFlashlight(): String {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull { cam ->
            cm.getCameraCharacteristics(cam)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return "No flashlight on this device"
        torchOn = !torchOn
        cm.setTorchMode(id, torchOn)
        return if (torchOn) "Flashlight on." else "Flashlight off."
    }

    fun battery(): Result {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = bm.isCharging
        return Result("Battery is at $pct%${if (charging) " and charging" else ""}.")
    }

    fun deviceInfo(): Result {
        val info = buildString {
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            append("Brand: ${Build.BRAND}")
        }
        return Result(info)
    }

    fun volume(delta: Int): Result {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when {
            delta > 0 -> am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI
            )
            delta < 0 -> am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI
            )
            else -> am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI
            )
        }
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return Result("Media volume $cur / $max.")
    }

    fun openSettings(action: String): String {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Opened settings."
    }

    fun setAlarm(): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, 7)
            putExtra(AlarmClock.EXTRA_MINUTES, 0)
            putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS alarm")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            "Opening the system clock to confirm the alarm. Android requires the clock app UI for alarms."
        } else {
            "No clock app found that can create alarms."
        }
    }

    fun scheduleReminder(text: String): String {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", "JARVIS")
            putExtra("body", text.ifBlank { "Reminder" })
        }
        val pi = PendingIntent.getBroadcast(
            context, (System.currentTimeMillis() % 100000).toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val whenAt = System.currentTimeMillis() + 10 * 60 * 1000
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
            return "Reminder scheduled in about 10 minutes (inexact — grant exact alarm permission in settings for precision)."
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
        return "Reminder scheduled in 10 minutes."
    }

    fun media(q: String): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val key = when {
            "next" in q -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" in q || "prev" in q -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
        return "Sent media command. Works when a player is active."
    }

    fun openApp(name: String): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val match = apps.firstOrNull {
            it.loadLabel(pm).toString().contains(name, ignoreCase = true)
        } ?: return "I couldn't find an installed app named \"$name\"."
        val launch = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            ?: return "That app cannot be launched."
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        return "Opening ${match.loadLabel(pm)}."
    }

    fun installedApps(): List<String> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.loadLabel(pm).toString() }
            .distinct()
            .sorted()
    }

    private fun looks(q: String, vararg keys: String) = keys.any { q.contains(it) }
}
