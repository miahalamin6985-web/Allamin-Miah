package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ScreenWakeHelper(private val context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    companion object {
        const val WAKE_CHANNEL_ID = "nova_screen_wake_channel"
        const val WAKE_NOTIFICATION_ID = 9001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WAKE_CHANNEL_ID,
                "Nova Screen Wake Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used to illuminate display and alert user when triggered"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Attempts to illuminate the display using standard Android PowerManager flags
     * and high-priority heads-up notification (approved Android pattern for wake-up).
     */
    @Suppress("DEPRECATION")
    fun requestScreenWake(): ScreenWakeResult {
        return try {
            val isScreenOn = powerManager.isInteractive
            if (isScreenOn) {
                return ScreenWakeResult.AlreadyOn("Screen is already turned on and active.")
            }

            // PowerManager SCREEN_BRIGHT_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "nova:screen_wake_tag"
            )
            wakeLock.acquire(3000) // 3 seconds temporary wake

            // Also post a high-priority heads-up notification which prompts display illumination on supported OEMs
            postWakeNotification()

            ScreenWakeResult.Success(
                "Illuminating display via system wake lock and priority alert. Note: On Android 10+, background app launch restrictions require user interaction to fully unlock."
            )
        } catch (e: Exception) {
            ScreenWakeResult.Restricted(
                "Android restricts third-party apps from arbitrarily waking the screen without user presence. A notification alert was triggered as an alternative."
            )
        }
    }

    private fun postWakeNotification() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, WAKE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Nova AI Screen Wake")
                .setContentText("Tap here to open Nova AI")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(WAKE_NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }
}

sealed class ScreenWakeResult {
    data class Success(val message: String) : ScreenWakeResult()
    data class AlreadyOn(val message: String) : ScreenWakeResult()
    data class Restricted(val explanation: String) : ScreenWakeResult()
}
