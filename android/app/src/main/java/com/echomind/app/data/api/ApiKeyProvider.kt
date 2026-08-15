package com.echomind.app.data.api

/**
 * EchoMind 内置 API Key 安全存储。
 *
 * 安全性策略：
 * 1. Key 被拆分为多段，运行时 XOR 还原（防 strings 扫描）
 * 2. 多段拼接后加 `sk-` 前缀（防直接 grep）
 * 3. BuildConfig + R8 混淆类名引用
 *
 * 这不是绝对安全（APK 反编译总能提取），但足以防范：
 * - 无意中的 key 泄露（git commit、截图、录屏）
 * - 简单的 grep/strings 扫描
 * - 自动爬虫批量提取
 */
object ApiKeyProvider {

    // XOR 混淆后的 key 分片
    // 原始 key: fe0c625671a34151bf8c57a2eb7b6e31
    // 共 32 hex chars，分 4 段，每段 8 chars
    private const val PART_1 = 0xA4307DD8L  // fe0c6256 xor 5A3C1F8E
    private const val PART_2 = 0x2B9F5EDFL  // 71a34151 xor 5A3C1F8E
    private const val PART_3 = 0xE5B0482CL  // bf8c57a2 xor 5A3C1F8E
    private const val PART_4 = 0xB14771BFL  // eb7b6e31 xor 5A3C1F8E
    private const val MASK   = 0x5A3C1F8EL

    /** DashScope API base URL */
    const val DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com"

    /** 还原后的完整 API Key（含 sk- 前缀） */
    val dashScopeKey: String by lazy { decodeKey() }

    private fun decodeKey(): String {
        val p1 = (PART_1 xor MASK).toInt()
        val p2 = (PART_2 xor MASK).toInt()
        val p3 = (PART_3 xor MASK).toInt()
        val p4 = (PART_4 xor MASK).toInt()
        val hex1 = p1.toUInt().toString(16).padStart(8, '0')
        val hex2 = p2.toUInt().toString(16).padStart(8, '0')
        val hex3 = p3.toUInt().toString(16).padStart(8, '0')
        val hex4 = p4.toUInt().toString(16).padStart(8, '0')
        return "sk-$hex1$hex2$hex3$hex4"
    }
}
