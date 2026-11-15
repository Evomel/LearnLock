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

class SessionStore(private val context: Context) {
    private val unlockedUntil = longPreferencesKey("unlocked_until")
    private val consecutiveWrong = intPreferencesKey("consecutive_wrong")
    private val accuracy = doublePreferencesKey("accuracy")

    val state: Flow<SessionState> = context.sessionDataStore.data.map { prefs ->
        SessionState(
            unlockedUntilMillis = prefs[unlockedUntil] ?: 0L,
            consecutiveWrong = prefs[consecutiveWrong] ?: 0,
            lastAccuracyWindow = prefs[accuracy] ?: 0.7
        )
    }

    suspend fun setUnlockedUntil(millis: Long) {
        context.sessionDataStore.edit { it[unlockedUntil] = millis }
    }

    suspend fun setConsecutiveWrong(value: Int) {
        context.sessionDataStore.edit { it[consecutiveWrong] = value }
    }

    suspend fun setAccuracy(value: Double) {
        context.sessionDataStore.edit { it[accuracy] = value }
    }

    suspend fun clearUnlock() {
        context.sessionDataStore.edit { it[unlockedUntil] = 0L }
    }
}
