# 声念 EchoMind

> **将碎片思维，一语成章。**

对着手机说出一天的杂乱想法，AI 自动整理成结构化的 Notion 笔记。

---

## 项目结构

```
echomind/
├── PRD-EchoMind.md         # 产品需求文档 (v1.1)
├── README.md               # 本文件
└── templates/              # 模板定义（核心产品资产）
    ├── daily-review.yaml   # 每日复盘模板
    ├── quick-idea.yaml     # 碎片想法模板
    └── meeting-notes.yaml  # 会议纪要模板
```

---

## 架构（V1 决策：零后端）

V1 不做后端服务。Android 客户端直接调用：

```
用户录音 → MediaRecorder (.aac)
              ↓
       OpenAI Whisper API（转写）
              ↓
       OpenAI GPT-4o-mini（结构化）
              ↓
       预览编辑 → Notion API（写入）
```

**为什么不做后端？**

1. **验证速度优先** — 2-3 周交付 MVP，先验证结构化质量这个最大风险
2. **零运维** — 一个 APK 就是一个产品
3. **数据隐私** — 数据不入自己服务器
4. **灵活切换** — 将来加后端时无需改动客户端核心逻辑

---

## 当前状态

| 模块 | 状态 |
|:--|:--|
| PRD 文档 | ✅ v1.1（已补齐全部缺失章节） |
| 模板定义 | ✅ 3 个核心模板（`templates/*.yaml`） |
| 结构化 prompt | ✅ 已定义，需实测迭代 |
| Android 客户端 | ❌ 未开始（优先级：V1 核心闭环） |
| Widget / 离线队列 | ❌ V1.1 规划 |
| 用户系统 / 付费 | ❌ V2 规划 |

---

## 下一步行动

1. 🟢 **立即** — 用 `templates/daily-review.yaml` 的 prompt + 30 条真实语音跑测试
2. 🟢 **本周** — 启动 Android 客户端开发（Jetpack Compose + Kotlin）
3. 🟡 **V1 完成前** — 招募 5 名种子用户进行封闭测试

---

## 核心文档

- [PRD-EchoMind.md](PRD-EchoMind.md) — 完整产品需求文档
- [templates/daily-review.yaml](templates/daily-review.yaml) — 每日复盘模板定义
- [templates/quick-idea.yaml](templates/quick-idea.yaml) — 碎片想法模板定义
- [templates/meeting-notes.yaml](templates/meeting-notes.yaml) — 会议纪要模板定义
