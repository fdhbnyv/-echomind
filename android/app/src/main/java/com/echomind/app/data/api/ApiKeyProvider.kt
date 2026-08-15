package com.echomind.app.data.api

/**
 * EchoMind API Key 提供器。
 * 建议在设置界面手动配置，或通过系统环境变量/BuildConfig 注入。
 */
object ApiKeyProvider {

    /** DashScope API base URL */
    const val DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com"

    /** API Key（默认空，可通过环境变量或设置提供） */
    var dashScopeKey: String = System.getenv("DASHSCOPE_API_KEY") ?: ""
}
