package com.echomind.app.data.memory

import android.content.Context
import com.echomind.app.data.local.CloudDeleteEntity
import com.echomind.app.data.local.EchoMindDatabase
import com.echomind.app.data.sync.CloudSyncManager
import com.echomind.app.service.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MemoryRepository(private val context: Context) {

    private val db = EchoMindDatabase.getInstance(context)
    private val dao = db.memoryDao()

    // ── Basic CRUD ──

    /** All active memories */
    val allMemories: Flow<List<Memory>> = dao.getAllActiveMemories().map { list ->
        list.map { it.toMemory() }
    }

    /** Get single memory and increment access count */
    suspend fun getMemory(id: Long): Memory? {
        val entity = dao.getMemoryById(id)
        if (entity != null) {
            dao.incrementAccess(id)
        }
        return entity?.toMemory()
    }

    /** Create memory — 生成云端 uuid 并标记待同步 */
    suspend fun addMemory(memory: Memory): Long {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            memory.toEntity().copy(
                uuid = CloudSyncManager.newUuid(),
                createdAt = now,
                lastAccessedAt = now,
                updatedAt = now,
                cloudSynced = false,
            )
        )
        SyncWorker.enqueueSafe(context)
        return id
    }

    /** Update memory — last-write-wins：递增 updatedAt 并标记待同步 */
    suspend fun updateMemory(memory: Memory) {
        dao.update(
            memory.toEntity(memory.id).copy(
                updatedAt = System.currentTimeMillis(),
                cloudSynced = false,
            )
        )
        SyncWorker.enqueueSafe(context)
    }

    /** Soft delete — 云端同步 isActive=false（保留行） */
    suspend fun deleteMemory(id: Long) {
        dao.softDelete(id)
        dao.markUnsynced(id)
        SyncWorker.enqueueSafe(context)
    }

    /** Hard delete — 云端删除墓碑 */
    suspend fun forceDeleteMemory(id: Long) {
        val entity = dao.getMemoryById(id)
        dao.hardDelete(id)
        if (entity?.uuid?.isNotBlank() == true) {
            db.cloudDeleteDao().insert(
                CloudDeleteEntity(entityType = CloudSyncManager.TYPE_MEMORY, uuid = entity.uuid)
            )
        }
        SyncWorker.enqueueSafe(context)
    }

    // ── Category / Type filters ──

    fun getByCategory(category: String): Flow<List<Memory>> =
        dao.getMemoriesByCategory(category).map { list -> list.map { it.toMemory() } }

    fun getByType(type: String): Flow<List<Memory>> =
        dao.getMemoriesByType(type).map { list -> list.map { it.toMemory() } }

    // ── Search ──

    /** Search by keyword across content and tags */
    fun search(keyword: String): Flow<List<Memory>> =
        dao.searchByKeyword(keyword).map { list -> list.map { it.toMemory() } }

    /** Get all memories matching a specific tag */
    fun getByTag(tag: String): Flow<List<Memory>> =
        dao.getMemoriesByTag("%$tag%").map { list -> list.map { it.toMemory() } }

    // ── Count ──

    val memoryCount: Flow<Int> = dao.getActiveCount()

    // ── Bulk Import from Notes ──
    // Extract meaningful facts from existing notes and create memories

    /**
     * Auto-extract memories from notes (user facts, preferences, recurring patterns).
     * Run this once during onboarding or when user requests "sync memories from notes".
     */
    suspend fun syncMemoriesFromNotes(
        noteRepository: com.echomind.app.data.repository.NoteRepository,
    ): Int {
        return withContext(Dispatchers.IO) {
            val all = allMemories.first()
            val existingContent = all.map { it.content }
            val notes = noteRepository.allNotes.first()
            var added = 0

            for (note in notes) {
                // Extract action items as recurring preferences
                for (action in note.actionItems) {
                    val content = "行动项: $action"
                    if (content !in existingContent) {
                        addMemory(Memory(
                            id = 0,
                            content = content,
                            category = MemoryCategory.NOTES.name,
                            type = MemoryType.FACT.name,
                            tags = note.tags,
                            importance = 3,
                            source = "auto:note:${note.date}",
                            createdAt = System.currentTimeMillis(),
                            lastAccessedAt = System.currentTimeMillis(),
                        ))
                        added++
                    }
                }
                // Extract mood pattern
                if (note.mood != null && note.mood.isNotEmpty()) {
                    val content = "情绪: ${note.mood} 于 ${note.date}"
                    if (content !in existingContent) {
                        addMemory(Memory(
                            id = 0,
                            content = content,
                            category = MemoryCategory.USER_FACTS.name,
                            type = MemoryType.FACT.name,
                            tags = listOf("情绪"),
                            importance = 2,
                            source = "auto:note:${note.date}",
                            createdAt = System.currentTimeMillis(),
                            lastAccessedAt = System.currentTimeMillis(),
                        ))
                        added++
                    }
                }
            }
            added
        }
    }

    // ── Relevance Scoring ──
    // Returns top-N most relevant memories for a given query/context

    /**
     * Get top-K memories relevant to the current context.
     * Uses tag overlap + keyword match + recency weighting.
     */
    suspend fun getRelevantMemories(
        query: String,
        existingTags: List<String> = emptyList(),
        limit: Int = 5,
    ): List<Memory> = withContext(Dispatchers.IO) {
        val all = allMemories.first()
        all.sortedByDescending { memory ->
            var score = 0
            // Tag overlap
            for (tag in existingTags) {
                if (memory.tags.contains(tag)) score += 3
            }
            // Content keyword match
            if (query.isNotBlank() && memory.content.contains(query, ignoreCase = true)) {
                score += 5
            }
            // Tags keyword match
            for (tag in memory.tags) {
                if (tag.contains(query, ignoreCase = true)) score += 2
            }
            // Recency bonus
            val daysSinceLastAccess = (System.currentTimeMillis() - memory.lastAccessedAt) / (1000 * 60 * 60 * 24)
            if (daysSinceLastAccess < 7) score += 2
            else if (daysSinceLastAccess < 30) score += 1
            // Importance
            score += memory.importance
            score
        }.take(limit)
    }
}
