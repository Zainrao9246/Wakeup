package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wakeproof_prefs")

class ClockPreferences(private val context: Context) {

    private object Keys {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val DEFAULT_RE_RING_SECONDS = intPreferencesKey("default_re_ring_seconds")
        val WORLD_CLOCK_ZONES = stringSetPreferencesKey("world_clock_zones")
    }

    val use24HourFormat: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.USE_24_HOUR] ?: false
    }

    val defaultReRingSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_RE_RING_SECONDS] ?: 60
    }

    val savedWorldZones: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.WORLD_CLOCK_ZONES] ?: DEFAULT_ZONES
    }

    suspend fun setUse24HourFormat(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_24_HOUR] = enabled
        }
    }

    suspend fun setDefaultReRingSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_RE_RING_SECONDS] = seconds
        }
    }

    suspend fun addWorldZone(zoneId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WORLD_CLOCK_ZONES] ?: DEFAULT_ZONES
            prefs[Keys.WORLD_CLOCK_ZONES] = current + zoneId
        }
    }

    suspend fun removeWorldZone(zoneId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WORLD_CLOCK_ZONES] ?: DEFAULT_ZONES
            prefs[Keys.WORLD_CLOCK_ZONES] = current - zoneId
        }
    }

    companion object {
        val DEFAULT_ZONES = setOf(
            "Asia/Riyadh",
            "Europe/London",
            "America/New_York"
        )
    }
}
