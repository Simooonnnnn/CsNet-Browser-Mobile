package com.csnet.browser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")

    val isDarkMode: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[isDarkModeKey] ?: false
        }

    suspend fun setDarkMode(isDark: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[isDarkModeKey] = isDark
        }
    }
}