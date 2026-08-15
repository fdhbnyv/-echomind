package com.echomind.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.echomind.app.data.model.IdeaEntry
import com.echomind.app.data.model.ScheduleEntry
import com.echomind.app.data.model.StructuredNote
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Room entity for persisted voice/text notes.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateType: String,
    val title: String,
    val date: String,
    val summary: String,
    val accomplishments: String,
    val challenges: String,
    val actionItems: String,
    val keyPoints: String,
    val ideas: String,
    val schedule: String,
    val mood: String?,
    val tags: String,
    val rawTranscription: String,
    val isVoice: Boolean,
    val synced: Boolean,
    val createdAt: Long,
)

/**
 * Convert NoteEntity back to StructuredNote domain model.
 */
fun NoteEntity.toStructuredNote(): StructuredNote {
    val json = Json { ignoreUnknownKeys = true }
    return StructuredNote(
        templateType = templateType,
        title = title,
        date = date,
        summary = summary,
        accomplishments = safeParseStringList(json, accomplishments),
        challenges = safeParseStringList(json, challenges),
        actionItems = safeParseStringList(json, actionItems),
        keyPoints = safeParseStringList(json, keyPoints),
        ideas = safeParseTypedList(json, ideas, serializer<IdeaEntry>()),
        schedule = safeParseTypedList(json, schedule, serializer<ScheduleEntry>()),
        mood = mood,
        tags = safeParseStringList(json, tags),
        rawTranscription = rawTranscription,
    )
}

private fun safeParseStringList(json: Json, value: String): List<String> {
    if (value.isBlank() || value == "[]") return emptyList()
    return try {
        json.decodeFromString(ListSerializer(serializer<String>()), value)
    } catch (_: Exception) { emptyList() }
}

private fun <T> safeParseTypedList(json: Json, value: String, serializer: KSerializer<T>): List<T> {
    if (value.isBlank() || value == "[]") return emptyList()
    return try {
        json.decodeFromString(ListSerializer(serializer), value)
    } catch (_: Exception) { emptyList() }
}
