package com.echomind.app.ui.screens

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echomind.app.audio.AudioRecorder
import com.echomind.app.data.api.DashScopeApi
import com.echomind.app.data.api.NotionApi
import com.echomind.app.data.model.RecordingState
import com.echomind.app.data.model.StructuredNote
import com.echomind.app.data.model.TemplateType
import com.echomind.app.data.repository.NoteRepository
import com.echomind.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val Application.dataStore by preferencesDataStore(name = "echomind_settings")

data class MainUiState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val selectedTemplate: TemplateType = TemplateType.DAILY_REVIEW,
    val transcription: String = "",
    val structuredNote: StructuredNote? = null,
    val currentNoteId: Long = 0L,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val hasApiKeys: Boolean = false,
    val audioAmplitude: Int = 0,
    val recentNotes: List<NoteListItem> = emptyList(),
)

/** UI 模型：列表中的笔记摘要 */
data class NoteListItem(
    val id: Long,
    val title: String,
    val date: String,
    val isVoice: Boolean,
    val synced: Boolean,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application.dataStore)
    private val noteRepo = NoteRepository(application)
    private val audioRecorder = AudioRecorder(application)

    private var dashScopeApi: DashScopeApi? = null
    private var notionApi: NotionApi? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { s ->
                dashScopeApi = DashScopeApi()
                notionApi = if (s.notionApiKey.isNotBlank()) NotionApi(s.notionApiKey) else null
                com.echomind.app.ui.theme.ThemeManager.setThemeById(s.selectedTheme)
                _uiState.update { it.copy(
                    hasApiKeys = true,
                    selectedTemplate = s.preferredTemplate,
                )}
            }
        }
        // Collect recent notes for home screen display
        viewModelScope.launch {
            noteRepo.allEntities.collect { entities ->
                _uiState.update { it.copy(
                    recentNotes = entities.map { e ->
                        NoteListItem(
                            id = e.id,
                            title = e.title,
                            date = e.date,
                            isVoice = e.isVoice,
                            synced = e.synced,
                        )
                    }
                )}
            }
        }
    }

    fun selectTemplate(type: TemplateType) {
        _uiState.update { it.copy(selectedTemplate = type) }
        viewModelScope.launch { settingsRepo.updatePreferredTemplate(type) }
    }

    fun startRecording() {
        if (_uiState.value.recordingState == RecordingState.RECORDING) return
        try {
            audioRecorder.startRecording()
            _uiState.update { it.copy(recordingState = RecordingState.RECORDING, transcription = "", structuredNote = null, errorMessage = null) }
        } catch (t: Throwable) {
            _uiState.update { it.copy(recordingState = RecordingState.ERROR, errorMessage = "录音启动失败: ${t.message}") }
        }
    }

    fun stopRecording() {
        if (_uiState.value.recordingState != RecordingState.RECORDING) return
        val audioFile = audioRecorder.stopRecording()
        if (audioFile == null || !audioFile.exists()) {
            _uiState.update { it.copy(recordingState = RecordingState.IDLE, errorMessage = "录音文件未找到") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(recordingState = RecordingState.TRANSCRIBING) }
            transcribeAndStructure(audioFile)
        }
    }

    private suspend fun transcribeAndStructure(audioFile: java.io.File) {
        _uiState.update { it.copy(recordingState = RecordingState.TRANSCRIBING) }
        val tr = dashScopeApi?.transcribe(audioFile)
        if (tr == null || tr.isFailure) {
            _uiState.update { it.copy(recordingState = RecordingState.ERROR, errorMessage = "转写失败: ${tr?.exceptionOrNull()?.message ?: "API 不可用"}") }
            return
        }
        val text = tr.getOrThrow()
        _uiState.update { it.copy(transcription = text, recordingState = RecordingState.STRUCTURING) }
        val sr = dashScopeApi!!.structureNote(text, _uiState.value.selectedTemplate.id)
        finishPipeline(sr, text, isVoice = true)
    }

    private suspend fun finishPipeline(sr: Result<StructuredNote>, transcription: String, isVoice: Boolean = false) {
        if (sr.isFailure) { _uiState.update { it.copy(recordingState = RecordingState.ERROR, errorMessage = "结构化失败: ${sr.exceptionOrNull()?.message}") }; return }
        val note = sr.getOrThrow().copy(rawTranscription = transcription)
        val s = settingsRepo.settings.first()
        var synced = false
        var queuedOffline = false
        if (notionApi != null && s.notionDatabaseId.isNotBlank() && s.autoSync) {
            val result = notionApi!!.writeNote(note, s.notionDatabaseId)
            synced = result.isSuccess
            if (!synced) {
                // Offline queue: save to pending sync
                val app = getApplication<android.app.Application>()
                val pendingDb = com.echomind.app.service.PendingSyncDatabase.getInstance(app)
                pendingDb.pendingSyncDao().insert(
                    com.echomind.app.service.PendingSync(
                        templateType = note.templateType,
                        transcription = note.rawTranscription,
                        structuredNoteJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            .encodeToString(com.echomind.app.data.model.StructuredNote.serializer(), note),
                        notionDbId = s.notionDatabaseId,
                    )
                )
                com.echomind.app.service.SyncWorker.enqueueDelayed(app)
                queuedOffline = true
            }
        }
        // 保存到本地数据库
        val savedId = noteRepo.saveNote(note, isVoice = isVoice, synced = synced)
        _uiState.update { it.copy(recordingState = RecordingState.COMPLETED, structuredNote = note, currentNoteId = savedId) }
    }

    fun resetState() {
        _uiState.update { MainUiState(selectedTemplate = it.selectedTemplate, hasApiKeys = it.hasApiKeys) }
    }

    /** 真实录音：直接由 UI 层传入 AudioRecorder，替代原来的 startRecording 逻辑 */
    fun startRecordingReal(audioRecorder: com.echomind.app.audio.AudioRecorder) {
        if (_uiState.value.recordingState == RecordingState.RECORDING) return
        try {
            audioRecorder.startRecording()
            _uiState.update { it.copy(recordingState = RecordingState.RECORDING, transcription = "", structuredNote = null, errorMessage = null) }
            // 启动静音监测线程，持续静音 2.5 秒后自动停止录音
            audioRecorder.startSilenceMonitor {
                stopRecordingReal(audioRecorder)
            }
        } catch (t: Throwable) {
            _uiState.update { it.copy(recordingState = RecordingState.ERROR, errorMessage = "录音启动失败: ${t.message}") }
        }
    }

    /** 停止真实录音并触发转写+结构化流程 */
    fun stopRecordingReal(audioRecorder: com.echomind.app.audio.AudioRecorder) {
        if (_uiState.value.recordingState != RecordingState.RECORDING) return
        val audioFile = audioRecorder.stopRecording()
        if (audioFile == null || !audioFile.exists()) {
            _uiState.update { it.copy(recordingState = RecordingState.IDLE, errorMessage = "录音文件未找到") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(recordingState = RecordingState.TRANSCRIBING) }
            transcribeAndStructure(audioFile)
        }
    }

    /** 直接处理文字输入：跳过语音转写，用 AI 结构化 */
    fun processTextInput(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(recordingState = RecordingState.STRUCTURING, transcription = text) }
            val sr = dashScopeApi?.structureNote(text, _uiState.value.selectedTemplate.id)
            if (sr == null || sr.isFailure) {
                _uiState.update { it.copy(recordingState = RecordingState.ERROR, errorMessage = "AI 处理失败: ${sr?.exceptionOrNull()?.message ?: "API 不可用"}") }
                return@launch
            }
            finishPipeline(sr, text, isVoice = false)
        }
    }

    // ── 结果编辑 ──

    /** 进入编辑态 */
    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    /** 取消编辑，恢复原始 note */
    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    /** 保存编辑后的 note 到 Room 并更新 UI */
    fun saveEditedNote(updated: StructuredNote) {
        val id = _uiState.value.currentNoteId
        if (id <= 0) return
        viewModelScope.launch {
            noteRepo.updateNote(updated, id)
            _uiState.update { it.copy(structuredNote = updated, isEditing = false) }
        }
    }

    // ── 浏览历史记录 ──

    fun loadNoteById(id: Long) {
        viewModelScope.launch {
            val note = noteRepo.getNoteById(id) ?: return@launch
            _uiState.update { it.copy(
                structuredNote = note,
                currentNoteId = id,
                recordingState = RecordingState.COMPLETED,
                isEditing = false,
                errorMessage = null,
            )}
        }
    }

    // ── 保存原始文本（无需 API Key）──

    /** 将用户输入的原始文本直接保存到本地，不经过 AI 处理 */
    fun saveRawText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val note = StructuredNote(
                templateType = _uiState.value.selectedTemplate.id,
                title = text.lines().firstOrNull()?.take(30) ?: "笔记",
                date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                summary = text.take(50),
                actionItems = emptyList(),
                tags = emptyList(),
                rawTranscription = text,
            )
            noteRepo.saveNote(note, isVoice = false, synced = false)
            _uiState.update { it.copy(
                recordingState = RecordingState.COMPLETED,
                structuredNote = note,
                errorMessage = null,
            )}
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
    }
}

