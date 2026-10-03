package com.echomind.app.data.sync

import android.content.Context
import com.echomind.app.data.api.SupabaseApi
import com.echomind.app.data.api.SupabaseMemoryRow
import com.echomind.app.data.api.SupabaseNoteRow
import com.echomind.app.data.local.EchoMindDatabase
import com.echomind.app.data.local.NoteEntity
import com.echomind.app.data.memory.MemoryEntity
import com.echomind.app.data.model.IdeaEntry
import com.echomind.app.data.model.ScheduleEntry
import com.echomind.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.UUID

/**
 * 云端同步引擎（Supabase）。
 *
 * 模型：离线优先，Room 为本地真相源。
 *  - 推送：cloudSynced = 0 的行批量 upsert 到云端（按 uuid 冲突合并）
 *  - 删除：本地删除时在 cloud_deletes 排队墓碑，推送远端 DELETE
 *  - 拉取：按 updated_at 游标增量拉取，last-write-wins 合并进 Room
 *    （本地 updatedAt >= 远端 → 跳过，本地稍后推送覆盖）
 *
 * 由 SyncWorker 在网络可用时自动触发，设置页「立即同步」也可手动触发。
 */
class CloudSyncManager(
    context: Context,
    private val settingsRepo: SettingsRepository,
) {

    private val db = EchoMindDatabase.getInstance(context)
    private val noteDao = db.noteDao()
    private val memoryDao = db.memoryDao()
    private val deleteDao = db.cloudDeleteDao()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncAll(): Boolean {
        val settings = settingsRepo.settings.first()
        if (!settings.cloudSyncConfigured) return true
        val api = SupabaseApi(settings.supabaseUrl, settings.supabaseAnonKey)
        val deviceId = settingsRepo.getOrCreateDeviceId()

        // 存量行兜底（正常情况下迁移已补齐）
        noteDao.backfillUpdatedAt()
        memoryDao.backfillUpdatedAt()

        if (!pushDeletes(api)) return false
        if (!pushNotes(api, deviceId)) return false
        if (!pushMemories(api, deviceId)) return false
        if (!pullNotes(api)) return false
        if (!pullMemories(api)) return false
        return true
    }

    // ── 推送：删除墓碑 ──

    private suspend fun pushDeletes(api: SupabaseApi): Boolean {
        val tombstones = deleteDao.getAll()
        if (tombstones.isEmpty()) return true

        val noteUuids = tombstones.filter { it.entityType == TYPE_NOTE }.map { it.uuid }
        val memoryUuids = tombstones.filter { it.entityType == TYPE_MEMORY }.map { it.uuid }

        // in.(...) 过滤器有 URL 长度上限，分批 100
        for (chunk in noteUuids.chunked(100)) {
            api.deleteNotes(chunk).getOrElse { return false }
        }
        for (chunk in memoryUuids.chunked(100)) {
            api.deleteMemories(chunk).getOrElse { return false }
        }
        deleteDao.deleteByIds(tombstones.map { it.id })
        return true
    }

    // ── 推送：脏笔记 ──

    private suspend fun pushNotes(api: SupabaseApi, deviceId: String): Boolean {
        while (true) {
            val dirty = noteDao.getUnsyncedNotes(PAGE_SIZE)
            if (dirty.isEmpty()) return true
            if (!api.upsertNotes(dirty.map { it.toCloudRow(deviceId) }).isSuccess) return false
            noteDao.markSynced(dirty.map { it.uuid })
            if (dirty.size < PAGE_SIZE) return true
        }
    }

    private suspend fun pushMemories(api: SupabaseApi, deviceId: String): Boolean {
        while (true) {
            val dirty = memoryDao.getUnsyncedMemories(PAGE_SIZE)
            if (dirty.isEmpty()) return true
            if (!api.upsertMemories(dirty.map { it.toCloudRow(deviceId) }).isSuccess) return false
            memoryDao.markSynced(dirty.map { it.uuid })
            if (dirty.size < PAGE_SIZE) return true
        }
    }

    // ── 拉取：增量合并 ──

    private suspend fun pullNotes(api: SupabaseApi): Boolean {
        var cursor = settingsRepo.lastPullNotesAt.first()
        var maxSeen = 0L
        while (true) {
            val batch = api.fetchNotes(cursor).getOrElse { return false }
            for (remote in batch) mergeNote(remote)
            maxSeen = maxOf(maxSeen, batch.maxOfOrNull { it.updatedAt } ?: 0L)
            if (batch.size < PULL_LIMIT) break
            cursor = maxSeen + 1
        }
        if (maxSeen > 0) settingsRepo.setLastPullNotesAt(maxSeen + 1)
        return true
    }

    private suspend fun pullMemories(api: SupabaseApi): Boolean {
        var cursor = settingsRepo.lastPullMemoriesAt.first()
        var maxSeen = 0L
        while (true) {
            val batch = api.fetchMemories(cursor).getOrElse { return false }
            for (remote in batch) mergeMemory(remote)
            maxSeen = maxOf(maxSeen, batch.maxOfOrNull { it.updatedAt } ?: 0L)
            if (batch.size < PULL_LIMIT) break
            cursor = maxSeen + 1
        }
        if (maxSeen > 0) settingsRepo.setLastPullMemoriesAt(maxSeen + 1)
        return true
    }

    /** last-write-wins：远端较新才落地；本地较新保持不动（稍后推送覆盖云端） */
    private suspend fun mergeNote(remote: SupabaseNoteRow) {
        val existing = noteDao.getNoteByUuid(remote.uuid)
        if (existing != null && existing.updatedAt >= remote.updatedAt) return
        val entity = remote.toEntity(
            localId = existing?.id ?: 0L,
            notionSynced = existing?.synced ?: false,
        )
        if (existing != null) noteDao.updateNote(entity) else noteDao.insertNote(entity)
    }

    private suspend fun mergeMemory(remote: SupabaseMemoryRow) {
        val existing = memoryDao.getMemoryByUuid(remote.uuid)
        if (existing != null && existing.updatedAt >= remote.updatedAt) return
        val entity = remote.toEntity(localId = existing?.id ?: 0L)
        if (existing != null) memoryDao.update(entity) else memoryDao.insert(entity)
    }

    // ── 映射：Room Entity ↔ 云端行 ──

    private fun NoteEntity.toCloudRow(deviceId: String) = SupabaseNoteRow(
        uuid = uuid,
        deviceId = deviceId,
        templateType = templateType,
        title = title,
        date = date,
        summary = summary,
        accomplishments = decodeList(accomplishments, serializer<String>()),
        challenges = decodeList(challenges, serializer<String>()),
        actionItems = decodeList(actionItems, serializer<String>()),
        keyPoints = decodeList(keyPoints, serializer<String>()),
        ideas = decodeList(ideas, serializer<IdeaEntry>()),
        schedule = decodeList(schedule, serializer<ScheduleEntry>()),
        mood = mood,
        tags = decodeList(tags, serializer<String>()),
        rawTranscription = rawTranscription,
        isVoice = isVoice,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun SupabaseNoteRow.toEntity(localId: Long, notionSynced: Boolean) = NoteEntity(
        id = localId,
        uuid = uuid,
        templateType = templateType,
        title = title,
        date = date,
        summary = summary,
        accomplishments = json.encodeToString(ListSerializer(serializer<String>()), accomplishments),
        challenges = json.encodeToString(ListSerializer(serializer<String>()), challenges),
        actionItems = json.encodeToString(ListSerializer(serializer<String>()), actionItems),
        keyPoints = json.encodeToString(ListSerializer(serializer<String>()), keyPoints),
        ideas = json.encodeToString(ListSerializer(serializer<IdeaEntry>()), ideas),
        schedule = json.encodeToString(ListSerializer(serializer<ScheduleEntry>()), schedule),
        mood = mood,
        tags = json.encodeToString(ListSerializer(serializer<String>()), tags),
        rawTranscription = rawTranscription,
        isVoice = isVoice,
        synced = notionSynced,
        updatedAt = updatedAt,
        cloudSynced = true,
        createdAt = createdAt,
    )

    private fun MemoryEntity.toCloudRow(deviceId: String) = SupabaseMemoryRow(
        uuid = uuid,
        deviceId = deviceId,
        content = content,
        category = category,
        type = type,
        tags = decodeList(tags, serializer<String>()),
        importance = importance,
        source = source,
        isActive = isActive,
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt,
        accessCount = accessCount,
        updatedAt = updatedAt,
    )

    private fun SupabaseMemoryRow.toEntity(localId: Long) = MemoryEntity(
        id = localId,
        uuid = uuid,
        content = content,
        category = category,
        type = type,
        tags = json.encodeToString(ListSerializer(serializer<String>()), tags),
        importance = importance,
        source = source,
        isActive = isActive,
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt,
        accessCount = accessCount,
        updatedAt = updatedAt,
        cloudSynced = true,
    )

    private fun <T> decodeList(value: String, serializer: KSerializer<T>): List<T> {
        if (value.isBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(ListSerializer(serializer), value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        const val TYPE_NOTE = "note"
        const val TYPE_MEMORY = "memory"
        private const val PAGE_SIZE = 200
        private const val PULL_LIMIT = 1000

        /** 云端主键：32 位 hex（与迁移中的 randomblob 格式一致） */
        fun newUuid(): String = UUID.randomUUID().toString().replace("-", "")
    }
}
