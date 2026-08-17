package com.echomind.app.data.memory

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 记忆注入工具 — 将相关记忆注入到 LLM prompt 中。
 * 类似 Hermes 的 "memory is injected into every turn" 机制。
 */
object MemoryInjector {

    /**
     * 构建带记忆的 system prompt。
     *
     * @param basePrompt 原始 system prompt（来自 TemplatePrompts）
     * @param contextTags 当前上下文的标签（如笔记的 tags、当前模板类型）
     * @param repository 记忆仓库
     */
    suspend fun inject(
        basePrompt: String,
        contextTags: List<String>,
        repository: MemoryRepository,
    ): String = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val memories = repository.getRelevantMemories(
            query = contextTags.joinToString(" "),
            existingTags = contextTags,
            limit = 8,
        )

        if (memories.isEmpty()) return@withContext basePrompt

        val memoryBlock = buildString {
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## 你的记忆（EchoMind 长期记忆系统）")
            appendLine("以下信息基于你过去的记录和用户手动维护的偏好，请结合使用：")
            appendLine()

            memories.forEachIndexed { index, memory ->
                val catEmoji = memory.categoryEnum?.emoji ?: "📌"
                val typeLabel = memory.typeEnum?.label ?: ""
                appendLine("${index + 1}. [$catEmoji $typeLabel] ${memory.content}")
                if (memory.tags.isNotEmpty()) {
                    appendLine("   标签: ${memory.tags.joinToString(", ")}")
                }
                appendLine()
            }

            appendLine("## 使用规则")
            appendLine("- 当你处理今天的笔记时，参考以上记忆使输出更个性化")
            appendLine("- 如果记忆与当前内容冲突，以当前内容为准")
            appendLine("- 不要直接输出记忆内容，而是将其融入理解")
        }

        "$basePrompt$memoryBlock"
    }

    /**
     * 快速版：仅按 tags 获取顶部记忆，不计算复杂度评分。
     */
    suspend fun injectQuick(
        basePrompt: String,
        repository: MemoryRepository,
    ): String = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val allMemories = repository.allMemories.first()
        if (allMemories.isEmpty()) return@withContext basePrompt

        val recent = allMemories.take(5)
        val memoryBlock = buildString {
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## 你的记忆")
            appendLine()
            recent.forEachIndexed { i, m ->
                appendLine("${i + 1}. ${m.content}")
            }
            appendLine()
            appendLine("参考以上信息使输出更贴合用户习惯。")
        }
        "$basePrompt$memoryBlock"
    }
}
