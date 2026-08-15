package com.echomind.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echomind.app.data.model.AppSettings
import com.echomind.app.data.model.TemplateType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_NOTION_API_KEY = stringPreferencesKey("notion_api_key")
        val KEY_NOTION_DATABASE_ID = stringPreferencesKey("notion_database_id")
        val KEY_PREFERRED_TEMPLATE = stringPreferencesKey("preferred_template")
        val KEY_SELECTED_THEME = stringPreferencesKey("selected_theme")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val KEY_AUTO_TITLE = booleanPreferencesKey("auto_title")
        val KEY_AUTO_LIST = booleanPreferencesKey("auto_list")
        val KEY_AUTO_TAGS = booleanPreferencesKey("auto_tags")
        val KEY_SILENT_STOP = booleanPreferencesKey("silent_stop")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            notionApiKey = prefs[KEY_NOTION_API_KEY] ?: "",
            notionDatabaseId = prefs[KEY_NOTION_DATABASE_ID] ?: "",
            preferredTemplate = try {
                TemplateType.valueOf(prefs[KEY_PREFERRED_TEMPLATE] ?: "DAILY_REVIEW")
            } catch (_: Exception) { TemplateType.DAILY_REVIEW },
            selectedTheme = prefs[KEY_SELECTED_THEME] ?: "liquid-glass",
            isDarkMode = prefs[KEY_DARK_MODE],
            autoSync = prefs[KEY_AUTO_SYNC] ?: true,
            autoTitle = prefs[KEY_AUTO_TITLE] ?: true,
            autoList = prefs[KEY_AUTO_LIST] ?: true,
            autoTags = prefs[KEY_AUTO_TAGS] ?: true,
            silentStop = prefs[KEY_SILENT_STOP] ?: true,
        )
    }

    suspend fun updateNotionKey(key: String) {
        dataStore.edit { it[KEY_NOTION_API_KEY] = key }
    }

    suspend fun updateNotionDatabaseId(id: String) {
        dataStore.edit { it[KEY_NOTION_DATABASE_ID] = id }
    }

    suspend fun updatePreferredTemplate(type: TemplateType) {
        dataStore.edit { it[KEY_PREFERRED_TEMPLATE] = type.name }
    }

    suspend fun updateDarkMode(enabled: Boolean?) {
        dataStore.edit {
            if (enabled != null) it[KEY_DARK_MODE] = enabled
            else it.remove(KEY_DARK_MODE)
        }
    }

    suspend fun updateAutoSync(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_SYNC] = enabled }
    }

    suspend fun updateAutoTitle(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_TITLE] = enabled }
    }

    suspend fun updateAutoList(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_LIST] = enabled }
    }

    suspend fun updateAutoTags(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_TAGS] = enabled }
    }

    suspend fun updateSilentStop(enabled: Boolean) {
        dataStore.edit { it[KEY_SILENT_STOP] = enabled }
    }

    suspend fun updateSelectedTheme(themeId: String) {
        dataStore.edit { it[KEY_SELECTED_THEME] = themeId }
    }
}
