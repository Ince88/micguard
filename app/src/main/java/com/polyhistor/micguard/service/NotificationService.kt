package com.polyhistor.micguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.polyhistor.micguard.MainActivity
import com.polyhistor.micguard.R

class NotificationService {
    
    companion object {
        private const val CHANNEL_ID = "micguard_alerts"
        private const val NOTIFICATION_ID = 1001
        
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.notification_channel_name)
                val descriptionText = context.getString(R.string.notification_channel_description)
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    enableLights(true)
                }
                
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
        
        fun showMicrophoneNotification(context: Context, appName: String? = null) {
            android.util.Log.d("NotificationService", "Showing microphone usage notification for app: $appName")
            android.util.Log.d("NotificationService", "App name is null: ${appName == null}")
            android.util.Log.d("NotificationService", "App name is empty: ${appName?.isEmpty()}")
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notificationText = if (appName != null) {
                "$appName is using your microphone"
            } else {
                context.getString(R.string.notification_text_generic)
            }
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_alert)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(notificationText))
                .build()
            
            try {
                with(NotificationManagerCompat.from(context)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == 
                            PackageManager.PERMISSION_GRANTED) {
                            notify(NOTIFICATION_ID, notification)
                            android.util.Log.d("NotificationService", "Notification sent successfully")
                        } else {
                            android.util.Log.e("NotificationService", "POST_NOTIFICATIONS permission not granted")
                        }
                    } else {
                        notify(NOTIFICATION_ID, notification)
                        android.util.Log.d("NotificationService", "Notification sent successfully")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationService", "Error showing notification", e)
            }
        }
        
        fun showOngoingNotification(context: Context, packageName: String, appName: String) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("package_name", packageName)
            }
            
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_active)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_ongoing))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()
            
            with(NotificationManagerCompat.from(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == 
                        PackageManager.PERMISSION_GRANTED) {
                        notify(NOTIFICATION_ID + 1, notification)
                    }
                } else {
                    notify(NOTIFICATION_ID + 1, notification)
                }
            }
        }
        
        fun showMicrophoneStoppedNotification(context: Context, durationMillis: Long) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val seconds = durationMillis / 1000
            val minutes = seconds / 60
            val durationText = if (minutes > 0) {
                "$minutes minute${if (minutes > 1) "s" else ""}"
            } else {
                "$seconds second${if (seconds != 1L) "s" else ""}"
            }
            val notificationText = "Microphone was in use for $durationText."
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_alert)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                .build()
            try {
                with(NotificationManagerCompat.from(context)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == 
                            PackageManager.PERMISSION_GRANTED) {
                            notify(NOTIFICATION_ID + 2, notification)
                        }
                    } else {
                        notify(NOTIFICATION_ID + 2, notification)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationService", "Error showing stopped notification", e)
            }
        }
        
        fun cancelNotifications(context: Context) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
        }
    }
} 