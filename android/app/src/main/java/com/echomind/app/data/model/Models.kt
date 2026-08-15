package com.echomind.app.data.model

import kotlinx.serialization.Serializable

/**
 * EchoMind template types — maps to template files in project root.
 */
enum class TemplateType(val id: String, val displayName: String) {
    DAILY_REVIEW("daily-review", "每日复盘"),
    QUICK_IDEA("quick-idea", "碎片想法"),
    MEETING_NOTES("meeting-notes", "会议纪要");
}

/**
 * Structured output from GPT after processing voice transcription.
 */
@Serializable
data class StructuredNote(
    val templateType: String,
    val title: String,
    val date: String,
    val summary: String,
    val accomplishments: List<String> = emptyList(),
    val challenges: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val ideas: List<IdeaEntry> = emptyList(),
    val schedule: List<ScheduleEntry> = emptyList(),
    val mood: String? = null,
    val tags: List<String> = emptyList(),
    val rawTranscription: String = "",
)

@Serializable
data class IdeaEntry(
    val title: String,
    val description: String,
    val tags: List<String> = emptyList(),
)

@Serializable
data class ScheduleEntry(
    val title: String,
    val dateTime: String? = null,
    val description: String? = null,
)

/**
 * State for the recording pipeline.
 */
enum class RecordingState {
    IDLE,
    RECORDING,
    TRANSCRIBING,
    STRUCTURING,
    COMPLETED,
    ERROR,
}

/**
 * App settings stored in DataStore.
 */
data class AppSettings(
    val notionApiKey: String = "",
    val notionDatabaseId: String = "",
    val preferredTemplate: TemplateType = TemplateType.DAILY_REVIEW,
    val selectedTheme: String = "liquid-glass",
    val isDarkMode: Boolean? = null, // null = follow system
    val autoSync: Boolean = true,
    val autoTitle: Boolean = true,
    val autoList: Boolean = true,
    val autoTags: Boolean = true,
    val silentStop: Boolean = true,
)

/**
 * Chat message for LLM API calls.
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)