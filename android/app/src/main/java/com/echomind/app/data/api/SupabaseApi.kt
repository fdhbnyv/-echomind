package com.echomind.app.data.api

import com.echomind.app.data.model.IdeaEntry
import com.echomind.app.data.model.ScheduleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Supabase REST (PostgREST) 客户端。
 *
 * 与 NotionApi 同风格：手写 OkHttp + kotlinx.serialization。
 * 对应云端表结构见 docs/supabase-schema.sql。
 *
 * 认证：anon key 即凭据（见 schema 头部安全说明）。
 * 增量拉取：updated_at（bigint 毫秒）+ gte 游标，last-write-wins 合并。
 */
class SupabaseApi(
    baseUrl: String,
    private val anonKey: String,
) {

    /** 兼容用户粘贴 https://xxxx.supabase.co 或带 /rest/v1 的完整地址 */
    private val restBase: String =
        baseUrl.trim().trimEnd('/').removeSuffix("/rest/v1") + "/rest/v1"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
    private val mediaType = "application/json".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Upsert ──

    /** 批量 upsert 笔记（按 uuid 冲突合并） */
    suspend fun upsertNotes(rows: List<SupabaseNoteRow>): Result<Unit> =
        upsert(
            table = "notes",
            body = json.encodeToString(ListSerializer(SupabaseNoteRow.serializer()), rows),
        )

    /** 批量 upsert 记忆（按 uuid 冲突合并） */
    suspend fun upsertMemories(rows: List<SupabaseMemoryRow>): Result<Unit> =
        upsert(
            table = "memories",
            body = json.encodeToString(ListSerializer(SupabaseMemoryRow.serializer()), rows),
        )

    private suspend fun upsert(table: String, body: String): Result<Unit> = try {
        val request = Request.Builder()
            .url("$restBase/$table?on_conflict=uuid")
            .apiHeaders()
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toRequestBody(mediaType))
            .build()
        execute(request)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Fetch ──

    /** 拉取 updatedAt >= since 的笔记，按 updated_at 升序 */
    suspend fun fetchNotes(since: Long): Result<List<SupabaseNoteRow>> =
        fetch("notes", since) {
            json.decodeFromString(ListSerializer(SupabaseNoteRow.serializer()), it)
        }

    /** 拉取 updatedAt >= since 的记忆，按 updated_at 升序 */
    suspend fun fetchMemories(since: Long): Result<List<SupabaseMemoryRow>> =
        fetch("memories", since) {
            json.decodeFromString(ListSerializer(SupabaseMemoryRow.serializer()), it)
        }

    private suspend fun <T> fetch(
        table: String,
        since: Long,
        parse: (String) -> List<T>,
    ): Result<List<T>> {
        return fetchRaw(table, since).map { parse(it) }
    }

    private suspend fun fetchRaw(table: String, since: Long): Result<String> = try {
        val url = "$restBase/$table".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("select", "*")
            ?.addQueryParameter("updated_at", "gte.$since")
            ?.addQueryParameter("order", "updated_at.asc")
            ?.addQueryParameter("limit", "1000")
            ?.build()
            ?: return Result.failure(IllegalArgumentException("Invalid Supabase URL: $restBase"))
        val request = Request.Builder().url(url).apiHeaders().get().build()
        executeRaw(request)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Delete ──

    /** 按 uuid 批量删除笔记（PostgREST query filter，无请求体） */
    suspend fun deleteNotes(uuids: List<String>): Result<Unit> = deleteByUuids("notes", uuids)

    /** 按 uuid 批量删除记忆 */
    suspend fun deleteMemories(uuids: List<String>): Result<Unit> = deleteByUuids("memories", uuids)

    private suspend fun deleteByUuids(table: String, uuids: List<String>): Result<Unit> = try {
        // PostgREST in.("a","b") 语法；引号/逗号经 query 编码传输
        val filter = "in.(" + uuids.joinToString(",") { "\"$it\"" } + ")"
        val url = "$restBase/$table".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("uuid", filter)
            ?.build()
            ?: return Result.failure(IllegalArgumentException("Invalid Supabase URL: $restBase"))
        val request = Request.Builder().url(url).apiHeaders().build()
        execute(request)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── HTTP 基础 ──

    private fun Request.Builder.apiHeaders(): Request.Builder = this
        .header("apikey", anonKey)
        .header("Authorization", "Bearer $anonKey")

    /** 成功时响应体已读取/关闭（return=minimal / DELETE 204 无响应体） */
    private suspend fun execute(request: Request): Result<Unit> {
        return executeRaw(request).map { }
    }

    private suspend fun executeRaw(request: Request): Result<String> {
        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
        response.use {
            if (!it.isSuccessful) {
                return Result.failure(Exception("Supabase API error: ${it.code} ${it.body?.string()?.take(300)}"))
            }
            return Result.success(it.body?.string() ?: "")
        }
    }
}

// ── 云端行 DTO（@SerialName 对应 Postgres 列名） ──

@Serializable
data class SupabaseNoteRow(
    val uuid: String,
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("template_type") val templateType: String,
    val title: String,
    val date: String,
    val summary: String = "",
    val accomplishments: List<String> = emptyList(),
    val challenges: List<String> = emptyList(),
    @SerialName("action_items") val actionItems: List<String> = emptyList(),
    @SerialName("key_points") val keyPoints: List<String> = emptyList(),
    val ideas: List<IdeaEntry> = emptyList(),
    val schedule: List<ScheduleEntry> = emptyList(),
    val mood: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("raw_transcription") val rawTranscription: String = "",
    @SerialName("is_voice") val isVoice: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class SupabaseMemoryRow(
    val uuid: String,
    @SerialName("device_id") val deviceId: String = "",
    val content: String,
    val category: String,
    val type: String,
    val tags: List<String> = emptyList(),
    val importance: Int = 3,
    val source: String = "manual",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("last_accessed_at") val lastAccessedAt: Long,
    @SerialName("access_count") val accessCount: Int = 0,
    @SerialName("updated_at") val updatedAt: Long,
)
