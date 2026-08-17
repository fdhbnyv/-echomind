package com.echomind.app.data.api

import com.echomind.app.data.model.ChatMessage
import com.echomind.app.data.model.StructuredNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * DashScope (通义千问) API 客户端。
 * 支持 Qwen 文本生成 + Paraformer 语音识别。
 *
 * API Key 从 ApiKeyProvider 内置获取，无需外部传入。
 */
class DashScopeApi {

    private val apiKey: String = ApiKeyProvider.dashScopeKey
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://dashscope.aliyuncs.com"

    // ========================================================================
    // 1. Paraformer 语音转写
    // ========================================================================

    /**
     * 上传音频文件 → 创建转写任务 → 轮询结果
     */
    suspend fun transcribe(audioFile: File, language: String = "zh"): Result<String> {
        return try {
            // Step 1: 上传文件
            val fileId = uploadFile(audioFile)
                ?: return Result.failure(Exception("文件上传失败"))

            // Step 2: 创建转写任务
            val taskId = createTranscriptionTask(fileId, language)
                ?: return Result.failure(Exception("创建转写任务失败"))

            // Step 3: 轮询结果（最多 60 秒）
            val result = pollTranscriptionResult(taskId, maxRetries = 30, intervalMs = 2000)
                ?: return Result.failure(Exception("转写超时或失败"))

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadFile(file: File): String? = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("purpose", "file-ext")
            .addFormDataPart(
                "file", file.name,
                file.asRequestBody("audio/m4a".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$baseUrl/api/v1/files")
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string() ?: ""
            // 如果文件上传失败尝试直接使用同步识别
            return@withContext null
        }
        val respJson = json.decodeFromString<DashScopeFileResponse>(response.body!!.string())
        respJson.output?.fileId
    }

    private suspend fun createTranscriptionTask(fileId: String, language: String): String? = withContext(Dispatchers.IO) {
        val taskBody = TranscriptionTaskRequest(
            model = "paraformer-v2",
            input = TranscriptionInput(source = fileId),
            parameters = TranscriptionParams(languageHints = listOf(language)),
        )
        val bodyJson = json.encodeToString(TranscriptionTaskRequest.serializer(), taskBody)

        val request = Request.Builder()
            .url("$baseUrl/api/v1/services/audio/transcription/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(bodyJson.toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null
        val respJson = json.decodeFromString<TranscriptionTaskResponse>(response.body!!.string())
        respJson.output?.taskId
    }

    private suspend fun pollTranscriptionResult(taskId: String, maxRetries: Int, intervalMs: Long): String? {
        for (i in 0 until maxRetries) {
            kotlinx.coroutines.delay(intervalMs)
            val result = getTranscriptionResult(taskId)
            if (result != null && result.isNotEmpty()) return result
        }
        return null
    }

    private suspend fun getTranscriptionResult(taskId: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/v1/services/audio/transcription/transcriptions/$taskId")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null
        val respJson = json.decodeFromString<TranscriptionResultResponse>(response.body!!.string())
        when (respJson.output?.taskStatus) {
            "SUCCEEDED" -> respJson.output.result?.sentences?.joinToString("") { it.text }
            "FAILED" -> null
            else -> null // RUNNING/PENDING — 继续轮询
        }
    }

    // ========================================================================
    // 2. Qwen 文本生成（替代 GPT）
    // ========================================================================

    /**
     * 使用 Qwen 模型将转写文本结构化为笔记。
     */
    suspend fun structureNote(
        transcription: String,
        templateType: String = "auto",
        model: String = "qwen-max",
    ): Result<StructuredNote> {
        return try {
            val systemPrompt = TemplatePrompts.getPrompt(templateType)
            val today = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.getDefault()).format(java.util.Date())
            val userMessage = "今天是$today。\n\n$transcription"

            val qwenRequest = QwenRequest(
                model = model,
                input = QwenInput(
                    messages = listOf(
                        ChatMessage(role = "system", content = systemPrompt),
                        ChatMessage(role = "user", content = userMessage),
                    )
                ),
                parameters = QwenParameters(
                    temperature = 0.3,
                    resultFormat = "message",
                ),
            )

            val bodyJson = json.encodeToString(QwenRequest.serializer(), qwenRequest)

            val request = Request.Builder()
                .url("$baseUrl/api/v1/services/aigc/text-generation/generation")
                .header("Authorization", "Bearer $apiKey")
                .post(bodyJson.toRequestBody(mediaType))
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception("DashScope API error: ${response.code} ${response.body?.string()}")
                )
            }

            val responseBody = response.body!!.string()
            val completion = json.decodeFromString<QwenResponse>(responseBody)
            val content = completion.output?.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty Qwen response"))

            val note = json.decodeFromString<StructuredNote>(content)
            Result.success(note.copy(rawTranscription = transcription))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ========================================================================
// DTOs — DashScope API
// ========================================================================

// --- File Upload ---
@Serializable
data class DashScopeFileResponse(
    val output: DashScopeFileOutput? = null,
)

@Serializable
data class DashScopeFileOutput(
    val fileId: String? = null,
)

// --- Transcription Task ---
@Serializable
data class TranscriptionTaskRequest(
    val model: String,
    val input: TranscriptionInput,
    val parameters: TranscriptionParams? = null,
)

@Serializable
data class TranscriptionInput(
    val source: String,
)

@Serializable
data class TranscriptionParams(
    val languageHints: List<String>? = null,
)

@Serializable
data class TranscriptionTaskResponse(
    val output: TranscriptionTaskOutput? = null,
)

@Serializable
data class TranscriptionTaskOutput(
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("task_status") val taskStatus: String? = null,
    val result: TranscriptionResult? = null,
)

@Serializable
data class TranscriptionResult(
    val sentences: List<TranscriptionSentence>? = null,
)

@Serializable
data class TranscriptionSentence(
    val text: String = "",
)

@Serializable
data class TranscriptionResultResponse(
    val output: TranscriptionTaskOutput? = null,
)

// --- Qwen Chat ---
@Serializable
data class QwenRequest(
    val model: String,
    val input: QwenInput,
    val parameters: QwenParameters? = null,
)

@Serializable
data class QwenInput(
    val messages: List<ChatMessage>,
)

@Serializable
data class QwenParameters(
    val temperature: Double = 0.3,
    @SerialName("result_format") val resultFormat: String = "text",
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
data class QwenResponse(
    val output: QwenOutput? = null,
    val usage: QwenUsage? = null,
)

@Serializable
data class QwenOutput(
    val text: String? = null,
    val choices: List<QwenChoice>? = null,
)

@Serializable
data class QwenChoice(
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class QwenUsage(
    @SerialName("output_tokens") val outputTokens: Int = 0,
    @SerialName("input_tokens") val inputTokens: Int = 0,
)
