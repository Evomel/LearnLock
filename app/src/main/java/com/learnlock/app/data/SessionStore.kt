package com.learnlock.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore("session")

/**
 * Хранилище текущей «свободной» сессии (DataStore Preferences).
 * Отдельно от Room, чтобы быстро читать/писать без миграций схемы.
 */
class SessionStore(private val context: Context) {
    private val unlockedUntil = longPreferencesKey("unlocked_until")
    private val nextLaunchAt = longPreferencesKey("next_launch_at")
    private val tasksSolved = intPreferencesKey("tasks_solved")
    private val consecutiveWrong = intPreferencesKey("consecutive_wrong")
    private val accuracy = doublePreferencesKey("accuracy")

    /** Поток состояния сессии для UI и проверок блокировки. */
    val state: Flow<SessionState> = context.sessionDataStore.data.map { prefs ->
        SessionState(
            unlockedUntilMillis = prefs[unlockedUntil] ?: 0L,
            nextLaunchAtMillis = prefs[nextLaunchAt] ?: 0L,
            tasksSolved = prefs[tasksSolved] ?: 0,
            consecutiveWrong = prefs[consecutiveWrong] ?: 0,
            lastAccuracyWindow = prefs[accuracy] ?: 0.7
        )
    }

    /** Задаёт момент, до которого телефон свободен без задачи. */
    suspend fun setUnlockedUntil(millis: Long) {
        context.sessionDataStore.edit { it[unlockedUntil] = millis }
    }

    suspend fun setNextLaunchAt(millis: Long) {
        context.sessionDataStore.edit { it[nextLaunchAt] = millis }
    }

    suspend fun setTasksSolved(value: Int) {
        context.sessionDataStore.edit { it[tasksSolved] = value }
    }

    /** Обновляет счётчик подряд идущих ошибок. */
    suspend fun setConsecutiveWrong(value: Int) {
        context.sessionDataStore.edit { it[consecutiveWrong] = value }
    }

    /** Кэширует accuracy для генератора задач. */
    suspend fun setAccuracy(value: Double) {
        context.sessionDataStore.edit { it[accuracy] = value }
    }

    /** Немедленно завершает свободную сессию (кнопка «Завершить сессию»). */
    suspend fun clearUnlock() {
        context.sessionDataStore.edit { it[unlockedUntil] = 0L }
    }
}
