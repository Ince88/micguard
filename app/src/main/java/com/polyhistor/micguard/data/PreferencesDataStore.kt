package com.polyhistor.micguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "micguard_preferences")

object PreferencesDataStore {
    private var dataStore: DataStore<Preferences>? = null
    
    fun getInstance(context: Context): DataStore<Preferences> {
        return dataStore ?: synchronized(this) {
            dataStore ?: context.applicationContext.dataStore.also { dataStore = it }
        }
    }
} 