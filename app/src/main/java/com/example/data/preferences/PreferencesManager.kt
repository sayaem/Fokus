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
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fokus_user_prefs")

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class DailyAvailability(
    val availableMinutesPerDay: Int = 240, // 4 hours
    val preferredStartTime: String = "09:00",
    val preferredEndTime: String = "23:00",
    val defaultSessionDurationMinutes: Int = 50,
    val breakDurationMinutes: Int = 10,
    val weeklyStudyDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // Calendar.SUNDAY..Calendar.SATURDAY
    val preferredSubjectIds: Set<String> = emptySet(),
    val daySpecificOverridesJson: String = "{}" // "{epochDay: minutes}"
)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val dailyTargetMinutes: Int = 300, // 5 hours default
    val selectedGoalId: String = "default_goal_hsc",
    val onboardingCompleted: Boolean = false,
    val showQuotes: Boolean = true,
    val strictModeDefault: Boolean = false,
    val availability: DailyAvailability = DailyAvailability(),
    val revisionIntervals: List<Int> = listOf(1, 3, 7, 14, 30)
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

        // Availability Keys
        val AVAILABLE_MINUTES = intPreferencesKey("avail_minutes_per_day")
        val PREF_START_TIME = stringPreferencesKey("avail_start_time")
        val PREF_END_TIME = stringPreferencesKey("avail_end_time")
        val DEFAULT_SESSION_DURATION = intPreferencesKey("avail_session_duration")
        val BREAK_DURATION = intPreferencesKey("avail_break_duration")
        val WEEKLY_STUDY_DAYS = stringPreferencesKey("avail_weekly_days")
        val PREFERRED_SUBJECT_IDS = stringPreferencesKey("avail_preferred_subjects")
        val DAY_OVERRIDES_JSON = stringPreferencesKey("avail_day_overrides_json")

        // Revision Intervals Key
        val REVISION_INTERVALS_CSV = stringPreferencesKey("revision_intervals_csv")
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

        // Parse availability
        val availMinutes = prefs[Keys.AVAILABLE_MINUTES] ?: 240
        val startTime = prefs[Keys.PREF_START_TIME] ?: "09:00"
        val endTime = prefs[Keys.PREF_END_TIME] ?: "23:00"
        val sessionDur = prefs[Keys.DEFAULT_SESSION_DURATION] ?: 50
        val breakDur = prefs[Keys.BREAK_DURATION] ?: 10
        val daysStr = prefs[Keys.WEEKLY_STUDY_DAYS] ?: "1,2,3,4,5,6,7"
        val parsedDays = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        val subjectsStr = prefs[Keys.PREFERRED_SUBJECT_IDS] ?: ""
        val parsedSubjects = if (subjectsStr.isNotBlank()) subjectsStr.split(",").map { it.trim() }.toSet() else emptySet()
        val overridesJson = prefs[Keys.DAY_OVERRIDES_JSON] ?: "{}"

        val availability = DailyAvailability(
            availableMinutesPerDay = availMinutes,
            preferredStartTime = startTime,
            preferredEndTime = endTime,
            defaultSessionDurationMinutes = sessionDur,
            breakDurationMinutes = breakDur,
            weeklyStudyDays = if (parsedDays.isNotEmpty()) parsedDays else setOf(1, 2, 3, 4, 5, 6, 7),
            preferredSubjectIds = parsedSubjects,
            daySpecificOverridesJson = overridesJson
        )

        val intervalsStr = prefs[Keys.REVISION_INTERVALS_CSV] ?: "1,3,7,14,30"
        val intervals = intervalsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
            .let { if (it.isNotEmpty()) it else listOf(1, 3, 7, 14, 30) }

        UserPreferences(
            themeMode = themeMode,
            language = language,
            dailyTargetMinutes = dailyTarget,
            selectedGoalId = goalId,
            onboardingCompleted = onboarding,
            showQuotes = quotes,
            strictModeDefault = strictMode,
            availability = availability,
            revisionIntervals = intervals
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

    suspend fun updateAvailability(
        availableMinutes: Int,
        startTime: String,
        endTime: String,
        sessionDuration: Int,
        breakDuration: Int,
        studyDays: Set<Int>,
        preferredSubjects: Set<String>
    ) {
        context.dataStore.edit {
            it[Keys.AVAILABLE_MINUTES] = availableMinutes
            it[Keys.PREF_START_TIME] = startTime
            it[Keys.PREF_END_TIME] = endTime
            it[Keys.DEFAULT_SESSION_DURATION] = sessionDuration
            it[Keys.BREAK_DURATION] = breakDuration
            it[Keys.WEEKLY_STUDY_DAYS] = studyDays.joinToString(",")
            it[Keys.PREFERRED_SUBJECT_IDS] = preferredSubjects.joinToString(",")
        }
    }

    suspend fun setDaySpecificAvailableMinutes(epochDay: Long, minutes: Int) {
        context.dataStore.edit { prefs ->
            val currentJsonStr = prefs[Keys.DAY_OVERRIDES_JSON] ?: "{}"
            val obj = try { JSONObject(currentJsonStr) } catch (e: Exception) { JSONObject() }
            obj.put(epochDay.toString(), minutes)
            prefs[Keys.DAY_OVERRIDES_JSON] = obj.toString()
        }
    }

    suspend fun setRevisionIntervals(intervals: List<Int>) {
        context.dataStore.edit {
            it[Keys.REVISION_INTERVALS_CSV] = intervals.joinToString(",")
        }
    }
}
