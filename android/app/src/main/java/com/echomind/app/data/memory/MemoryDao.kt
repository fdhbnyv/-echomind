package com.echomind.app.data.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories WHERE isActive = 1 ORDER BY importance DESC, createdAt DESC")
    fun getAllActiveMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): MemoryEntity?

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY importance DESC, createdAt DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE type = :type ORDER BY importance DESC, createdAt DESC")
    fun getMemoriesByType(type: String): Flow<List<MemoryEntity>>

    /** 按标签筛选（tags JSON 数组中包含任一标签） */
    @Query("SELECT * FROM memories WHERE isActive = 1 AND (tags LIKE :tag OR content LIKE '%' || :query || '%') ORDER BY importance DESC, lastAccessedAt DESC")
    fun searchMemories(tag: String, query: String): Flow<List<MemoryEntity>>

    /** 仅按标签筛选 */
    @Query("SELECT * FROM memories WHERE isActive = 1 AND tags LIKE :tag ORDER BY importance DESC, lastAccessedAt DESC")
    fun getMemoriesByTag(tag: String): Flow<List<MemoryEntity>>

    /** 增量更新访问记录 */
    @Query("UPDATE memories SET accessCount = accessCount + 1, lastAccessedAt = :now WHERE id = :id")
    suspend fun incrementAccess(id: Long, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Query("UPDATE memories SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT COUNT(*) FROM memories WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>

    /** 批量查询多个关键词的匹配记忆 */
    @Query("""
        SELECT * FROM memories 
        WHERE isActive = 1 
        AND (content LIKE '%' || :keyword || '%' 
             OR tags LIKE '%' || :keyword || '%')
        ORDER BY importance DESC, lastAccessedAt DESC
        LIMIT 20
    """)
    fun searchByKeyword(keyword: String): Flow<List<MemoryEntity>>
}
