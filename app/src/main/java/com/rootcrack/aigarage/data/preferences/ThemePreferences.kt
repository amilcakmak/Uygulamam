// Dosya: app/src/main/java/com/rootcrack/aigarage/data/preferences/ThemePreferences.kt
package com.rootcrack.aigarage.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Açıklama: DataStore sabitleri ve extension'lar tek dosyada toplandı
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object ThemePreferences {
    private val THEME_KEY = stringPreferencesKey("theme_preference")
    
    // Açıklama: Tema sabitleri
    const val THEME_DARK_SPECIAL = "dark_special"
    const val THEME_BRIGHT_SPECIAL = "bright_special"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    
    fun getThemeFlow(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: THEME_DARK_SPECIAL
        }
    }
    
    suspend fun setTheme(context: Context, theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }
}
