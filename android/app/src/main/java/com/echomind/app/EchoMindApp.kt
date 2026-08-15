package com.echomind.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.echomind.app.service.NotificationActionReceiver

class EchoMindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Recording channel
            val recordingChannel = NotificationChannel(
                NotificationActionReceiver.NOTIFICATION_CHANNEL_RECORDING,
                "录音控制",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "控制录音开始/停止"
                setShowBadge(false)
            }

            // Result channel
            val resultChannel = NotificationChannel(
                NotificationActionReceiver.NOTIFICATION_CHANNEL_RESULT,
                "整理结果",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "录音整理完成通知"
                setShowBadge(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(recordingChannel)
            manager.createNotificationChannel(resultChannel)
        }
    }

    companion object {
        lateinit var instance: EchoMindApp
            private set
    }
}
