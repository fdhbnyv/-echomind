package com.echomind.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' OR rawTranscription LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE templateType = :templateType ORDER BY createdAt DESC")
    fun getNotesByTemplate(templateType: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE templateType = :templateType AND (title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' OR rawTranscription LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchNotesByTemplate(templateType: String, query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("SELECT COUNT(*) FROM notes")
    fun getNoteCount(): Flow<Int>

    // ── 云端同步 ──

    @Query("SELECT * FROM notes WHERE cloudSynced = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getUnsyncedNotes(limit: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE uuid = :uuid LIMIT 1")
    suspend fun getNoteByUuid(uuid: String): NoteEntity?

    @Query("UPDATE notes SET cloudSynced = 1 WHERE uuid IN (:uuids)")
    suspend fun markSynced(uuids: List<String>)

    @Query("UPDATE notes SET cloudSynced = 0 WHERE id = :id")
    suspend fun markUnsynced(id: Long)

    @Query("UPDATE notes SET updatedAt = createdAt WHERE updatedAt = 0")
    suspend fun backfillUpdatedAt()

    @Query("SELECT uuid FROM notes WHERE uuid != ''")
    suspend fun getAllUuids(): List<String>
}
