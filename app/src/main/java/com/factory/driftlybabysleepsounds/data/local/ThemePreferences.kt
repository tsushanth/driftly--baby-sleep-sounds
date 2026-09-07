package com.factory.driftlybabysleepsounds.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "driftly_settings")

class ThemePreferences(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        val stored = preferences[themeModeKey]
        AppThemeMode.entries.find { it.name == stored } ?: AppThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.name }
    }
}
