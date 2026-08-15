package com.echomind.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.echomind.app.MainActivity
import com.echomind.app.R
import com.echomind.app.audio.AudioRecorder
import com.echomind.app.data.api.DashScopeApi
import com.echomind.app.data.api.NotionApi
import com.echomind.app.data.local.EchoMindDatabase
import com.echomind.app.data.model.RecordingState
import com.echomind.app.data.model.StructuredNote
import com.echomind.app.data.model.TemplateType
import com.echomind.app.data.repository.NoteRepository
import com.echomind.app.data.repository.SettingsRepository
import com.echomind.app.ui.screens.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Foreground service for recording audio from the notification.
 *
 * Lifecycle:
 *   start → create notification → start recording → wait for stop →
 *   transcribe → structure → save → notify result → stop self
 */
class RecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioRecorder by lazy { AudioRecorder(this) }
    private val noteRepo by lazy { NoteRepository(this) }
    private val settingsRepo by lazy { SettingsRepository((application as android.app.Application).dataStore) }

    private var notionApi: NotionApi? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> handleStart()
            ACTION_STOP_RECORDING -> handleStop()
            else -> {
                // Initial start without action — begin recording
                if (intent == null) handleStart()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── Actions ──

    private fun handleStart() {
        showRecordingNotification()
        try {
            // Load API config
            scope.launch {
                val s = settingsRepo.settings.first()
                notionApi = if (s.notionApiKey.isNotBlank()) NotionApi(s.notionApiKey) else null
            }
            audioRecorder.startRecording()
            // 启动静音监测线程，持续静音 2.5 秒后自动停止录音
            audioRecorder.startSilenceMonitor {
                handleStop()
            }
        } catch (e: Exception) {
            showResultNotification("录音启动失败", e.message ?: "", isError = true)
            stopSelf()
        }
    }

    private fun handleStop() {
        val audioFile = audioRecorder.stopRecording()
        if (audioFile == null || !audioFile.exists()) {
            showResultNotification("录音已取消", "未保存录音", isError = true)
            stopSelf()
            return
        }
        // Show processing notification
        showProcessingNotification()
        // Process in background
        scope.launch {
            processRecording(audioFile)
        }
    }

    private suspend fun processRecording(audioFile: File) {
        val transcription = transcribe(audioFile) ?: run {
            showResultNotification("转写失败", "请重试", isError = true)
            stopSelf()
            return
        }
        val note = structure(transcription) ?: run {
            showResultNotification("结构化失败", transcription, isError = true)
            stopSelf()
            return
        }

        // Save to local DB
        noteRepo.saveNote(note, isVoice = true, synced = false)

        // Try Notion sync
        scope.launch {
            try {
                val s = settingsRepo.settings.first()
                if (notionApi != null && s.notionDatabaseId.isNotBlank() && s.autoSync) {
                    notionApi!!.writeNote(note, s.notionDatabaseId)
                }
            } catch (_: Exception) { /* offline — queue handled by WorkManager */ }
        }

        showResultNotification(note.title, note.summary)
        stopSelf()
    }

    private suspend fun transcribe(audioFile: File): String? {
        val r = com.echomind.app.data.api.DashScopeApi().transcribe(audioFile)
        return r.getOrNull()
    }

    private suspend fun structure(transcription: String): StructuredNote? {
        val api = com.echomind.app.data.api.DashScopeApi()
        return api.structureNote(transcription, TemplateType.QUICK_IDEA.id).getOrNull()
    }

    // ── Notifications ──

    private fun createNotificationChannels() {
        NotificationManagerCompat.from(this).apply {
            createNotificationChannel(
                NotificationChannelCompat.Builder(
                    NotificationActionReceiver.NOTIFICATION_CHANNEL_RECORDING,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                ).setName("录音中")
                    .setDescription("录音时的前台通知")
                    .build()
            )
            createNotificationChannel(
                NotificationChannelCompat.Builder(
                    NotificationActionReceiver.NOTIFICATION_CHANNEL_RESULT,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ).setName("处理完成")
                    .setDescription("录音处理完成通知")
                    .build()
            )
        }
    }

    private fun showRecordingNotification() {
        val stopPending = NotificationActionReceiver.createPendingIntent(
            this, NotificationActionReceiver.ACTION_STOP_RECORDING
        )
        val notification = NotificationCompat.Builder(
            this, NotificationActionReceiver.NOTIFICATION_CHANNEL_RECORDING
        )
            .setContentTitle("声念 录音中")
            .setContentText("点击停止按钮结束录音")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "停止",
                stopPending,
            )
            .build()

        startForeground(NotificationActionReceiver.NOTIFICATION_ID_RECORDING, notification)
    }

    private fun showProcessingNotification() {
        val notification = NotificationCompat.Builder(
            this, NotificationActionReceiver.NOTIFICATION_CHANNEL_RECORDING
        )
            .setContentTitle("声念 处理中")
            .setContentText("AI 正在分析你的录音…")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setSilent(true)
            .build()
        val nm = NotificationManagerCompat.from(this)
        nm.notify(NotificationActionReceiver.NOTIFICATION_ID_RECORDING, notification)
    }

    private fun showResultNotification(title: String, text: String, isError: Boolean = false, transcriptFallback: String? = null) {
        stopForeground(STOP_FOREGROUND_REMOVE)

        val contentText = if (isError) text else (text.take(120))
        val openPending = NotificationActionReceiver.createPendingIntent(
            this, NotificationActionReceiver.ACTION_OPEN_APP
        )
        val dismissPending = NotificationActionReceiver.createPendingIntent(
            this, NotificationActionReceiver.ACTION_DISMISS_RESULT,
            requestCode = 9999,
        )

        val notification = NotificationCompat.Builder(
            this, NotificationActionReceiver.NOTIFICATION_CHANNEL_RESULT
        )
            .setContentTitle(if (isError) "❌ $title" else "✅ $title")
            .setContentText(contentText)
            .setSmallIcon(if (isError) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", dismissPending)
            .build()

        NotificationManagerCompat.from(this).notify(
            NotificationActionReceiver.NOTIFICATION_ID_RESULT, notification
        )
    }

    companion object {
        const val ACTION_START_RECORDING = "com.echomind.app.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.echomind.app.STOP_RECORDING"

        /** Start recording from notification / widget */
        fun start(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_RECORDING
            }
            context.startForegroundService(intent)
        }

        /** Stop recording from notification / widget */
        fun stop(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }
}
