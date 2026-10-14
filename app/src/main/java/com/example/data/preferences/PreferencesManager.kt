package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fokus_user_prefs")

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val dailyTargetMinutes: Int = 300, // 5 hours default
    val selectedGoalId: String = "default_goal_hsc",
    val onboardingCompleted: Boolean = false,
    val showQuotes: Boolean = true,
    val strictModeDefault: Boolean = false
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val DAILY_TARGET = intPreferencesKey("daily_target_minutes")
        val SELECTED_GOAL_ID = stringPreferencesKey("selected_goal_id")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SHOW_QUOTES = booleanPreferencesKey("show_quotes")
        val STRICT_MODE_DEFAULT = booleanPreferencesKey("strict_mode_default")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)

        val langStr = prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.code
        val language = if (langStr == "bn") AppLanguage.BANGLA else AppLanguage.ENGLISH

        val dailyTarget = prefs[Keys.DAILY_TARGET] ?: 300
        val goalId = prefs[Keys.SELECTED_GOAL_ID] ?: "default_goal_hsc"
        val onboarding = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        val quotes = prefs[Keys.SHOW_QUOTES] ?: true
        val strictMode = prefs[Keys.STRICT_MODE_DEFAULT] ?: false

        UserPreferences(
            themeMode = themeMode,
            language = language,
            dailyTargetMinutes = dailyTarget,
            selectedGoalId = goalId,
            onboardingCompleted = onboarding,
            showQuotes = quotes,
            strictModeDefault = strictMode
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setDailyTargetMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DAILY_TARGET] = minutes }
    }

    suspend fun setSelectedGoalId(goalId: String) {
        context.dataStore.edit { it[Keys.SELECTED_GOAL_ID] = goalId }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setStrictModeDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STRICT_MODE_DEFAULT] = enabled }
    }
}
