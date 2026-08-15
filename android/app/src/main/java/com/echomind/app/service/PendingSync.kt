package com.echomind.app.service

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room entity for offline queue — pending sync items
 * stored when API calls fail due to network issues.
 */
@Entity(tableName = "pending_sync")
data class PendingSync(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateType: String,
    val transcription: String,
    val structuredNoteJson: String,
    val notionDbId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
)

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSync>

    @Query("SELECT COUNT(*) FROM pending_sync")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingSync): Long

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()

    @Query("UPDATE pending_sync SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)
}

@Database(entities = [PendingSync::class], version = 1, exportSchema = false)
abstract class PendingSyncDatabase : RoomDatabase() {
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile private var INSTANCE: PendingSyncDatabase? = null
        fun getInstance(context: Context): PendingSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    PendingSyncDatabase::class.java,
                    "echomind_pending_sync"
                ).build()
                INSTANCE = db
                db
            }
        }
    }
}
