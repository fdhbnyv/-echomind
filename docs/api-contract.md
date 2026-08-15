# API 接口契约

> EchoMind V1 — Android 客户端直调第三方 API

**架构决策**：V1 不做后端代理。客户端直接调用 OpenAI（Whisper + GPT）和 Notion 的 REST API。

---

## 目录

1. [总览：调用流程](#1-总览调用流程)
2. [OpenAI Whisper API：语音转文字](#2-openai-whisper-api-语音转文字)
3. [OpenAI GPT API：结构化提取](#3-openai-gpt-api-结构化提取)
4. [Notion API：写入笔记](#4-notion-api-写入笔记)
5. [错误码速查表](#5-错误码速查表)
6. [重试与降级策略](#6-重试与降级策略)
7. [API Key 管理](#7-api-key-管理)

---

## 1. 总览：调用流程

```
┌─────────────────────────────────────────────────────────────┐
│                    录音完成                                    │
│  MediaRecorder 输出 .aac / .mp4 文件                          │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Step 1: Whisper 转写                                        │
│  POST /v1/audio/transcriptions                               │
│  输入: 音频文件 → 输出: { text: "今天做了三件事..." }           │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Step 2: GPT 结构化                                          │
│  POST /v1/chat/completions                                   │
│  输入: system prompt(来自模板) + user message(转写文本)        │
│  输出: { date, summary, accomplishments, ... }               │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Step 3: Notion 写入                                         │
│  POST /v1/pages                                              │
│  输入: 结构化 JSON 按字段映射 → 输出: page id                  │
└─────────────────────────────────────────────────────────────┘
```

**端到端超时目标**：< 语音时长的 50%（例如 2 分钟录音 → 总耗时 < 1 分钟）

---

## 2. OpenAI Whisper API：语音转文字

### 2.1 基本信息

| 字段 | 值 |
|------|-----|
| **Endpoint** | `POST https://api.openai.com/v1/audio/transcriptions` |
| **Auth** | `Authorization: Bearer {api_key}` |
| **Content-Type** | `multipart/form-data` |
| **超时** | 30 秒（客户端配置） |
| **模型** | `whisper-1` |

### 2.2 请求参数

| 参数 | 类型 | 必填 | 说明 | V1 固定值 |
|------|------|------|------|----------|
| `file` | File | ✅ | 音频文件，支持 flac/m4a/mp3/mp4/mpeg/mpga/oga/ogg/wav/webm | AAC(.m4a), 16kHz, mono |
| `model` | String | ✅ | 转写模型 | `whisper-1` |
| `language` | String | ❌ | 语言代码，建议指定以提高准确率 | `zh`（中文）/ `en`（英文） |
| `response_format` | String | ❌ | 返回格式 | `json`（默认） |
| `temperature` | Float | ❌ | 采样温度 0-1 | `0`（保持稳定） |

### 2.3 请求示例 (Kotlin + OkHttp)

```kotlin
// --- 请求构建 ---
val audioFile = File(cacheDir, "recording_${timestamp}.m4a")

val requestBody = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", audioFile.name, 
        audioFile.asRequestBody("audio/m4a".toMediaType()))
    .addFormDataPart("model", "whisper-1")
    .addFormDataPart("language", "zh")
    .addFormDataPart("response_format", "json")
    .addFormDataPart("temperature", "0")
    .build()

val request = Request.Builder()
    .url("https://api.openai.com/v1/audio/transcriptions")
    .header("Authorization", "Bearer $OPENAI_API_KEY")
    .post(requestBody)
    .build()

// 超时配置
val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)  // 大音频文件需要更长时间
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
```

### 2.4 成功响应

```json
{
    "text": "今天终于把那个用户访谈报告写完了，拖了一周了都。下午开了个团队会，讨论了下一期的排期，感觉大家意见不太统一，有点烦。"
}
```

### 2.5 常见错误

| HTTP 状态 | 错误类型 | 原因 | 客户端处理 |
|-----------|---------|------|-----------|
| `400` | `invalid_file_format` | 文件格式不支持 | 检查录音格式，提示用户重新录音 |
| `400` | `file_too_large` | 文件超过 25MB | 压缩音频或分段（15 分钟录音通常 < 10MB） |
| `401` | `invalid_api_key` | API Key 无效 | 提示用户检查配置 |
| `429` | `rate_limit_exceeded` | 超出速率限制 | 等待 5 秒后重试（指数退避） |
| `500` | `server_error` | OpenAI 服务端错误 | 重试 2 次，间隔递增 |

### 2.6 成本估算

| 模型 | 计费 | 1 分钟中文音频（约 ~150 tokens） |
|------|------|-------------------------------|
| `whisper-1` | $0.006 / 分钟 | ~$0.006（约 ¥0.04） |

---

## 3. OpenAI GPT API：结构化提取

### 3.1 基本信息

| 字段 | 值 |
|------|-----|
| **Endpoint** | `POST https://api.openai.com/v1/chat/completions` |
| **Auth** | `Authorization: Bearer {api_key}` |
| **Content-Type** | `application/json` |
| **超时** | 20 秒 |
| **模型** | `gpt-4o-mini`（V1，成本与效果平衡） |

### 3.2 请求参数

| 参数 | 类型 | 必填 | 说明 | V1 固定值 |
|------|------|------|------|----------|
| `model` | String | ✅ | 模型 ID | `gpt-4o-mini` |
| `messages` | Array | ✅ | 消息列表（system + user） | 见下方示例 |
| `temperature` | Float | ❌ | 越低越稳定 | `0.0`（结构化任务不需要创造力） |
| `response_format` | Object | ❌ | JSON 模式 | `{ "type": "json_object" }` |
| `max_tokens` | Integer | ❌ | 最大输出长度 | 1000（根据模板字段数量调整） |

### 3.3 请求示例

```kotlin
// --- 构建 messages ---
// system prompt 来自 templates/daily-review.yaml 的 llm_prompt 字段
val systemPrompt = """
| # 角色
| 你是一个专业的个人效能助手...
| # 输出要求
| 严格按照以下 JSON Schema 输出纯 JSON...
""".trimMargin()

val transcript = "今天终于把那个用户访谈报告写完了..."

val messages = listOf(
    Message("system", systemPrompt),
    Message("user", transcript)
)

// --- 请求体 ---
val requestBody = mapOf(
    "model" to "gpt-4o-mini",
    "messages" to messages,
    "temperature" to 0.0,
    "response_format" to mapOf("type" to "json_object"),
    "max_tokens" to 1000
)

val requestBodyJson = Json.encodeToString(requestBody)

val request = Request.Builder()
    .url("https://api.openai.com/v1/chat/completions")
    .header("Authorization", "Bearer $OPENAI_API_KEY")
    .header("Content-Type", "application/json")
    .post(requestBody.toRequestBody("application/json".toMediaType()))
    .build()
```

### 3.4 成功响应

```json
{
    "id": "chatcmpl-xxx",
    "object": "chat.completion",
    "created": 1720000000,
    "model": "gpt-4o-mini-2024-07-18",
    "choices": [
        {
            "index": 0,
            "message": {
                "role": "assistant",
                "content": "{\n  \"date\": \"2026-07-03\",\n  \"summary\": \"完成了拖延一周的访谈报告\",\n  \"accomplishments\": [\"完成用户访谈报告\", \"参加团队排期讨论会\"],\n  \"mood\": \"tired\",\n  \"action_items\": [\n    {\"task\": \"把报告发给老王\", \"deadline\": \"2026-07-04T12:00:00\"}\n  ],\n  \"tags\": [\"工作\", \"生活\"]\n}"
            },
            "finish_reason": "stop"
        }
    ],
    "usage": {
        "prompt_tokens": 850,
        "completion_tokens": 180,
        "total_tokens": 1030
    }
}
```

### 3.5 JSON 解析器

```kotlin
// 从 GPT 响应中提取 content JSON 字符串
val contentJson = response.choices[0].message.content

// 反序列化为对应模板的 data class
data class DailyReview(
    val date: String,
    val summary: String,
    val accomplishments: List<String>,
    val unfinished: List<String>,
    val mood: String,
    val reflections: List<String>,
    val tomorrowPlan: List<String>,
    val actionItems: List<ActionItem>,
    val tags: List<String>
)

data class ActionItem(
    val task: String,
    val deadline: String?  // nullable
)

// 使用 kotlinx.serialization 或 Moshi 解析
val review = Json.decodeFromString<DailyReview>(contentJson)
```

### 3.6 降级方案

当 GPT 结构化失败时：

```
Step 2 失败
  ├── GPT API 500 / 超时
  │     └── 重试 1 次
  │           └── 仍失败 → 降级：展示纯转写文本
  │
  ├── GPT 返回了非 JSON 内容
  │     └── 尝试正则提取 JSON 块
  │           └── 失败 → 降级：展示纯转写文本
  │
  └── GPT 返回了 JSON 但缺少必填字段
        └── 用默认值填充缺失字段
              └── 在 UI 上标记"部分字段未识别"
```

### 3.7 成本估算

| 模型 | 输入价格 | 输出价格 | 典型调用 |
|------|---------|---------|---------|
| `gpt-4o-mini` | $0.15 / 百万 token | $0.60 / 百万 token | ~$0.0002/次（~¥0.0015） |

**每条语音结构化的成本极低，主要成本在 Whisper 转写（约 ¥0.04/分钟录音）。**

---

## 4. Notion API：写入笔记

### 4.1 基本信息

| 字段 | 值 |
|------|-----|
| **Endpoint** | `POST https://api.notion.com/v1/pages`（创建页面） |
| | `GET https://api.notion.com/v1/databases/{id}`（查询数据库结构） |
| | `POST https://api.notion.com/v1/databases/{id}/query`（查询已有页面） |
| **Auth** | `Authorization: Bearer {notion_integration_token}`（OAuth 或内部集成） |
| **Content-Type** | `application/json` |
| **Notion-Version** | 固定请求头 `2022-06-28` |
| **超时** | 15 秒 |

### 4.2 OAuth 授权流程

```
用户点击"连接 Notion"
  ↓
App 打开浏览器 → https://api.notion.com/v1/oauth/authorize
  ?client_id={client_id}
  &redirect_uri={app_scheme}://oauth/callback
  &response_type=code
  &owner=user
  ↓
用户授权后 Notion 回调 redirect_uri
  ↓
App 用 authorization_code 换取 access_token:
  POST https://api.notion.com/v1/oauth/token
  Headers: Basic base64(client_id:client_secret)
  Body: { grant_type: "authorization_code", code: "...", redirect_uri: "..." }
  ↓
返回: { access_token, workspace_id, workspace_name, bot_id }
  ↓
存储 token → EncryptedSharedPreferences
```

> **V1 注意**：OAuth 的 `redirect_uri` 需要使用 Android App Link 或自定义 scheme（如 `echomind://oauth/callback`）。Notion 不支持 `https://` 以外的回调，所以 V1 需要一个小型代理或改用 Notion 内部集成 token（手动配置）。

**V1 简化方案**：由于 OAuth 的 redirect_uri 限制（需要 HTTPS 端点），V1 可以先使用 **Notion 内部集成**（用户自己在 Notion 创建 Integration，复制 token 粘贴到 App），V1.1 再支持完整的 OAuth 流程。

### 4.3 查询数据库结构

Get database metadata (including property schema):

```
GET https://api.notion.com/v1/databases/{database_id}
Headers:
  Authorization: Bearer {token}
  Notion-Version: 2022-06-28
```

Response includes `properties` map — 用于自动匹配模板字段到数据库属性：

```json
{
    "properties": {
        "总结": { "id": "title", "type": "title" },
        "日期": { "id": "%3Fz%2BK", "type": "date" },
        "心情": { "id": "ps%40%3E", "type": "select", "select": { "options": [...] } },
        "完成事项": { "id": "l%5D%40%3E", "type": "multi_select" },
        "标签": { "id": "qwe%3E", "type": "multi_select" }
    }
}
```

### 4.4 创建页面请求

```kotlin
// --- 根据模板映射构建 Notion 属性 ---
// 模板中的 notion_mapping:
//   date: { property: "日期", type: "date" }
//   summary: { property: "总结", type: "title" }
//   mood: { property: "心情", type: "select" }
//   tags: { property: "标签", type: "multi_select" }

val properties = mapOf(
    "总结" to mapOf(
        "title" to listOf(
            mapOf("text" to mapOf("content" to structuredResult.summary))
        )
    ),
    "日期" to mapOf(
        "date" to mapOf("start" to structuredResult.date)
    ),
    "心情" to mapOf(
        "select" to mapOf("name" to structuredResult.mood)  // 需匹配 Notion 选项值
    ),
    "标签" to mapOf(
        "multi_select" to structuredResult.tags.map { tag ->
            mapOf("name" to tag)
        }
    )
)

val requestBody = mapOf(
    "parent" to mapOf("database_id" to DATABASE_ID),
    "properties" to properties
)

// --- 请求 ---
val request = Request.Builder()
    .url("https://api.notion.com/v1/pages")
    .header("Authorization", "Bearer $NOTION_TOKEN")
    .header("Notion-Version", "2022-06-28")
    .header("Content-Type", "application/json")
    .post(requestBody.toRequestBody("application/json".toMediaType()))
    .build()
```

### 4.5 成功响应

```json
{
    "object": "page",
    "id": "abc123def456",
    "created_time": "2026-07-03T12:00:00.000Z",
    "url": "https://www.notion.so/abc123def456",
    "properties": {
        "总结": { "id": "title", "type": "title", "title": [...] },
        "日期": { "id": "%3Fz%2BK", "type": "date", "date": { "start": "2026-07-03" } }
    }
}
```

### 4.6 字段映射规则

| Notion 属性类型 | SDK 映射 | 模板字段类型 | 注意事项 |
|----------------|---------|-------------|---------|
| `title` | `{ title: [{ text: { content } }] }` | String | 每个 database 必须有且仅有一个 title 字段 |
| `rich_text` | `{ rich_text: [{ text: { content } }] }` | String | 支持多行文本 |
| `date` | `{ date: { start: "YYYY-MM-DD" } }` | String (ISO 8601) | 可选加 end 和 time_zone |
| `select` | `{ select: { name: "选项名" } }` | String (enum) | name **必须匹配** Notion 数据库中已存在的选项名 |
| `multi_select` | `{ multi_select: [{ name: "标签1" }, { name: "标签2" }] }` | Array<String> | 不存在时会自动创建 |
| `number` | `{ number: 42 }` | Number | — |

### 4.7 常见错误

| HTTP 状态 | 错误码 | 原因 | 处理 |
|-----------|--------|------|------|
| `400` | `validation_error` | 属性字段名或类型不匹配 | 提示用户检查数据库属性设置，提供截图指南 |
| `401` | `unauthorized` | Token 无效或已撤销 | 引导用户重新连接 Notion |
| `403` | `restricted_resource` | Token 没有写入该页面的权限 | 提示用户检查 Integration 的页面分享权限 |
| `404` | `object_not_found` | 数据库 ID 不存在或已删除 | 提示用户重新选择目标数据库 |
| `409` | `conflict_error` | 并发写入冲突 | 重试 1 次 |
| `429` | `rate_limited` | 超出 Notion API 速率限制 | 指数退避重试（见第 6 节） |

### 4.8 速率限制

Notion API 速率限制：
- **每个 workspace**：平均每秒 3 个请求
- **突发**：每秒最多 30 个请求
- **超出后**：返回 `429`，响应头包含 `Retry-After`

**客户端策略**：每次请求前检查是否超过限制，串行化写入操作，用户并发写入最多 1 条。

---

## 5. 错误码速查表

| 场景 | 错误表现 | 用户提示 | 自动恢复 |
|------|---------|---------|---------|
| 麦克风权限拒绝 | 录音按钮置灰 | "请在设置中开启麦克风权限" | 点击引导 → 系统设置页 |
| 录音文件损坏 | Whisper 返回 400 | "录音文件异常，请重新录制" | 重试 |
| API Key 无效 | Whisper/GPT 401 | "连接 OpenAI 失败，请检查配置" | 无（需用户操作） |
| Whisper 超时 | 30 秒无响应 | "录音转写超时，请重试" | 重试 1 次 |
| GPT 返回非 JSON | 解析失败 | 降级显示转写文本 | 无 |
| GPT 结构化质量低 | 空字段过多 | "部分内容未能识别，建议补充" | 用默认值填充 |
| Notion 数据库不存在 | 404 | "未找到目标数据库，请重新选择" | 无（需用户操作） |
| Notion 属性不匹配 | 400 + validation_error | "数据库字段与模板不匹配，请检查" | 提供自动匹配 UI |
| 网络断开 | OkHttp IOException | "网络连接失败，稍后自动重试" | WorkManager 离线队列 |
| 写入冲突 | 409 | "正在同步中，请稍候" | 重试 1 次 |

---

## 6. 重试与降级策略

### 6.1 指数退避重试

```kotlin
// Kotlin 实现模式
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelay: Long = 1000L,  // 1 秒
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            // 429 (rate limit) 优先使用 Retry-After header
            val retryAfter = (e as? HttpException)
                ?.response()
                ?.header("Retry-After")
                ?.toLongOrNull()
            delay(retryAfter?.times(1000) ?: currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    return block() // 最后一次尝试
}
```

### 6.2 各 API 重试配置

| API | 重试次数 | 初始延迟 | 可重试错误 | 不可重试错误 |
|-----|---------|---------|-----------|------------|
| Whisper | 2 次 | 2s | 429, 500, 503, timeout | 400（文件问题）, 401（Key 问题） |
| GPT | 2 次 | 1s | 429, 500, 502, timeout | 400（prompt 问题）, 401 |
| Notion | 3 次 | 3s | 429, 500, 409 | 401, 403, 404（需用户操作） |

### 6.3 降级优先级

```
正常路径: 录音 → 转写 → 结构化 → Notion 写入
                              ↓
                          ┌────┴────┐
                          ↓         ↓
                     结构化失败   结构化成功
                          ↓         ↓
                   降级为纯转写    Notion 失败
                    文本展示          ↓
                                  ┌──┴──┐
                                  ↓     ↓
                              重试成功 重试仍失败
                                        ↓
                                  本地排队
                                (V1.1 WorkManager)
                                  下次启动重试
```

---

## 7. API Key 管理

### 7.1 V1 方案：嵌入 App + 混淆

```kotlin
// 1. 在 BuildConfig 或 NDK 中存储
// build.gradle.kts:
//   buildConfigField("String", "OPENAI_API_KEY", "\"sk-xxx...\"")

// 2. 使用混淆（ProGuard / R8），不要硬编码字符串
// 混淆规则:
//   -keepclassmembers class com.echomind.BuildConfig {
//       public static <fields>;
//   }

// 3. 可选：远端配置下发（V1.1）
// 用 Firebase Remote Config 或自建 API 定期下发 Key
```

### 7.2 安全风险说明

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| APK 反编译提取 Key | 高（完全静态分析） | 混淆 + NDK（.so 文件）存储，增加逆向成本 |
| 网络抓包泄露 Key | 中 | HTTPS + TLS 1.3 |
| Key 被盗用产生费用 | 中 | 在 OpenAI 后台设置 **Usage Limits**（建议月上限 $20） |
| 用户超出免费额度后不付费 | 低 | V1 不限制，通过有限条数控制成本 |

### 7.3 OpenAI API Key 安全建议

1. 在 OpenAI Dashboard 创建**受限 API Key**（仅允许 whisper + gpt-4o-mini，不允许其他模型）
2. 设置**月度使用上限**（建议 $20-$50）
3. 定期轮换 Key（V1.1 实现远端配置下发后可自动轮换）

### 7.4 Notion Token 安全

- 使用 **EncryptedSharedPreferences** 存储 OAuth token
- Token 作用域限制为**只读写指定的数据库**
- Token 过期/撤销后引导用户重新授权
- App 卸载时自动清除所有 token

---

> **文档维护**：此 API 契约随模板文件（`templates/*.yaml`）一起维护。如果模板新增字段，需要同步更新 Notion 映射部分。V1.1 引入后端代理后，客户端只需调用自己的后端，此文档需要重写。
