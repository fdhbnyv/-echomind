package com.echomind.app.data.api

import okhttp3.Response
import kotlinx.coroutines.delay

/**
 * API 重试与降级工具函数。
 * 为 Whisper / GPT / Notion API 调用提供统一的指数退避重试策略。
 */

/** 可重试的 HTTP 状态码 */
private val RETRYABLE_CODES = setOf(429, 500, 502, 503, 504, 409)

/** 不可重试的错误码 */
private val NON_RETRYABLE_CODES = setOf(400, 401, 403, 404)

/**
 * 执行带指数退避重试的 API 调用。
 *
 * @param maxRetries 最大重试次数
 * @param initialDelayMs 初始延迟（毫秒）
 * @param factor 退避倍数
 * @param block 实际 API 调用，抛出异常或返回非 null 值表示失败
 * @return 成功返回 block 的返回值
 * @throws Exception 所有重试耗尽后抛出最后的异常
 */
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 2,
    initialDelayMs: Long = 2000L,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    repeat(maxRetries + 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e

            // 最后一次尝试后不再等待
            if (attempt >= maxRetries) {
                throw e
            }

            // 从异常中提取 Retry-After header
            val retryAfterMs = extractRetryAfterMs(e)
            val waitMs = retryAfterMs ?: currentDelay

            delay(waitMs)
            currentDelay = (currentDelay * factor).toLong()
        }
    }

    throw lastException ?: Exception("Unexpected retry failure")
}

/**
 * 判断 HTTP 响应是否可重试。
 */
fun isRetryable(response: Response): Boolean {
    return response.code in RETRYABLE_CODES
}

/**
 * 判断 HTTP 响应是否为不可恢复错误。
 */
fun isNonRetryable(response: Response): Boolean {
    return response.code in NON_RETRYABLE_CODES
}

/**
 * 从异常中提取 Retry-After 头信息。
 */
private fun extractRetryAfterMs(exception: Exception): Long? {
    // 尝试匹配 OkHttp 响应中的 Retry-After header
    val message = exception.message ?: return null

    // 简单匹配 "Retry-After: 数字" 模式
    val regex = Regex("""Retry-After[:\s]+(\d+)""", RegexOption.IGNORE_CASE)
    val match = regex.find(message)
    return match?.groupValues?.get(1)?.toLongOrNull()?.times(1000)
}

/**
 * 各 API 推荐的重试配置（参考 API 契约文档 6.2 节）
 */
object RetryConfig {
    /** Whisper API: 2 次重试，初始 2s */
    val whisper = RetryPolicy(maxRetries = 2, initialDelayMs = 2000L)

    /** GPT API: 2 次重试，初始 1s */
    val gpt = RetryPolicy(maxRetries = 2, initialDelayMs = 1000L)

    /** Notion API: 3 次重试，初始 3s */
    val notion = RetryPolicy(maxRetries = 3, initialDelayMs = 3000L)
}

data class RetryPolicy(
    val maxRetries: Int = 2,
    val initialDelayMs: Long = 1000L,
)
