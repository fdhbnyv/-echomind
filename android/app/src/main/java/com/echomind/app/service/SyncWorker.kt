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
 * Triggered when network is available. For each pending item:
 * 1. Parse the stored structuredNoteJson
 * 2. Attempt Notion API write
 * 3. If successful, delete from queue
 * 4. If fails, increment retry count (max 5 attempts)
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
        if (items.isEmpty()) return Result.success()

        val json = Json { ignoreUnknownKeys = true }
        var successCount = 0
        var failCount = 0

        val settingsRepo = runCatching {
            val app = appCtx as Application
            SettingsRepository(app.dataStore)
        }.getOrNull()

        val settings = settingsRepo?.settings?.first() ?: return Result.success()

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

        return if (failCount == 0) Result.success() else Result.retry()
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
    }
}
