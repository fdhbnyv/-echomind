package com.echomind.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echomind.app.audio.SherpaAsrEngine
import com.echomind.app.data.api.OpenAiClient
import com.echomind.app.data.model.AsrEngineMode
import com.echomind.app.data.model.TemplateType
import com.echomind.app.data.repository.NoteRepository
import com.echomind.app.data.repository.SettingsRepository
import com.echomind.app.data.sync.CloudSyncManager
import com.echomind.app.ui.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val openAiBaseUrl: String = "https://api.deepseek.com/v1",
    val openAiApiKey: String = "",
    val openAiModel: String = "deepseek-flash",
    val availableModels: List<String> = listOf("deepseek-flash", "deepseek-v4-pro", "deepseek-chat"),
    val isTestingAi: Boolean = false,
    val testAiMessage: String? = null,
    val testAiSuccess: Boolean? = null,
    val notionApiKey: String = "",
    val notionDatabaseId: String = "",
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val cloudSyncEnabled: Boolean = false,
    val preferredTemplate: TemplateType = TemplateType.AUTO,
    val asrMode: AsrEngineMode = AsrEngineMode.LOCAL,
    val livePreviewEnabled: Boolean = true,
    val isLocalAsrReady: Boolean = false,
    val localAsrModelName: String = "Zipformer-zh 14M (内置端侧离线)",
    val selectedTheme: String = "liquid-glass",
    val isDarkMode: Boolean? = null,
    val autoSync: Boolean = true,
    val autoTitle: Boolean = true,
    val autoList: Boolean = true,
    val autoTags: Boolean = true,
    val silentStop: Boolean = true,
    val saved: Boolean = false,
    val recordCount: Int = 0,
    val dataCleared: Boolean = false,
    /** null = 空闲；"syncing" = 同步中；"done"/"error" = 结果提示 */
    val cloudSyncStatus: String? = null,
) {
    val openAiConfigured: Boolean
        get() = openAiBaseUrl.isNotBlank() && openAiApiKey.isNotBlank() && openAiModel.isNotBlank()

    val cloudSyncConfigured: Boolean
        get() = cloudSyncEnabled && supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application.dataStore)
    private val noteRepo = NoteRepository(application)
    private val openAiClient = OpenAiClient()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val s = repo.settings.first()
            val count = noteRepo.noteCount.first()
            val cachedModels = s.openAiCustomModels.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            _uiState.update {
                it.copy(
                    openAiBaseUrl = s.openAiBaseUrl,
                    openAiApiKey = s.openAiApiKey,
                    openAiModel = s.openAiModel,
                    availableModels = cachedModels.ifEmpty { listOf("deepseek-flash", "deepseek-v4-pro", "deepseek-chat") },
                    notionApiKey = s.notionApiKey,
                    notionDatabaseId = s.notionDatabaseId,
                    supabaseUrl = s.supabaseUrl,
                    supabaseAnonKey = s.supabaseAnonKey,
                    cloudSyncEnabled = s.cloudSyncEnabled,
                    preferredTemplate = s.preferredTemplate,
                    asrMode = s.asrMode,
                    livePreviewEnabled = s.livePreviewEnabled,
                    isLocalAsrReady = SherpaAsrEngine.isModelPresent(application),
                    selectedTheme = s.selectedTheme,
                    isDarkMode = s.isDarkMode,
                    autoSync = s.autoSync,
                    autoTitle = s.autoTitle,
                    autoList = s.autoList,
                    autoTags = s.autoTags,
                    silentStop = s.silentStop,
                    recordCount = count,
                )
            }
            // 同步全局主题状态
            ThemeManager.setThemeById(s.selectedTheme)
            ThemeManager.updateDarkMode(s.isDarkMode)
        }

        // 监听记录数变化
        viewModelScope.launch {
            noteRepo.noteCount.collect { count ->
                _uiState.update { it.copy(recordCount = count) }
            }
        }
    }

    fun updateOpenAiBaseUrl(v: String) { _uiState.update { it.copy(openAiBaseUrl = v, saved = false, testAiMessage = null) } }
    fun updateOpenAiApiKey(v: String)  { _uiState.update { it.copy(openAiApiKey = v, saved = false, testAiMessage = null) } }
    fun updateOpenAiModel(v: String)   { _uiState.update { it.copy(openAiModel = v, saved = false) } }

    fun applyPreset(baseUrl: String, defaultModel: String) {
        _uiState.update {
            it.copy(
                openAiBaseUrl = baseUrl,
                openAiModel = defaultModel,
                saved = false,
                testAiMessage = null,
                testAiSuccess = null
            )
        }
    }

    fun testConnectionAndFetchModels() {
        val baseUrl = _uiState.value.openAiBaseUrl
        val apiKey = _uiState.value.openAiApiKey
        _uiState.update { it.copy(isTestingAi = true, testAiMessage = null, testAiSuccess = null) }

        viewModelScope.launch {
            val result = openAiClient.testConnectionAndFetchModels(baseUrl, apiKey)
            if (result.isSuccess) {
                val models = result.getOrThrow()
                val currentModel = _uiState.value.openAiModel
                val updatedModel = if (models.contains(currentModel)) currentModel else (models.firstOrNull() ?: currentModel)
                _uiState.update {
                    it.copy(
                        isTestingAi = false,
                        testAiSuccess = true,
                        testAiMessage = "✓ 连接成功！已拉取到 ${models.size} 个模型",
                        availableModels = models,
                        openAiModel = updatedModel,
                    )
                }
                repo.updateOpenAiCustomModels(models.joinToString(","))
            } else {
                _uiState.update {
                    it.copy(
                        isTestingAi = false,
                        testAiSuccess = false,
                        testAiMessage = "✗ ${result.exceptionOrNull()?.message ?: "连接失败"}",
                    )
                }
            }
        }
    }

    fun updateNotionKey(v: String)    { _uiState.update { it.copy(notionApiKey = v, saved = false) } }
    fun updateNotionDb(v: String)     { _uiState.update { it.copy(notionDatabaseId = v, saved = false) } }
    fun updateSupabaseUrl(v: String)  { _uiState.update { it.copy(supabaseUrl = v, saved = false) } }
    fun updateSupabaseKey(v: String)  { _uiState.update { it.copy(supabaseAnonKey = v, saved = false) } }
    fun updateCloudSync(v: Boolean)   { _uiState.update { it.copy(cloudSyncEnabled = v, saved = false) } }

    fun updateDarkMode(d: Boolean?) {
        _uiState.update { it.copy(isDarkMode = d, saved = false) }
        ThemeManager.updateDarkMode(d)
        viewModelScope.launch { repo.updateDarkMode(d) }
    }

    fun updateTheme(id: String) {
        _uiState.update { it.copy(selectedTheme = id, saved = false) }
        ThemeManager.setThemeById(id)
        viewModelScope.launch { repo.updateSelectedTheme(id) }
    }

    fun updateAutoSync(v: Boolean)    { _uiState.update { it.copy(autoSync = v, saved = false) } }
    fun updateAutoTitle(v: Boolean)   { _uiState.update { it.copy(autoTitle = v, saved = false) } }
    fun updateAutoList(v: Boolean)    { _uiState.update { it.copy(autoList = v, saved = false) } }
    fun updateAutoTags(v: Boolean)    { _uiState.update { it.copy(autoTags = v, saved = false) } }
    fun updateSilentStop(v: Boolean)  { _uiState.update { it.copy(silentStop = v, saved = false) } }

    fun updateAsrMode(mode: AsrEngineMode) {
        _uiState.update { it.copy(asrMode = mode, saved = false) }
        viewModelScope.launch { repo.updateAsrMode(mode) }
    }

    fun updateLivePreviewEnabled(enabled: Boolean) {
        _uiState.update { it.copy(livePreviewEnabled = enabled, saved = false) }
        viewModelScope.launch { repo.updateLivePreviewEnabled(enabled) }
    }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            repo.updateOpenAiBaseUrl(s.openAiBaseUrl)
            repo.updateOpenAiApiKey(s.openAiApiKey)
            repo.updateOpenAiModel(s.openAiModel)
            repo.updateNotionKey(s.notionApiKey)
            repo.updateNotionDatabaseId(s.notionDatabaseId)
            repo.updateSupabaseUrl(s.supabaseUrl)
            repo.updateSupabaseAnonKey(s.supabaseAnonKey)
            repo.updateCloudSyncEnabled(s.cloudSyncEnabled)
            repo.updateDarkMode(s.isDarkMode)
            repo.updateSelectedTheme(s.selectedTheme)
            repo.updateAutoSync(s.autoSync)
            repo.updateAutoTitle(s.autoTitle)
            repo.updateAutoList(s.autoList)
            repo.updateAutoTags(s.autoTags)
            repo.updateSilentStop(s.silentStop)
            repo.updateAsrMode(s.asrMode)
            repo.updateLivePreviewEnabled(s.livePreviewEnabled)
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** 立即执行云端同步（先落盘未保存的配置） */
    fun syncNow() {
        viewModelScope.launch {
            save()
            _uiState.update { it.copy(cloudSyncStatus = "syncing") }
            val ok = runCatching {
                CloudSyncManager(getApplication(), repo).syncAll()
            }.getOrDefault(false)
            _uiState.update {
                it.copy(cloudSyncStatus = if (ok) "done" else "error")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            noteRepo.deleteAllNotes()
            _uiState.update { it.copy(dataCleared = true) }
        }
    }
}
