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
}
