package com.echomind.app.data.api

import com.echomind.app.data.model.StructuredNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Notion REST API client.
 * POST /v1/pages — write structured notes to a Notion database.
 */
class NotionApi(private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Write a structured note to a Notion database.
     * @param databaseId the Notion database ID
     */
    suspend fun writeNote(
        note: StructuredNote,
        databaseId: String,
    ): Result<String> {
        return try {
            val properties = buildNotionProperties(note)
            val requestBody = NotionPageRequest(
                parent = Parent(databaseId = databaseId),
                properties = properties,
            )

            val bodyJson = json.encodeToString(NotionPageRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url("https://api.notion.com/v1/pages")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("Notion-Version", "2022-06-28")
                .post(bodyJson.toRequestBody(mediaType))
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception("Notion API error: ${response.code} ${response.body?.string()}")
                )
            }

            val responseBody = response.body!!.string()
            val pageResponse = json.decodeFromString<NotionPageResponse>(responseBody)
            Result.success(pageResponse.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildNotionProperties(note: StructuredNote): Map<String, NotionProperty> {
        return buildMap {
            put("title", NotionProperty(title = listOf(TitleText(text = TextContent(note.title)))))
            put("日期", NotionProperty(date = DateProperty(start = note.date)))
            put("类型", NotionProperty(select = SelectOption(name = note.templateType)))
            put("摘要", NotionProperty(richText = listOf(RichTextContent(text = TextContent(note.summary)))))

            if (note.accomplishments.isNotEmpty()) {
                put("完成事项", NotionProperty(richText = listOf(
                    RichTextContent(text = TextContent(note.accomplishments.joinToString("\n")))
                )))
            }
            if (note.actionItems.isNotEmpty()) {
                put("行动项", NotionProperty(richText = listOf(
                    RichTextContent(text = TextContent(note.actionItems.joinToString("\n")))
                )))
            }
            if (note.tags.isNotEmpty()) {
                put("标签", NotionProperty(multiSelect = note.tags.map { SelectOption(it) }))
            }
        }
    }
}

// --- Notion API DTOs ---

@Serializable
data class NotionPageRequest(
    val parent: Parent,
    val properties: Map<String, NotionProperty>,
)

@Serializable
data class Parent(
    val databaseId: String,
)

@Serializable
data class NotionProperty(
    val title: List<TitleText>? = null,
    val richText: List<RichTextContent>? = null,
    val date: DateProperty? = null,
    val select: SelectOption? = null,
    val multiSelect: List<SelectOption>? = null,
)

@Serializable
data class TitleText(
    val text: TextContent,
)

@Serializable
data class RichTextContent(
    val text: TextContent,
    val type: String = "text",
)

@Serializable
data class TextContent(
    val content: String,
)

@Serializable
data class DateProperty(
    val start: String,
    val end: String? = null,
)

@Serializable
data class SelectOption(
    val name: String,
    val color: String? = null,
)

@Serializable
data class NotionPageResponse(
    val id: String,
    val url: String? = null,
)
