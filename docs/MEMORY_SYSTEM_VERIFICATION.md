# EchoMind 记忆系统验证方案

## 一、验证目标

确认记忆系统是否真正"有用"，而非仅仅"存在"。

### 核心问题
1. **准确性**：记忆是否被正确存储、检索、注入？
2. **相关性**：注入的记忆是否与当前笔记主题相关？
3. **时效性**：旧记忆是否会被合理降级？
4. **用户感知**：用户是否感觉到 AI 更"懂"我了？

---

## 二、验证方法

### 方法1：单元测试（覆盖率 > 80%）

```bash
cd android && ./gradlew :app:testDebugUnitTest
```

**测试文件：**
- `MemorySystemTest.kt` — 纯逻辑测试（评分算法、数据转换、注入格式）
- `MemoryRepositoryTest.kt` — 集成测试（DB 读写、搜索、去重）

**关键测试点：**
| 测试场景 | 预期结果 |
|---------|---------|
| 记忆 CRUD | 添加后能检索到，更新后内容改变，删除后不出现 |
| 关键词搜索 | "用户偏好" 能匹配到 "用户喜欢简短回复" |
| 标签过滤 | 按 #习惯 标签能找到所有习惯相关记忆 |
| 相关性评分 | 含关键词+近期访问的记忆排第一 |
| 重复记忆检测 | 相同内容只添加一次 |
| 注入格式 | 包含 "---"、"## 你的记忆"、编号列表 |

---

### 方法2：A/B 对照实验

**实验设计：**

| 组别 | 配置 | 验证指标 |
|------|------|---------|
| 实验组 | 开启记忆注入 | 笔记结构化准确率、用户满意度 |
| 对照组 | 关闭记忆注入 | 同上 |

**操作步骤：**
1. 收集 10 条真实语音转写文本
2. 用同一组提示词，分别带/不带记忆注入
3. 对比输出质量

**评估维度：**
```
1. 个性化程度（1-5分）
   - 是否使用了用户偏好（如"用户喜欢简短回复"→输出更简洁）
   
2. 事实一致性（是/否）
   - 提到的时间、人物是否与历史记忆一致
   
3. 行动项完整性（项数）
   - 带记忆 vs 不带记忆的 actionItems 数量对比
```

---

### 方法3：日志追踪（生产环境）

在 `MemoryInjector` 中添加日志：

```kotlin
// MemoryInjector.kt
suspend fun injectQuick(basePrompt: String, repository: MemoryRepository): String {
    val allMemories = repository.allMemories.first()
    if (allMemories.isEmpty()) {
        Log.d("MemoryInjector", "No memories, using base prompt")
        return basePrompt
    }
    
    val recent = allMemories.take(5)
    val memoryBlock = buildString { ... }
    
    Log.d("MemoryInjector", "Injected ${recent.size} memories:")
    recent.forEach { m ->
        Log.d("MemoryInjector", "  [${m.importance}] ${m.content}")
    }
    
    return "$basePrompt$memoryBlock"
}
```

**观察指标：**
- 每次注入的记忆条数（应在 3-5 条）
- 记忆重要性分布（高重要性应优先出现）
- 空注入比例（初期应为 100%，随着记忆积累逐渐下降）

---

### 方法4：用户行为验证

**关键行为指标：**
| 指标 | 说明 |
|------|------|
| 记忆页面访问量 | 用户是否主动查看/编辑记忆 |
| 手动添加记忆数 | 用户认为需要补充的记忆 |
| 记忆同步按钮点击 | 用户是否使用自动抽取功能 |
| 重复记忆删除率 | 系统抽取质量如何 |

**用户调研问题：**
1. "AI 是否在回复中体现了你知道的我的偏好？"（是/否/有时）
2. "记忆是否让笔记更贴合你的习惯？"（1-5分）
3. "你修改或添加了哪些记忆？"

---

## 三、验收标准

### 通过标准（必须满足）

- [ ] 单元测试全部通过（`./gradlew :app:testDebugUnitTest`）
- [ ] 无记忆时 prompt 不变
- [ ] 有记忆时 prompt 包含记忆块
- [ ] 记忆被正确注入到 LLM 请求中

### 优秀标准（加分项）

- [ ] 相关性评分前 3 的记忆都是高相关
- [ ] 重复记忆检测准确（0 误添加）
- [ ] 用户手动编辑记忆的 UI 流畅

---

## 四、快速验证命令

```bash
# 1. 运行单元测试
cd D:\EchoMind\android
./gradlew :app:testDebugUnitTest

# 2. 构建 APK
./gradlew :app:assembleDebug

# 3. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 4. 查看日志
adb logcat | grep -E "MemoryInjector|MemoryRepository"
```

---

## 五、后续优化方向

1. **评分算法调优** — 当前是静态权重，可根据用户反馈动态调整
2. **记忆遗忘机制** — 超过 90 天未访问的记忆自动降权
3. **记忆冲突检测** — 新旧记忆矛盾时提示用户确认
4. **记忆导出/导入** — 支持备份和迁移
