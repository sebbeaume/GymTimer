package com.gymtimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore is the modern replacement for SharedPreferences.
 * It stores simple key/value data asynchronously using Kotlin coroutines,
 * so writes never block the UI thread.
 *
 * We use a Kotlin extension property on Context to create a single DataStore
 * instance for the whole app (the delegate handles the singleton logic).
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        // Type-safe key: only Int values can be stored under this key
        val REST_DURATION_KEY = intPreferencesKey("rest_duration_seconds")
        const val DEFAULT_REST_SECONDS = 90
    }

    /**
     * A Flow that emits the current rest duration every time it changes.
     * The UI can collect this to stay in sync without polling.
     */
    val restDurationFlow: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[REST_DURATION_KEY] ?: DEFAULT_REST_SECONDS }

    suspend fun setRestDuration(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[REST_DURATION_KEY] = seconds
        }
    }
}
