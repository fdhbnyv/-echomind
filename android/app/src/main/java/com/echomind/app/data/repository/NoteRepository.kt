package com.echomind.app.data.repository

import android.content.Context
import com.echomind.app.data.local.EchoMindDatabase
import com.echomind.app.data.local.NoteEntity
import com.echomind.app.data.local.toStructuredNote
import com.echomind.app.data.model.StructuredNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

// ── StructuredNote → Markdown ──

/** Generate a Markdown string (with YAML frontmatter) for Obsidian/Notion compatibility. */
fun StructuredNote.toMarkdown(): String = buildString {
    // YAML frontmatter
    appendLine("---")
    appendLine("title: \"${title.replace("\"", "\\\"")}\"")
    appendLine("date: $date")
    appendLine("type: $templateType")
    appendLine("tags: [${tags.joinToString(", ")}]")
    mood?.let { appendLine("mood: $it") }
    appendLine("created_at: ${System.currentTimeMillis()}")
    appendLine("---")
    appendLine()
    appendLine("# $title")
    appendLine()

    if (summary.isNotBlank()) {
        appendLine("> $summary")
        appendLine()
    }

    if (accomplishments.isNotEmpty()) {
        appendLine("## \u2705 完成事项")
        accomplishments.forEach { appendLine("- $it") }
        appendLine()
    }

    if (challenges.isNotEmpty()) {
        appendLine("## \u26A0\uFE0F 挑战")
        challenges.forEach { appendLine("- $it") }
        appendLine()
    }

    if (actionItems.isNotEmpty()) {
        appendLine("## \uD83C\uDFAF 行动项")
        actionItems.forEach { appendLine("- [ ] $it") }
        appendLine()
    }

    if (keyPoints.isNotEmpty()) {
        appendLine("## \uD83D\uDCAC 要点")
        keyPoints.forEach { appendLine("- $it") }
        appendLine()
    }

    if (ideas.isNotEmpty()) {
        appendLine("## \uD83D\uDCA1 想法")
        ideas.forEach { appendLine("- ${it.title}: ${it.description}") }
        appendLine()
    }

    // Raw transcription as blockquote
    if (rawTranscription.isNotBlank()) {
        appendLine("---")
        appendLine()
        appendLine("## \uD83D\uDCDD 原始记录")
        appendLine()
        appendLine("> ${rawTranscription.replace("\n", "\n> ")}")
        appendLine()
    }
}

/** Safe filename from note title and date */
fun StructuredNote.toMarkdownFileName(): String {
    val safeTitle = title
        .replace(Regex("[/\\\\:*?\"<>|]"), "_")
        .take(60)
        .ifBlank { "note" }
    return "${date}_${safeTitle}.md"
}

/** Full path to the .md file */
fun StructuredNote.toMarkdownFilePath(context: Context): String {
    val dir = File(context.filesDir, "notes")
    return File(dir, toMarkdownFileName()).absolutePath
}

// ── NoteRepository ──

class NoteRepository(private val context: Context) {

    private val db = EchoMindDatabase.getInstance(context)
    private val dao = db.noteDao()
    private val json = Json { ignoreUnknownKeys = true }

    /** Ensure notes directory exists */
    private fun notesDir(): File {
        val dir = File(context.filesDir, "notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** All notes ordered by recency */
    val allNotes: Flow<List<StructuredNote>> = dao.getAllNotes().map { list ->
        list.map { it.toStructuredNote() }
    }

    /** All note entities (with db metadata) */
    val allEntities: Flow<List<NoteEntity>> = dao.getAllNotes()

    /** Load a single note by id */
    suspend fun getNoteById(id: Long): StructuredNote? {
        return dao.getNoteById(id)?.toStructuredNote()
    }

    /** Note count */
    val noteCount: Flow<Int> = dao.getNoteCount()

    /** Search notes by query */
    fun searchNotes(query: String): Flow<List<StructuredNote>> {
        return dao.searchNotes(query).map { list -> list.map { it.toStructuredNote() } }
    }

    /** Filter by template type */
    fun getNotesByTemplate(templateType: String): Flow<List<StructuredNote>> {
        return dao.getNotesByTemplate(templateType).map { list -> list.map { it.toStructuredNote() } }
    }

    /** Search within a template type */
    fun searchNotesByTemplate(templateType: String, query: String): Flow<List<StructuredNote>> {
        return dao.searchNotesByTemplate(templateType, query).map { list -> list.map { it.toStructuredNote() } }
    }

    /** Encode a list to JSON string */
    private inline fun <reified T> encodeList(list: List<T>): String {
        return json.encodeToString(ListSerializer(serializer<T>()), list)
    }

    /** Write .md file to disk (non-blocking IO) */
    private suspend fun writeMarkdownFile(note: StructuredNote) = withContext(Dispatchers.IO) {
        val dir = notesDir()
        val file = File(dir, note.toMarkdownFileName())
        file.writeText(note.toMarkdown(), Charsets.UTF_8)
    }

    /** Delete .md file from disk */
    private suspend fun deleteMarkdownFile(note: StructuredNote) = withContext(Dispatchers.IO) {
        val file = File(notesDir(), note.toMarkdownFileName())
        if (file.exists()) file.delete()
    }

    /** Save a note — Room DB + .md file */
    suspend fun saveNote(note: StructuredNote, isVoice: Boolean, synced: Boolean): Long {
        val id = dao.insertNote(
            NoteEntity(
                templateType = note.templateType,
                title = note.title,
                date = note.date,
                summary = note.summary,
                accomplishments = encodeList(note.accomplishments),
                challenges = encodeList(note.challenges),
                actionItems = encodeList(note.actionItems),
                keyPoints = encodeList(note.keyPoints),
                ideas = encodeList(note.ideas),
                schedule = encodeList(note.schedule),
                mood = note.mood,
                tags = encodeList(note.tags),
                rawTranscription = note.rawTranscription,
                isVoice = isVoice,
                synced = synced,
                createdAt = System.currentTimeMillis(),
            )
        )
        // Write .md file
        writeMarkdownFile(note)
        return id
    }

    /** Update an existing note — preserves id, isVoice, synced, createdAt */
    suspend fun updateNote(note: StructuredNote, id: Long = 0L) {
        val existing = if (id > 0) dao.getNoteById(id) else null
        val entity = NoteEntity(
            id = id,
            templateType = note.templateType,
            title = note.title,
            date = note.date,
            summary = note.summary,
            accomplishments = encodeList(note.accomplishments),
            challenges = encodeList(note.challenges),
            actionItems = encodeList(note.actionItems),
            keyPoints = encodeList(note.keyPoints),
            ideas = encodeList(note.ideas),
            schedule = encodeList(note.schedule),
            mood = note.mood,
            tags = encodeList(note.tags),
            rawTranscription = note.rawTranscription,
            isVoice = existing?.isVoice ?: false,
            synced = existing?.synced ?: false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        dao.updateNote(entity)
        // Re-write .md file
        writeMarkdownFile(note)
    }

    /** Delete a note from Room + .md file */
    suspend fun deleteNoteById(id: Long) {
        val entity = dao.getNoteById(id) ?: return
        dao.deleteNote(entity)
        // Remove .md file
        deleteMarkdownFile(entity.toStructuredNote())
    }

    /** Delete ALL notes + all .md files */
    suspend fun deleteAllNotes() {
        dao.deleteAllNotes()
        withContext(Dispatchers.IO) {
            val dir = notesDir()
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        }
    }
}
