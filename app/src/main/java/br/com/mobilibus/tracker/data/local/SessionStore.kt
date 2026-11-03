package br.com.mobilibus.tracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionStore(private val context: Context) {
    private val userIdKey = longPreferencesKey("logged_user_id")
    val userId: Flow<Long?> = context.dataStore.data.map { it[userIdKey] }

    suspend fun saveUser(id: Long) = context.dataStore.edit { it[userIdKey] = id }
    suspend fun clear() = context.dataStore.edit { it.remove(userIdKey) }
}
