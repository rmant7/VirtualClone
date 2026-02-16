package com.virtualclone.app.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "model_settings")

data class ModelSettings(
    val maxTokens: Int = 150,
    val temperature: Float = 0.7f,
    val summarizationModel: String = "facebook/bart-large-cnn",
    val qaModel: String = "deepset/roberta-base-squad2"
)

@Singleton
class ModelSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val SUMMARIZATION_MODEL = stringPreferencesKey("summarization_model")
        val QA_MODEL = stringPreferencesKey("qa_model")
    }
    
    val settingsFlow: Flow<ModelSettings> = context.dataStore.data.map { prefs ->
        ModelSettings(
            maxTokens = prefs[PreferencesKeys.MAX_TOKENS] ?: 150,
            temperature = prefs[PreferencesKeys.TEMPERATURE] ?: 0.7f,
            summarizationModel = prefs[PreferencesKeys.SUMMARIZATION_MODEL] ?: "facebook/bart-large-cnn",
            qaModel = prefs[PreferencesKeys.QA_MODEL] ?: "deepset/roberta-base-squad2"
        )
    }
    
    suspend fun updateMaxTokens(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MAX_TOKENS] = value
        }
    }
    
    suspend fun updateTemperature(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TEMPERATURE] = value
        }
    }
    
    suspend fun updateSummarizationModel(value: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SUMMARIZATION_MODEL] = value
        }
    }
    
    suspend fun updateQaModel(value: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.QA_MODEL] = value
        }
    }
}
