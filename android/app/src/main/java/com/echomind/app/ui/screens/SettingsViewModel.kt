package com.echomind.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echomind.app.data.model.TemplateType
import com.echomind.app.data.repository.NoteRepository
import com.echomind.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notionApiKey: String = "",
    val notionDatabaseId: String = "",
    val preferredTemplate: TemplateType = TemplateType.DAILY_REVIEW,
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
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application.dataStore)
    private val noteRepo = NoteRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState>

    init {
        uiState = combine(
            repo.settings,
            noteRepo.noteCount,
        ) { s, count ->
            SettingsUiState(
                notionApiKey = s.notionApiKey,
                notionDatabaseId = s.notionDatabaseId,
                preferredTemplate = s.preferredTemplate,
                selectedTheme = s.selectedTheme,
                isDarkMode = s.isDarkMode,
                autoSync = s.autoSync,
                autoTitle = s.autoTitle,
                autoList = s.autoList,
                autoTags = s.autoTags,
                silentStop = s.silentStop,
                recordCount = count,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState(),
        )
    }

    fun updateNotionKey(v: String)    { _uiState.update { it.copy(notionApiKey = v, saved = false) } }
    fun updateNotionDb(v: String)     { _uiState.update { it.copy(notionDatabaseId = v, saved = false) } }
    fun updateDarkMode(d: Boolean?)   { _uiState.update { it.copy(isDarkMode = d, saved = false) } }
    fun updateTheme(id: String)       { _uiState.update { it.copy(selectedTheme = id, saved = false) }; com.echomind.app.ui.theme.ThemeManager.setThemeById(id) }
    fun updateAutoSync(v: Boolean)    { _uiState.update { it.copy(autoSync = v, saved = false) } }
    fun updateAutoTitle(v: Boolean)   { _uiState.update { it.copy(autoTitle = v, saved = false) } }
    fun updateAutoList(v: Boolean)    { _uiState.update { it.copy(autoList = v, saved = false) } }
    fun updateAutoTags(v: Boolean)    { _uiState.update { it.copy(autoTags = v, saved = false) } }
    fun updateSilentStop(v: Boolean)  { _uiState.update { it.copy(silentStop = v, saved = false) } }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            repo.updateNotionKey(s.notionApiKey)
            repo.updateNotionDatabaseId(s.notionDatabaseId)
            repo.updateDarkMode(s.isDarkMode)
            repo.updateSelectedTheme(s.selectedTheme)
            repo.updateAutoSync(s.autoSync)
            repo.updateAutoTitle(s.autoTitle)
            repo.updateAutoList(s.autoList)
            repo.updateAutoTags(s.autoTags)
            repo.updateSilentStop(s.silentStop)
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            noteRepo.deleteAllNotes()
            _uiState.update { it.copy(recordCount = 0, dataCleared = true) }
        }
    }
}
