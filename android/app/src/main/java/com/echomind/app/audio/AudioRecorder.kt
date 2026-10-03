package com.echomind.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * EchoMind 高保真录音机。
 *
 * 特性：
 * - 优先采用 AudioRecord 录制 16kHz 16-bit 单声道 PCM 音频，保存为标准 WAV 格式。
 * - 实时输出 FloatArray 音频分块供 Sherpa-ONNX 离线语音识别引擎实时流式推理（边说边出字）。
 * - 实时计算精确振幅（0..32767）供波形动效和静音检测使用。
 * - 若底层硬件暂不支持 AudioRecord，平滑降级至系统 MediaRecorder（M4A 格式）。
 */
class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private val isRecordingRunning = AtomicBoolean(false)
    private val currentAmplitude = AtomicInteger(0)
    private var outputFile: File? = null

    // 备用 MediaRecorder 降级
    private var fallbackRecorder: MediaRecorder? = null

    // 静音监测线程
    private var silenceMonitorThread: Thread? = null
    private var onSilenceDetected: (() -> Unit)? = null
    private var silenceThreshold: Int = 200       // 振幅低于此值视为静音
    private var silenceDurationMs: Long = 2500L   // 持续静音毫秒数后触发
    private var checkIntervalMs: Long = 200L      // 振幅采样间隔

    /** 实时音频帧回调 (归一化至 [-1.0, 1.0] 的 16kHz 采样点) */
    var onPcmChunkListener: ((FloatArray) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startRecording(onChunk: ((FloatArray) -> Unit)? = null): File {
        val cacheDir = context.cacheDir
            ?: throw RuntimeException("缓存目录不可用，无法录音")

        onPcmChunkListener = onChunk
        val fileName = "recording_${System.currentTimeMillis()}.wav"
        val file = File(cacheDir, fileName)
        outputFile = file

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = minBufferSize.coerceAtLeast(SAMPLE_RATE * 2 / 10) // 约 100ms 缓冲区

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                throw RuntimeException("AudioRecord 初始化状态异常: state != STATE_INITIALIZED")
            }

            audioRecord = record
            record.startRecording()
            isRecordingRunning.set(true)

            recordThread = Thread({
                recordLoop(record, file, bufferSize)
            }, "echomind-audio-record").apply {
                isDaemon = true
                start()
            }

            return file
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord 录音启动失败，降级使用 MediaRecorder: ${e.message}")
            return startFallbackMediaRecorder(cacheDir)
        }
    }

    private fun recordLoop(record: AudioRecord, file: File, bufferSize: Int) {
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            // 先写入 44 字节空白 WAV 头，录音结束时回写正确长度
            writeWavHeader(fos, 0, 0, SAMPLE_RATE.toLong(), 1, (SAMPLE_RATE * 2).toLong())

            val shortBuffer = ShortArray(bufferSize / 2)
            val byteBuffer = ByteArray(bufferSize)

            while (isRecordingRunning.get()) {
                val shortsRead = record.read(shortBuffer, 0, shortBuffer.size)
                if (shortsRead > 0) {
                    var maxAmp = 0
                    val floatSamples = FloatArray(shortsRead)
                    for (i in 0 until shortsRead) {
                        val s = shortBuffer[i].toInt()
                        val abs = if (s < 0) -s else s
                        if (abs > maxAmp) maxAmp = abs
                        floatSamples[i] = s / 32768.0f

                        // 写入字节缓冲 (小端序)
                        byteBuffer[i * 2] = (s and 0xff).toByte()
                        byteBuffer[i * 2 + 1] = ((s shr 8) and 0xff).toByte()
                    }

                    currentAmplitude.set(maxAmp)
                    fos.write(byteBuffer, 0, shortsRead * 2)

                    // 传递实时 PCM 帧供 Sherpa-ONNX 流式转写
                    onPcmChunkListener?.invoke(floatSamples)
                }
            }

            fos.flush()
        } catch (e: Exception) {
            Log.e(TAG, "recordLoop 异常", e)
        } finally {
            try { fos?.close() } catch (_: Exception) {}
            // 回写更新 WAV 文件头中的实际数据大小
            if (file.exists() && file.length() >= 44) {
                fixWavHeader(file)
            }
        }
    }

    private fun fixWavHeader(file: File) {
        try {
            val totalAudioLen = file.length() - 44
            val totalDataLen = totalAudioLen + 36
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(4)
                raf.write(
                    byteArrayOf(
                        (totalDataLen and 0xff).toByte(),
                        ((totalDataLen shr 8) and 0xff).toByte(),
                        ((totalDataLen shr 16) and 0xff).toByte(),
                        ((totalDataLen shr 24) and 0xff).toByte(),
                    )
                )
                raf.seek(40)
                raf.write(
                    byteArrayOf(
                        (totalAudioLen and 0xff).toByte(),
                        ((totalAudioLen shr 8) and 0xff).toByte(),
                        ((totalAudioLen shr 16) and 0xff).toByte(),
                        ((totalAudioLen shr 24) and 0xff).toByte(),
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "修正 WAV 头尺寸失败: ${e.message}")
        }
    }

    private fun writeWavHeader(
        out: OutputStream,
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Int,
        byteRate: Long,
    ) {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align = 2
        header[33] = 0
        header[34] = 16 // 16 bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        out.write(header, 0, 44)
    }

    @Suppress("DEPRECATION")
    private fun startFallbackMediaRecorder(cacheDir: File): File {
        val fileName = "recording_${System.currentTimeMillis()}.m4a"
        val file = File(cacheDir, fileName)
        outputFile = file

        fallbackRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(SAMPLE_RATE)
            setAudioChannels(1)
            setAudioEncodingBitRate(64000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun stopRecording(): File? {
        stopSilenceMonitor()

        if (isRecordingRunning.getAndSet(false)) {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord stop failed: ${e.message}")
            }
            audioRecord = null
            try {
                recordThread?.join(1000)
            } catch (_: Exception) {}
            recordThread = null
        }

        if (fallbackRecorder != null) {
            try {
                fallbackRecorder?.apply {
                    stop()
                    release()
                }
            } catch (_: Exception) {}
            fallbackRecorder = null
        }

        currentAmplitude.set(0)
        onPcmChunkListener = null
        return outputFile
    }

    fun isRecording(): Boolean = isRecordingRunning.get() || fallbackRecorder != null

    fun getAmplitude(): Int {
        return if (fallbackRecorder != null) {
            try { fallbackRecorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
        } else {
            currentAmplitude.get()
        }
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

        stopSilenceMonitor()

        silenceMonitorThread = Thread {
            var silentStartMs = 0L
            while (isRecording() && !Thread.currentThread().isInterrupted) {
                val amplitude = getAmplitude()
                if (amplitude < silenceThreshold) {
                    if (silentStartMs == 0L) {
                        silentStartMs = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - silentStartMs >= silenceDurationMs) {
                        this.onSilenceDetected?.invoke()
                        break
                    }
                } else {
                    silentStartMs = 0L
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
     */
    fun stopSilenceMonitor() {
        silenceMonitorThread?.interrupt()
        silenceMonitorThread = null
        onSilenceDetected = null
    }
}
