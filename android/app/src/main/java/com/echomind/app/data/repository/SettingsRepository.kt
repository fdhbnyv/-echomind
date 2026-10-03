package com.echomind.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echomind.app.data.model.AppSettings
import com.echomind.app.data.model.TemplateType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val KEY_OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val KEY_OPENAI_MODEL = stringPreferencesKey("openai_model")
        val KEY_OPENAI_CUSTOM_MODELS = stringPreferencesKey("openai_custom_models")
        val KEY_NOTION_API_KEY = stringPreferencesKey("notion_api_key")
        val KEY_NOTION_DATABASE_ID = stringPreferencesKey("notion_database_id")
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_SUPABASE_ANON_KEY = stringPreferencesKey("supabase_anon_key")
        val KEY_CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val KEY_LAST_PULL_NOTES_AT = longPreferencesKey("last_pull_notes_at")
        val KEY_LAST_PULL_MEMORIES_AT = longPreferencesKey("last_pull_memories_at")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        val KEY_PREFERRED_TEMPLATE = stringPreferencesKey("preferred_template")
        val KEY_ASR_MODE = stringPreferencesKey("asr_mode")
        val KEY_LIVE_PREVIEW_ENABLED = booleanPreferencesKey("live_preview_enabled")
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
            openAiBaseUrl = prefs[KEY_OPENAI_BASE_URL] ?: "https://api.deepseek.com/v1",
            openAiApiKey = prefs[KEY_OPENAI_API_KEY] ?: "",
            openAiModel = prefs[KEY_OPENAI_MODEL] ?: "deepseek-flash",
            openAiCustomModels = prefs[KEY_OPENAI_CUSTOM_MODELS] ?: "deepseek-flash,deepseek-v4-pro,deepseek-chat",
            notionApiKey = prefs[KEY_NOTION_API_KEY] ?: "",
            notionDatabaseId = prefs[KEY_NOTION_DATABASE_ID] ?: "",
            supabaseUrl = prefs[KEY_SUPABASE_URL] ?: "",
            supabaseAnonKey = prefs[KEY_SUPABASE_ANON_KEY] ?: "",
            cloudSyncEnabled = prefs[KEY_CLOUD_SYNC_ENABLED] ?: false,
            preferredTemplate = try {
                TemplateType.valueOf(prefs[KEY_PREFERRED_TEMPLATE] ?: "AUTO")
            } catch (_: Exception) { TemplateType.AUTO },
            asrMode = try {
                com.echomind.app.data.model.AsrEngineMode.fromId(prefs[KEY_ASR_MODE] ?: "local")
            } catch (_: Exception) { com.echomind.app.data.model.AsrEngineMode.LOCAL },
            livePreviewEnabled = prefs[KEY_LIVE_PREVIEW_ENABLED] ?: true,
            selectedTheme = prefs[KEY_SELECTED_THEME] ?: "liquid-glass",
            isDarkMode = prefs[KEY_DARK_MODE],
            autoSync = prefs[KEY_AUTO_SYNC] ?: true,
            autoTitle = prefs[KEY_AUTO_TITLE] ?: true,
            autoList = prefs[KEY_AUTO_LIST] ?: true,
            autoTags = prefs[KEY_AUTO_TAGS] ?: true,
            silentStop = prefs[KEY_SILENT_STOP] ?: true,
        )
    }

    /** 云端增量拉取游标（updatedAt gte），0 = 下次全量拉取 */
    val lastPullNotesAt: Flow<Long> = dataStore.data.map { it[KEY_LAST_PULL_NOTES_AT] ?: 0L }
    val lastPullMemoriesAt: Flow<Long> = dataStore.data.map { it[KEY_LAST_PULL_MEMORIES_AT] ?: 0L }

    suspend fun setLastPullNotesAt(value: Long) {
        dataStore.edit { it[KEY_LAST_PULL_NOTES_AT] = value }
    }

    suspend fun setLastPullMemoriesAt(value: Long) {
        dataStore.edit { it[KEY_LAST_PULL_MEMORIES_AT] = value }
    }

    /** 本设备标识（云端行来源诊断），首次调用时生成 */
    suspend fun getOrCreateDeviceId(): String {
        val existing = dataStore.data.map { it[KEY_DEVICE_ID] }.first()
        return existing ?: generateDeviceId()
    }

    private suspend fun generateDeviceId(): String {
        var id = ""
        dataStore.edit { prefs ->
            id = prefs[KEY_DEVICE_ID] ?: UUID.randomUUID().toString().replace("-", "")
            prefs[KEY_DEVICE_ID] = id
        }
        return id
    }

    suspend fun updateOpenAiBaseUrl(url: String) {
        dataStore.edit { it[KEY_OPENAI_BASE_URL] = url.trim() }
    }

    suspend fun updateOpenAiApiKey(key: String) {
        dataStore.edit { it[KEY_OPENAI_API_KEY] = key.trim() }
    }

    suspend fun updateOpenAiModel(model: String) {
        dataStore.edit { it[KEY_OPENAI_MODEL] = model.trim() }
    }

    suspend fun updateOpenAiCustomModels(models: String) {
        dataStore.edit { it[KEY_OPENAI_CUSTOM_MODELS] = models }
    }

    suspend fun updateNotionKey(key: String) {
        dataStore.edit { it[KEY_NOTION_API_KEY] = key }
    }

    suspend fun updateNotionDatabaseId(id: String) {
        dataStore.edit { it[KEY_NOTION_DATABASE_ID] = id }
    }

    suspend fun updateSupabaseUrl(url: String) {
        dataStore.edit { it[KEY_SUPABASE_URL] = url.trim() }
    }

    suspend fun updateSupabaseAnonKey(key: String) {
        dataStore.edit { it[KEY_SUPABASE_ANON_KEY] = key.trim() }
    }

    suspend fun updateCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_CLOUD_SYNC_ENABLED] = enabled }
    }

    suspend fun updatePreferredTemplate(type: TemplateType) {
        dataStore.edit { it[KEY_PREFERRED_TEMPLATE] = type.name }
    }

    suspend fun updateAsrMode(mode: com.echomind.app.data.model.AsrEngineMode) {
        dataStore.edit { it[KEY_ASR_MODE] = mode.id }
    }

    suspend fun updateLivePreviewEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_LIVE_PREVIEW_ENABLED] = enabled }
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
