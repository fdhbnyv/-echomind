package com.echomind.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * 云端删除墓碑队列。
 *
 * 本地删除笔记/记忆后先在此排队，由 SyncWorker 推送到 Supabase
 * 执行真正的远端 DELETE，成功后移除墓碑。uuid 为空（未同步过的
 * 本地行）无需入队。
 */
@Entity(tableName = "cloud_deletes")
data class CloudDeleteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "note" 或 "memory" */
    val entityType: String,
    val uuid: String,
    val deletedAt: Long = System.currentTimeMillis(),
)

@Dao
interface CloudDeleteDao {

    @Query("SELECT * FROM cloud_deletes ORDER BY deletedAt ASC")
    suspend fun getAll(): List<CloudDeleteEntity>

    @Insert
    suspend fun insert(item: CloudDeleteEntity): Long

    @Query("DELETE FROM cloud_deletes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM cloud_deletes")
    suspend fun deleteAll()
}
