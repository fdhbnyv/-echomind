package com.echomind.app.data.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * EchoMind 记忆系统 — 持久化存储用户偏好、事实、上下文。
 *
 * 类似 Hermes 的记忆架构：
 *   - 自动从笔记中抽取记忆（每日复盘的情绪/行动项、会议纪要的参与人）
 *   - 用户手动维护记忆
 *   - LLM 调用时可注入相关记忆作为 context
 *   - 支持标签分类、重要性评分、模糊搜索
 */

/** 记忆分类 */
enum class MemoryCategory(val label: String, val emoji: String) {
    USER_FACTS("用户事实", "👤"),
    PREFERENCES("偏好规则", "⚙️"),
    CONTEXT("上下文", "📌"),
    ENVIRONMENT("环境信息", "🌍"),
    TOOLS("工具技能", "🛠️"),
    NOTES("笔记关联", "📝"),
}

/** 记忆类型 */
enum class MemoryType(val label: String) {
    FACT("事实"),
    PREFERENCE("偏好"),
    RULE("规则"),
    CONTEXT("上下文"),
    ENVIRONMENT("环境"),
    SKILL("技能"),
}

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 记忆内容主体 */
    val content: String,
    /** 所属分类 */
    val category: String, // MemoryCategory.name
    /** 类型 */
    val type: String, // MemoryType.name
    /** 标签（JSON 数组字符串，用于快速过滤） */
    val tags: String = "[]",
    /** 重要性 1-5，5 为最高 */
    val importance: Int = 3,
    /** 来源（auto 或 manual） */
    val source: String = "manual",
    /** 是否活跃（可软删除） */
    val isActive: Boolean = true,
    /** 创建时间戳 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 最后访问时间戳 */
    val lastAccessedAt: Long = System.currentTimeMillis(),
    /** 访问次数 */
    val accessCount: Int = 0,
)

fun MemoryEntity.toMemory(): Memory = Memory(
    id = id,
    content = content,
    category = category,
    type = type,
    tags = safeParseJsonList(tags),
    importance = importance,
    source = source,
    isActive = isActive,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt,
    accessCount = accessCount,
)

fun Memory.toEntity(id: Long = 0L): MemoryEntity = MemoryEntity(
    id = id,
    content = content,
    category = category,
    type = type,
    tags = tags.toJsonListString(),
    importance = importance,
    source = source,
    isActive = isActive,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt,
    accessCount = accessCount,
)

// ── Domain model ──

data class Memory(
    val id: Long,
    val content: String,
    val category: String,
    val type: String,
    val tags: List<String> = emptyList(),
    val importance: Int = 3,
    val source: String = "manual",
    val isActive: Boolean = true,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val accessCount: Int = 0,
) {
    val categoryEnum: MemoryCategory?
        get() = MemoryCategory.entries.find { it.name == category }
    val typeEnum: MemoryType?
        get() = MemoryType.entries.find { it.name == type }
}

// ── JSON helpers ──

private fun safeParseJsonList(jsonStr: String): List<String> {
    if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
    return try {
        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()), jsonStr)
    } catch (_: Exception) { emptyList() }
}

private fun List<String>.toJsonListString(): String {
    if (isEmpty()) return "[]"
    return try {
        kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()), this
        )
    } catch (_: Exception) { "[]" }
}
