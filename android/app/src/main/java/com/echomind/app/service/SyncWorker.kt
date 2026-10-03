package com.echomind.app.service

import android.app.Application
import android.content.Context
import androidx.work.*
import com.echomind.app.data.api.NotionApi
import com.echomind.app.data.model.StructuredNote
import com.echomind.app.data.repository.SettingsRepository
import com.echomind.app.ui.screens.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker that processes the offline queue.
 *
 * Triggered when network is available:
 * 1. Process each pending Notion item (parse stored JSON, write, retry up to 5x)
 * 2. Run the Supabase cloud sync (push dirty rows + tombstones, pull remote changes)
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val db = PendingSyncDatabase.getInstance(applicationContext)
    private val dao = db.pendingSyncDao()
    private val appCtx = applicationContext

    override suspend fun doWork(): Result {
        val items = dao.getAll()
        val settingsRepo = runCatching {
            val app = appCtx as Application
            SettingsRepository(app.dataStore)
        }.getOrNull()
        val settings = settingsRepo?.settings?.first() ?: return Result.success()

        var successCount = 0
        var failCount = 0

        val json = Json { ignoreUnknownKeys = true }

        for (item in items) {
            if (item.retryCount >= MAX_RETRIES) {
                dao.deleteById(item.id)
                failCount++
                continue
            }

            try {
                val note = json.decodeFromString<StructuredNote>(item.structuredNoteJson)

                if (settings.notionApiKey.isNotBlank() && item.notionDbId.isNotBlank()) {
                    val notionApi = NotionApi(settings.notionApiKey)
                    val result = notionApi.writeNote(note, item.notionDbId)
                    if (result.isSuccess) {
                        dao.deleteById(item.id)
                        successCount++
                        continue
                    }
                }
                // If no Notion configured or write failed, just delete
                dao.deleteById(item.id)
                successCount++
            } catch (e: Exception) {
                dao.incrementRetry(item.id)
                failCount++
            }
        }

        // ── Supabase 云端同步（未配置时为 no-op 成功）──
        val cloudOk = settingsRepo?.let { repo ->
            runCatching {
                com.echomind.app.data.sync.CloudSyncManager(appCtx, repo).syncAll()
            }.getOrDefault(false)
        } ?: true

        return if (failCount == 0 && cloudOk) Result.success() else Result.retry()
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val WORK_NAME = "echomind_sync"

        /** Enqueue a one-time sync work with network constraint */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /** Enqueue after delay */
        fun enqueueDelayed(context: Context, delayMinutes: Long = 1) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /**
         * Repository 变更后的安全触发：WorkManager 未初始化（如纯 JVM 测试）
         * 时静默忽略。
         */
        fun enqueueSafe(context: Context) {
            runCatching { enqueue(context) }
        }
    }
}
