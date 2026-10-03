package com.echomind.app.audio

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.WaveReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Sherpa-ONNX 本地离线语音识别引擎。
 *
 * 特性：
 * - 采用 Zipformer-zh-14M 流式中文端侧模型，离线 0 流量极速推理
 * - 支持流式实时转写（边录音边输出文字）
 * - 支持整段音频（WAV / PCM）离线快速转写
 * - 内存安全单例管理，按需或预热加载
 */
object SherpaAsrEngine {

    private const val TAG = "SherpaAsrEngine"
    private const val MODEL_DIR = "sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23"

    private val lock = Any()
    private var recognizer: OnlineRecognizer? = null
    private var isInitialized = false
    private var lastInitError: String? = null

    /**
     * 检查本地模型资产是否完整
     */
    fun isModelPresent(context: Context): Boolean {
        return try {
            val list = context.assets.list(MODEL_DIR) ?: return false
            val required = setOf(
                "tokens.txt",
                "encoder-epoch-99-avg-1.int8.onnx",
                "joiner-epoch-99-avg-1.int8.onnx",
            )
            val found = list.toSet()
            required.all { it in found } && (
                "decoder-epoch-99-avg-1.onnx" in found || "decoder-epoch-99-avg-1.int8.onnx" in found
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check model assets: ${e.message}")
            false
        }
    }

    /**
     * 初始化 Sherpa-ONNX 识别器（线程安全，建议在 Dispatchers.Default 中调用）
     */
    suspend fun initialize(context: Context): Result<Unit> = withContext(Dispatchers.Default) {
        synchronized(lock) {
            if (isInitialized && recognizer != null) {
                return@withContext Result.success(Unit)
            }

            try {
                if (!isModelPresent(context)) {
                    val err = "本地语音模型文件缺失或不完整 ($MODEL_DIR)"
                    lastInitError = err
                    return@withContext Result.failure(IllegalStateException(err))
                }

                val assets = context.assets
                val files = assets.list(MODEL_DIR)?.toSet() ?: emptySet()
                val decoderName = if ("decoder-epoch-99-avg-1.onnx" in files) {
                    "decoder-epoch-99-avg-1.onnx"
                } else {
                    "decoder-epoch-99-avg-1.int8.onnx"
                }

                val featConfig = FeatureConfig().apply {
                    sampleRate = 16000
                    featureDim = 80
                }

                val transducerConfig = OnlineTransducerModelConfig().apply {
                    encoder = "$MODEL_DIR/encoder-epoch-99-avg-1.int8.onnx"
                    decoder = "$MODEL_DIR/$decoderName"
                    joiner = "$MODEL_DIR/joiner-epoch-99-avg-1.int8.onnx"
                }

                val modelConfig = OnlineModelConfig().apply {
                    transducer = transducerConfig
                    tokens = "$MODEL_DIR/tokens.txt"
                    numThreads = 2
                    debug = false
                    modelType = "zipformer"
                }

                val endpointConfig = EndpointConfig().apply {
                    rule1 = EndpointRule(false, 2.4f, 0.0f)
                    rule2 = EndpointRule(true, 1.4f, 0.0f)
                    rule3 = EndpointRule(false, 0.0f, 20.0f)
                }

                val config = OnlineRecognizerConfig().apply {
                    this.featConfig = featConfig
                    this.modelConfig = modelConfig
                    this.endpointConfig = endpointConfig
                    enableEndpoint = true
                    decodingMethod = "greedy_search"
                    maxActivePaths = 4
                }

                Log.d(TAG, "Creating OnlineRecognizer with asset model...")
                val created = OnlineRecognizer(assets, config)
                recognizer = created
                isInitialized = true
                lastInitError = null
                Log.i(TAG, "SherpaAsrEngine initialized successfully.")
                Result.success(Unit)
            } catch (t: Throwable) {
                Log.e(TAG, "SherpaAsrEngine init failed", t)
                lastInitError = t.message
                Result.failure(t)
            }
        }
    }

    /** 是否已就绪 */
    fun isReady(): Boolean = isInitialized && recognizer != null

    /** 获取上次初始化失败的原因 */
    fun getLastError(): String? = lastInitError

    /**
     * 创建流式识别会话
     */
    fun createStream(): OnlineStream? {
        val rec = recognizer ?: return null
        return try {
            rec.createStream()
        } catch (e: Exception) {
            Log.e(TAG, "createStream failed", e)
            null
        }
    }

    /**
     * 向流式识别会话输入 PCM 浮点音频帧并执行增量解码
     *
     * @param stream 当前识别流
     * @param samples 归一化至 [-1.0, 1.0] 的音频采样点 (16kHz)
     * @param sampleRate 采样率，默认 16000
     * @return 当前累积识别出的文本
     */
    fun processSamples(
        stream: OnlineStream,
        samples: FloatArray,
        sampleRate: Int = 16000,
    ): String {
        val rec = recognizer ?: return ""
        try {
            stream.acceptWaveform(samples, sampleRate)
            while (rec.isReady(stream)) {
                rec.decode(stream)
            }
            return rec.getResult(stream).text.trim()
        } catch (e: Exception) {
            Log.w(TAG, "processSamples error: ${e.message}")
            return ""
        }
    }

    /**
     * 结束当前流输入并返回最终识别文本，自动释放 stream 原生内存
     */
    fun finishStream(stream: OnlineStream): String {
        val rec = recognizer ?: return ""
        return try {
            stream.inputFinished()
            while (rec.isReady(stream)) {
                rec.decode(stream)
            }
            val finalText = rec.getResult(stream).text.trim()
            stream.release()
            finalText
        } catch (e: Exception) {
            Log.e(TAG, "finishStream error", e)
            try { stream.release() } catch (_: Exception) {}
            ""
        }
    }

    /**
     * 转写单个 WAV 音频文件（适合已录制完成的文件）
     */
    suspend fun transcribeWaveFile(
        context: Context,
        wavFile: File,
    ): Result<String> = withContext(Dispatchers.Default) {
        if (!wavFile.exists() || wavFile.length() < 44) {
            return@withContext Result.failure(IllegalArgumentException("音频文件不存在或为空"))
        }

        if (!isReady()) {
            val initRes = initialize(context)
            if (initRes.isFailure) {
                return@withContext Result.failure(
                    initRes.exceptionOrNull() ?: Exception("语音引擎未初始化")
                )
            }
        }

        val rec = recognizer ?: return@withContext Result.failure(Exception("语音引擎不可用"))

        try {
            val waveData = WaveReader.readWave(wavFile.absolutePath)
            val stream = rec.createStream()

            stream.acceptWaveform(waveData.samples, waveData.sampleRate)
            stream.inputFinished()

            while (rec.isReady(stream)) {
                rec.decode(stream)
            }

            val text = rec.getResult(stream).text.trim()
            stream.release()

            if (text.isEmpty()) {
                Result.failure(Exception("未能识别出语音内容（可能说话声音过小或空白录音）"))
            } else {
                Result.success(text)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "transcribeWaveFile failed", t)
            Result.failure(t)
        }
    }

    /**
     * 转写 PCM 原始 short 数组
     */
    suspend fun transcribePcm(
        context: Context,
        pcmShorts: ShortArray,
        sampleRate: Int = 16000,
    ): Result<String> = withContext(Dispatchers.Default) {
        if (pcmShorts.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("音频数据为空"))
        }

        if (!isReady()) {
            val initRes = initialize(context)
            if (initRes.isFailure) {
                return@withContext Result.failure(initRes.exceptionOrNull() ?: Exception("引擎未就绪"))
            }
        }

        val rec = recognizer ?: return@withContext Result.failure(Exception("语音引擎不可用"))

        try {
            val floatSamples = FloatArray(pcmShorts.size) { i ->
                pcmShorts[i] / 32768.0f
            }
            val stream = rec.createStream()
            stream.acceptWaveform(floatSamples, sampleRate)
            stream.inputFinished()

            while (rec.isReady(stream)) {
                rec.decode(stream)
            }

            val text = rec.getResult(stream).text.trim()
            stream.release()

            if (text.isEmpty()) {
                Result.failure(Exception("未能识别出语音内容"))
            } else {
                Result.success(text)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * 释放原生资源
     */
    fun release() {
        synchronized(lock) {
            try {
                recognizer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing recognizer: ${e.message}")
            }
            recognizer = null
            isInitialized = false
        }
    }
}
