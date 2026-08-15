package com.echomind.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Handles notification action button clicks for the recording service.
 *
 * Actions:
 * - ACTION_STOP_RECORDING — stops current recording
 * - ACTION_DISMISS_RESULT — dismisses the result notification
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP_RECORDING -> {
                // Forward stop to the foreground service
                val stopIntent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP_RECORDING
                }
                context.startForegroundService(stopIntent)
            }
            ACTION_DISMISS_RESULT -> {
                // Just cancel the notification
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_RESULT)
            }
            ACTION_OPEN_APP -> {
                // Open main activity
                val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                openIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (openIntent != null) {
                    context.startActivity(openIntent)
                }
            }
        }
    }

    companion object {
        const val ACTION_STOP_RECORDING = "com.echomind.app.STOP_RECORDING"
        const val ACTION_DISMISS_RESULT = "com.echomind.app.DISMISS_RESULT"
        const val ACTION_OPEN_APP = "com.echomind.app.OPEN_APP"

        const val NOTIFICATION_CHANNEL_RECORDING = "echomind_recording"
        const val NOTIFICATION_CHANNEL_RESULT = "echomind_result"
        const val NOTIFICATION_ID_RECORDING = 1001
        const val NOTIFICATION_ID_RESULT = 1002

        fun createPendingIntent(
            context: Context,
            action: String,
            requestCode: Int = System.currentTimeMillis().toInt(),
        ): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
