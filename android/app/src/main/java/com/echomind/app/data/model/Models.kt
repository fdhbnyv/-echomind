package com.echomind.app.data.model

import kotlinx.serialization.Serializable

/**
 * EchoMind template types — maps to template files in project root.
 */
enum class TemplateType(val id: String, val displayName: String) {
    AUTO("auto", "✨ 智能识别"),
    QUICK_IDEA("quick-idea", "碎片想法"),
    DAILY_REVIEW("daily-review", "每日复盘"),
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
 * 语音识别引擎模式
 */
enum class AsrEngineMode(val id: String, val displayName: String, val description: String) {
    LOCAL("local", "本地离线 (Sherpa-ONNX)", "0 流量、端侧推理、隐私安全、极速出字"),
    CLOUD("cloud", "云端接口 (OpenAI Whisper)", "调用兼容 OpenAI 规范的语音转写端点"),
    AUTO("auto", "智能推荐 (本地优先)", "优先本地离线模型，未就绪时自动回退云端");

    companion object {
        fun fromId(id: String): AsrEngineMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LOCAL
    }
}

/**
 * App settings stored in DataStore.
 */
data class AppSettings(
    val openAiBaseUrl: String = "https://api.deepseek.com/v1",
    val openAiApiKey: String = "",
    val openAiModel: String = "deepseek-flash",
    val openAiCustomModels: String = "deepseek-flash,deepseek-v4-pro,deepseek-chat",
    val notionApiKey: String = "",
    val notionDatabaseId: String = "",
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val cloudSyncEnabled: Boolean = false,
    val preferredTemplate: TemplateType = TemplateType.AUTO,
    val asrMode: AsrEngineMode = AsrEngineMode.LOCAL,
    val livePreviewEnabled: Boolean = true,
    val selectedTheme: String = "liquid-glass",
    val isDarkMode: Boolean? = null, // null = follow system
    val autoSync: Boolean = true,
    val autoTitle: Boolean = true,
    val autoList: Boolean = true,
    val autoTags: Boolean = true,
    val silentStop: Boolean = true,
) {
    /** OpenAI 协议 AI 服务是否已配置完整 */
    val openAiConfigured: Boolean
        get() = openAiBaseUrl.isNotBlank() && openAiApiKey.isNotBlank() && openAiModel.isNotBlank()

    /** 云端同步是否已配置完整（开关 + URL + anon key） */
    val cloudSyncConfigured: Boolean
        get() = cloudSyncEnabled && supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}

/**
 * Chat message for LLM API calls.
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)