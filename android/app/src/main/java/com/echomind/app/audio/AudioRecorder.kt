package com.echomind.app.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    // 静音监测线程
    private var silenceMonitorThread: Thread? = null
    private var onSilenceDetected: (() -> Unit)? = null

    // 静音参数（可调）
    private var silenceThreshold: Int = 200       // 振幅低于此值视为静音
    private var silenceDurationMs: Long = 2500L   // 持续静音毫秒数后触发
    private var checkIntervalMs: Long = 200L      // 振幅采样间隔

    fun startRecording(): File {
        // cacheDir 可能为 null（存储空间不足等极端情况）
        val cacheDir = context.cacheDir
            ?: throw RuntimeException("缓存目录不可用，无法录音")

        val fileName = "recording_${System.currentTimeMillis()}.m4a"
        val file = File(cacheDir, fileName)

        // 使用无参构造 + @Suppress 兼容更多 OEM 设备
        // MediaRecorder(Context) 在某些国产 ROM 上可能抛出 NoSuchMethodError
        @Suppress("DEPRECATION")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000)
            setAudioChannels(1)
            setAudioEncodingBitRate(64000)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                throw RuntimeException("录音初始化失败: ${e.message}", e)
            }
        }

        outputFile = file
        return file
    }

    fun stopRecording(): File? {
        stopSilenceMonitor()
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // 录音太短时 stop() 可能失败，忽略
        }
        recorder = null
        return outputFile
    }

    fun isRecording(): Boolean = recorder != null

    fun getAmplitude(): Int {
        return recorder?.maxAmplitude ?: 0
    }

    // ── 静音自动结束 ──

    /**
     * 启动后台线程监测振幅，当持续静音达到 [durationMs] 时回调 [onSilenceDetected]。
     *
     * @param threshold     振幅阈值（低于此视为静音），默认 200
     * @param durationMs    持续静音时长，默认 2500ms
     * @param checkIntervalMs 采样间隔，默认 200ms
     * @param onSilenceDetected 静音超时回调（通常在此调用 stopRecording）
     */
    fun startSilenceMonitor(
        threshold: Int = this.silenceThreshold,
        durationMs: Long = this.silenceDurationMs,
        checkIntervalMs: Long = this.checkIntervalMs,
        onSilenceDetected: () -> Unit,
    ) {
        this.silenceThreshold = threshold
        this.silenceDurationMs = durationMs
        this.checkIntervalMs = checkIntervalMs
        this.onSilenceDetected = onSilenceDetected

        stopSilenceMonitor() // 清理旧线程

        silenceMonitorThread = Thread {
            var silentStartMs = 0L
            while (isRecording() && !Thread.currentThread().isInterrupted) {
                val amplitude = getAmplitude()
                if (amplitude < silenceThreshold) {
                    if (silentStartMs == 0L) {
                        silentStartMs = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - silentStartMs >= silenceDurationMs) {
                        // 静音持续足够长，触发回调
                        this.onSilenceDetected?.invoke()
                        break
                    }
                } else {
                    silentStartMs = 0L // 有声音，重置计时
                }
                try {
                    Thread.sleep(checkIntervalMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }.apply {
            isDaemon = true
            name = "silence-monitor"
            start()
        }
    }

    /**
     * 停止静音监测线程。
     * 在 [stopRecording] 中会自动调用，通常无需手动调用。
     */
    fun stopSilenceMonitor() {
        silenceMonitorThread?.interrupt()
        silenceMonitorThread = null
        onSilenceDetected = null
    }
}
