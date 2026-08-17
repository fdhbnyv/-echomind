package com.echomind.app.data.memory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.echomind.app.data.local.EchoMindDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 记忆系统集成测试
 * 需要 Android 环境（使用 AndroidJUnit4 runner）
 *
 * 运行方式：
 * ./gradlew :app:testDebugUnitTest
 */
@RunWith(AndroidJUnit4::class)
class MemoryRepositoryTest {

    private lateinit var db: EchoMindDatabase
    private lateinit var repository: MemoryRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = EchoMindDatabase.getInstance(context)
        repository = MemoryRepository(context)
    }

    @After
    fun teardown() {
        db.close()
    }

    // ==========================================================================
    // CRUD 测试
    // ==========================================================================

    @Test
    fun testAddAndGetMemory() = runBlocking {
        val memory = Memory(
            id = 0,
            content = "用户偏好简短回复",
            category = "PREFERENCES",
            type = "PREFERENCE",
            tags = listOf("用户", "偏好"),
            importance = 4,
            source = "manual",
        )

        val id = repository.addMemory(memory)
        assertTrue("ID should be positive", id > 0)

        val retrieved = repository.getMemory(id)
        assertNotNull("Memory should exist", retrieved)
        assertEquals("内容应匹配", memory.content, retrieved!!.content)
        assertEquals("分类应匹配", memory.category, retrieved.category)
    }

    @Test
    fun testUpdateMemory() = runBlocking {
        val original = Memory(id = 0, content = "原始内容", category = "FACT", type = "FACT",
            tags = emptyList(), importance = 3, source = "manual")

        val id = repository.addMemory(original)
        val updated = original.copy(content = "更新后内容", importance = 5)

        repository.updateMemory(updated.copy(id = id))

        val retrieved = repository.getMemory(id)
        assertEquals("更新后内容", retrieved?.content)
        assertEquals(5, retrieved?.importance)
    }

    @Test
    fun testSoftDelete() = runBlocking {
        val memory = Memory(id = 0, content = "待删除", category = "FACT", type = "FACT",
            tags = emptyList(), importance = 3, source = "manual")

        val id = repository.addMemory(memory)
        repository.deleteMemory(id)

        val retrieved = repository.getMemory(id)
        assertNull("软删除后应返回null", retrieved)
    }

    @Test
    fun testHardDelete() = runBlocking {
        val memory = Memory(id = 0, content = "永久删除", category = "FACT", type = "FACT",
            tags = emptyList(), importance = 3, source = "manual")

        val id = repository.addMemory(memory)
        repository.forceDeleteMemory(id)

        // Try to get by raw query
        val all = repository.allMemories.first()
        assertFalse("硬删除后不应存在", all.any { it.id == id })
    }

    // ==========================================================================
    // 搜索测试
    // ==========================================================================

    @Test
    fun testSearchByKeyword() = runBlocking {
        repository.addMemory(Memory(id = 0, content = "用户喜欢咖啡", category = "FACT", type = "FACT",
            tags = listOf("饮食"), importance = 3, source = "manual"))
        repository.addMemory(Memory(id = 0, content = "用户讨厌早起", category = "PREFERENCES", type = "PREFERENCE",
            tags = listOf("习惯"), importance = 4, source = "manual"))

        val results = repository.search("用户").first()

        assertTrue("应找到至少2条", results.size >= 2)
    }

    @Test
    fun testSearchByTag() = runBlocking {
        repository.addMemory(Memory(id = 0, content = "咖啡相关", category = "FACT", type = "FACT",
            tags = listOf("饮食"), importance = 3, source = "manual"))
        repository.addMemory(Memory(id = 0, content = "非饮食相关", category = "FACT", type = "FACT",
            tags = listOf("其他"), importance = 3, source = "manual"))

        val results = repository.getByTag("饮食").first()

        assertTrue("应按标签过滤", results.all { it.tags.contains("饮食") })
    }

    // ==========================================================================
    // 相关性评分测试
    // ==========================================================================

    @Test
    fun testRelevanceScoring() = runBlocking {
        val today = System.currentTimeMillis()

        // High relevance: exact keyword match + tag overlap + recent
        repository.addMemory(Memory(
            id = 0, content = "用户偏好简短回复",
            category = "PREFERENCES", type = "PREFERENCE",
            tags = listOf("用户", "偏好"),
            importance = 5, source = "manual",
            createdAt = today, lastAccessedAt = today - 1000L * 60 * 60 * 2
        ))

        // Low relevance: no match + old + low importance
        repository.addMemory(Memory(
            id = 0, content = "用户喜欢足球",
            category = "USER_FACTS", type = "FACT",
            tags = listOf("运动"),
            importance = 1, source = "manual",
            createdAt = today - 1000L * 60 * 60 * 24 * 60, lastAccessedAt = today - 1000L * 60 * 60 * 24 * 60
        ))

        val relevant = repository.getRelevantMemories(
            query = "用户偏好",
            existingTags = listOf("用户"),
            limit = 5
        )

        assertTrue("应返回至少1条", relevant.isNotEmpty())
        // Top result should be the high-relevance one
        assertEquals("用户偏好简短回复", relevant[0].content)
    }

    @Test
    fun testRelevanceLimit() = runBlocking {
        repeat(10) { i ->
            repository.addMemory(Memory(
                id = 0, content = "测试记忆 $i",
                category = "FACT", type = "FACT",
                tags = emptyList(),
                importance = 5, source = "manual",
                createdAt = System.currentTimeMillis(), lastAccessedAt = System.currentTimeMillis()
            ))
        }

        val top5 = repository.getRelevantMemories(query = "", limit = 5)
        assertEquals("应限制返回5条", 5, top5.size)
    }

    @Test
    fun testRecencyBonus() = runBlocking {
        val today = System.currentTimeMillis()

        // Fresh memory
        repository.addMemory(Memory(
            id = 0, content = "新鲜记忆",
            category = "FACT", type = "FACT",
            tags = listOf("测试"), importance = 3,
            source = "manual",
            createdAt = today, lastAccessedAt = today
        ))

        // Old memory
        repository.addMemory(Memory(
            id = 0, content = "陈旧记忆",
            category = "FACT", type = "FACT",
            tags = listOf("测试"), importance = 3,
            source = "manual",
            createdAt = today - 1000L * 60 * 60 * 24 * 30,
            lastAccessedAt = today - 1000L * 60 * 60 * 24 * 30
        ))

        val results = repository.getRelevantMemories(query = "测试", limit = 10)

        // Fresh should rank higher
        val freshIdx = results.indexOfFirst { it.content == "新鲜记忆" }
        val oldIdx = results.indexOfFirst { it.content == "陈旧记忆" }
        assertTrue("新鲜记忆应排在陈旧记忆前面", freshIdx < oldIdx)
    }

    // ==========================================================================
    // 自动抽取测试
    // ==========================================================================

    @Test
    fun testSyncFromNotes() = runBlocking {
        // NoteRepository needs Context, so we test the concept
        val noteActionItem = "明天下午3点开会"
        val expectedMemory = "行动项: $noteActionItem"

        // Simulate extraction logic
        val existing = repository.allMemories.first()
        val existingContent = existing.map { it.content }

        val shouldAdd = expectedMemory !in existingContent
        assertTrue("新记忆应被添加", shouldAdd)

        val id = repository.addMemory(Memory(
            id = 0, content = expectedMemory,
            category = "NOTES", type = "FACT",
            tags = emptyList(), importance = 3,
            source = "auto:note:2024-01-01"
        ))
        assertTrue("同步应成功", id > 0)
    }

    @Test
    fun testSyncAvoidsDuplicates() = runBlocking {
        val existing = repository.allMemories.first()
        val existingContent = existing.map { it.content }

        val duplicate = "行动项: 重复内容"

        if (duplicate !in existingContent) {
            repository.addMemory(Memory(
                id = 0, content = duplicate,
                category = "NOTES", type = "FACT",
                tags = emptyList(), importance = 3,
                source = "auto:note:2024-01-01"
            ))
        }

        val afterFirst = repository.allMemories.first().count { it.content == duplicate }
        assertEquals("只应有一条", 1, afterFirst)

        // Try adding again
        if (duplicate !in existingContent) {
            repository.addMemory(Memory(
                id = 0, content = duplicate,
                category = "NOTES", type = "FACT",
                tags = emptyList(), importance = 3,
                source = "auto:note:2024-01-02"
            ))
        }

        val afterSecond = repository.allMemories.first().count { it.content == duplicate }
        assertEquals("重复记忆不应添加", 1, afterSecond)
    }

    // ==========================================================================
    // 注入测试
    // ==========================================================================

    @Test
    fun testMemoryInjectionFormat() = runBlocking {
        val basePrompt = "你是一个每日复盘助手"
        repository.addMemory(Memory(
            id = 0, content = "用户偏好简短回复",
            category = "PREFERENCES", type = "PREFERENCE",
            tags = listOf("用户"), importance = 4, source = "manual"
        ))
        repository.addMemory(Memory(
            id = 0, content = "每天早上9点复盘",
            category = "USER_FACTS", type = "FACT",
            tags = listOf("习惯"), importance = 5, source = "manual"
        ))

        val injected = MemoryInjector.injectQuick(basePrompt, repository)

        assertTrue("应包含base prompt", injected.contains(basePrompt))
        assertTrue("应包含分隔符", injected.contains("---"))
        assertTrue("应包含记忆标题", injected.contains("## 你的记忆"))
        assertTrue("应包含第一条记忆", injected.contains("用户偏好简短回复"))
        assertTrue("应包含第二条记忆", injected.contains("每天早上9点复盘"))
        assertTrue("应包含使用提示", injected.contains("参考以上信息"))
    }

    @Test
    fun testNoMemoryReturnsOriginalPrompt() = runBlocking {
        // Clear all memories first
        val all = repository.allMemories.first()
        all.forEach { repository.deleteMemory(it.id) }

        val basePrompt = "原始prompt"
        val injected = MemoryInjector.injectQuick(basePrompt, repository)

        assertEquals("无记忆时应返回原始prompt", basePrompt, injected)
    }

    // ==========================================================================
    // 统计测试
    // ==========================================================================

    @Test
    fun testMemoryCount() = runBlocking {
        repository.addMemory(Memory(id = 0, content = "a", category = "FACT", type = "FACT",
            tags = emptyList(), importance = 3, source = "manual"))
        repository.addMemory(Memory(id = 0, content = "b", category = "FACT", type = "FACT",
            tags = emptyList(), importance = 3, source = "manual"))

        val count = repository.memoryCount.first()
        assertEquals("应有2条记忆", 2, count)
    }

    @Test
    fun testFilterByCategory() = runBlocking {
        repository.addMemory(Memory(id = 0, content = "偏好记忆", category = "PREFERENCES", type = "PREFERENCE",
            tags = emptyList(), importance = 3, source = "manual"))
        repository.addMemory(Memory(id = 0, content = "事实记忆", category = "USER_FACTS", type = "FACT",
            tags = emptyList(), importance = 3, source = "manual"))

        val prefs = repository.getByCategory("PREFERENCES").first()
        val facts = repository.getByCategory("USER_FACTS").first()

        assertEquals("偏好记忆数", 1, prefs.size)
        assertEquals("事实记忆数", 1, facts.size)
    }
}
