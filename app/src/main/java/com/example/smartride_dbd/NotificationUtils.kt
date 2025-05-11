package com.example.smartride_dbd

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

object NotificationUtils {
    private const val PREFS_NAME = "smart_ride_prefs"
    private const val KEY_NOTIFICATION_ENABLED = "is_notification_enabled"

    private const val CHANNEL_ID = "foreground_service"
    private const val CHANNEL_NAME = "Foreground Service"
    private const val CHANNEL_DESCRIPTION = "Notification channel for foreground service"

    /**
     * Check if notifications are enabled.
     */
    fun isNotificationEnabled(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }

    /**
     * Enable or disable notifications.
     */
    fun setNotificationEnabled(context: Context, isEnabled: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, isEnabled).apply()
    }

    /**
     * Create notification channels for Android 8.0+ with custom sound.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Define custom sound URI
            val soundUri: Uri = Uri.parse("android.resource://${context.packageName}/raw/alert_sound")

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Set to HIGH to ensure sound plays
            ).apply {
                description = CHANNEL_DESCRIPTION
                setSound(soundUri, audioAttributes) // Assign custom sound to the channel
                enableLights(true)
                enableVibration(true)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Create a notification for the foreground service.
     */
    fun createForegroundServiceNotification(context: Context, title: String, message: String): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.logodbd) // Ensure this icon exists
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Send a notification for aggressive driving alerts.
     */
    fun sendNotification(context: Context, title: String, message: String) {
        // Check if notifications are enabled before sending
        if (!isNotificationEnabled(context)) {
            Log.d("NotificationUtils", "Notifications are disabled by user")
            return
        }

        // Check if we have permission to post notifications on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e("NotificationUtils", "Notification permission not granted")
                return
            }
        }

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.logodbd)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val notificationId = System.currentTimeMillis().toInt()
            Log.d("NotificationUtils", "Sending notification with ID: $notificationId")
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d("NotificationUtils", "Notification sent successfully")
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Error sending notification: ${e.message}")
        }
    }
}
