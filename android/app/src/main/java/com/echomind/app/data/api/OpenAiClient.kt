package com.echomind.app.data.api

import com.echomind.app.data.memory.MemoryInjector
import com.echomind.app.data.memory.MemoryRepository
import com.echomind.app.data.model.ChatMessage
import com.echomind.app.data.model.StructuredNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 通用 OpenAI 兼容协议客户端。
 *
 * 支持任何兼容 OpenAI 规范的 API 接口：
 * - OpenAI 官方 (https://api.openai.com/v1)
 * - DeepSeek (https://api.deepseek.com/v1)
 * - 阿里云百炼兼容模式 (https://dashscope.aliyuncs.com/compatible-mode/v1)
 * - 硅基流动 SiliconFlow (https://api.siliconflow.cn/v1)
 * - OpenRouter (https://openrouter.ai/api/v1)
 * - 本地 Ollama (http://10.0.2.2:11434/v1) / OneAPI / NewAPI / FastGPT 等中转
 */
class OpenAiClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        fun normalizeBaseUrl(url: String): String {
            var u = url.trim().trimEnd('/')
            if (u.isEmpty()) return "https://api.openai.com/v1"
            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                u = "https://$u"
            }
            return u
        }
    }

    // ========================================================================
    // 1. 测试连接并拉取模型列表 (GET /models)
    // ========================================================================

    suspend fun testConnectionAndFetchModels(
        baseUrl: String,
        apiKey: String,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val root = normalizeBaseUrl(baseUrl)
        val url = "$root/models"

        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = when (code) {
                    401 -> "认证失败 (HTTP 401)：API Key 无效或未提供"
                    403 -> "权限不足 (HTTP 403)：该 Key 无权访问模型列表"
                    404 -> "端点未找到 (HTTP 404)：请检查接口基础地址是否正确（如缺少 /v1）"
                    500, 502, 503 -> "服务端错误 (HTTP $code)：代理或 AI 接口服务异常"
                    else -> "连接失败 (HTTP $code): ${bodyString.take(120)}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            // 解析模型列表
            val parsedModels = parseModelList(bodyString)
            if (parsedModels.isEmpty()) {
                return@withContext Result.success(listOf("gpt-4o", "gpt-4o-mini", "deepseek-chat"))
            }

            Result.success(parsedModels)
        } catch (e: Exception) {
            Result.failure(Exception("无法连接到接口: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun parseModelList(jsonStr: String): List<String> {
        val modelIds = mutableListOf<String>()
        try {
            val element = json.parseToJsonElement(jsonStr)
            val obj = element.jsonObject

            // OpenAI 标准格式: {"data": [{"id": "gpt-4o"}, ...]}
            if (obj.containsKey("data")) {
                val dataArr = obj["data"]?.jsonArray
                dataArr?.forEach { item ->
                    item.jsonObject["id"]?.jsonPrimitive?.content?.let { modelIds.add(it) }
                }
            }
            // Ollama / 其它扩展格式: {"models": [{"name": "qwen2.5:7b"}, ...]}
            else if (obj.containsKey("models")) {
                val modelsArr = obj["models"]?.jsonArray
                modelsArr?.forEach { item ->
                    val id = item.jsonObject["name"]?.jsonPrimitive?.content
                        ?: item.jsonObject["id"]?.jsonPrimitive?.content
                    if (id != null) modelIds.add(id)
                }
            }
        } catch (_: Exception) {}

        return modelIds.distinct().sortedWith { a, b ->
            val priorityA = getModelPriority(a)
            val priorityB = getModelPriority(b)
            if (priorityA != priorityB) priorityA.compareTo(priorityB)
            else a.compareTo(b, ignoreCase = true)
        }
    }

    private fun getModelPriority(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("deepseek-flash") || lower.contains("deepseek-v4") -> 0
            lower.contains("deepseek-chat") || lower.contains("deepseek-v3") -> 1
            lower.contains("gpt-4o") || lower.contains("claude-3-5") || lower.contains("qwen") -> 2
            lower.contains("gpt-4") || lower.contains("deepseek-r1") -> 3
            lower.contains("gpt-3.5") || lower.contains("mini") || lower.contains("flash") -> 4
            lower.contains("whisper") || lower.contains("tts") || lower.contains("embed") -> 8
            else -> 5
        }
    }

    // ========================================================================
    // 2. 文本/转写结构化生成 (POST /chat/completions)
    // ========================================================================

    suspend fun structureNote(
        transcription: String,
        templateType: String = "daily-review",
        baseUrl: String,
        apiKey: String,
        model: String,
        memoryRepository: MemoryRepository? = null,
    ): Result<StructuredNote> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext Result.failure(Exception("请先在设置中填写 API 接口地址"))
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("请先在设置中填写 API Key"))
        val targetModel = model.ifBlank { "gpt-4o-mini" }

        try {
            val systemPrompt = TemplatePrompts.getPrompt(templateType)
            val today = SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date())
            val userMessage = "今天是$today。\n\n$transcription"

            val finalPrompt = if (memoryRepository != null) {
                MemoryInjector.injectQuick(systemPrompt, memoryRepository)
            } else systemPrompt

            val root = normalizeBaseUrl(baseUrl)
            val url = "$root/chat/completions"

            val requestDto = OpenAiChatRequest(
                model = targetModel,
                messages = listOf(
                    ChatMessage(role = "system", content = finalPrompt),
                    ChatMessage(role = "user", content = userMessage),
                ),
                temperature = 0.3,
            )

            val bodyJson = json.encodeToString(OpenAiChatRequest.serializer(), requestDto)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .post(bodyJson.toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("AI 接口返回错误 (HTTP $code): ${bodyString.take(200)}")
                )
            }

            val chatResp = json.decodeFromString<OpenAiChatResponse>(bodyString)
            val content = chatResp.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("AI 未返回有效内容"))

            val cleanedJson = extractJson(content)
            val note = json.decodeFromString<StructuredNote>(cleanedJson)
            val finalTemplate = if (templateType == "auto") {
                note.templateType.ifBlank { "quick-idea" }
            } else {
                templateType
            }
            val finalDate = if (note.date.isBlank()) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            } else note.date
            Result.success(
                note.copy(
                    rawTranscription = transcription,
                    templateType = finalTemplate,
                    date = finalDate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================================================
    // 3. 语音转写 (POST /audio/transcriptions)
    // ========================================================================

    suspend fun transcribe(
        audioFile: File,
        baseUrl: String,
        apiKey: String,
        model: String = "whisper-1",
        language: String = "zh",
    ): Result<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext Result.failure(Exception("请在设置中配置 API 接口地址"))
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("请在设置中配置 API Key"))

        try {
            val root = normalizeBaseUrl(baseUrl)
            val url = "$root/audio/transcriptions"

            val audioMediaType = if (audioFile.name.endsWith(".wav", ignoreCase = true)) {
                "audio/wav".toMediaType()
            } else {
                "audio/m4a".toMediaType()
            }

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", model.ifBlank { "whisper-1" })
                .addFormDataPart("language", language)
                .addFormDataPart("response_format", "json")
                .addFormDataPart(
                    "file", audioFile.name,
                    audioFile.asRequestBody(audioMediaType)
                )
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("语音转写失败 (HTTP $code): ${bodyString.take(150)}")
                )
            }

            val transcriptionResp = json.decodeFromString<OpenAiTranscriptionResponse>(bodyString)
            val text = transcriptionResp.text?.trim()
            if (text.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("未能识别出语音内容，请重试"))
            }

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJson(text: String): String {
        val trimmed = text.trim()
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```").find(trimmed)
        val candidate = if (fenced != null) {
            fenced.groupValues[1].trim()
        } else {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start >= 0 && end > start) {
                trimmed.substring(start, end + 1)
            } else trimmed
        }

        // 处理大模型嵌套包裹在 {"type": "json_object", "content": {...}} 的情况
        return try {
            val element = json.parseToJsonElement(candidate)
            if (element is kotlinx.serialization.json.JsonObject && element.containsKey("content")) {
                val inner = element["content"]
                if (inner is kotlinx.serialization.json.JsonObject && (inner.containsKey("title") || inner.containsKey("summary") || inner.containsKey("templateType"))) {
                    json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), inner)
                } else candidate
            } else candidate
        } catch (_: Exception) {
            candidate
        }
    }
}

// ========================================================================
// OpenAI DTOs
// ========================================================================

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3,
)

@Serializable
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage? = null,
)

@Serializable
data class OpenAiMessage(
    val role: String = "assistant",
    val content: String? = null,
)

@Serializable
data class OpenAiTranscriptionResponse(
    val text: String? = null,
)
