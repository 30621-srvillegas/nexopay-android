package com.uc1.nexopay.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nexo_settings"
)

class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    val lastAccount: Flow<String?> = store.data.map { it[LAST_ACCOUNT] }
    val notificationsEnabled: Flow<Boolean> = store.data.map {
        it[NOTIFICATIONS] ?: true
    }

    suspend fun setLastAccount(accountId: String) {
        store.edit { it[LAST_ACCOUNT] = accountId }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.edit { it[NOTIFICATIONS] = enabled }
    }

    suspend fun clearLastAccount() {
        store.edit { it.remove(LAST_ACCOUNT) }
    }

    private companion object {
        val LAST_ACCOUNT = stringPreferencesKey("last_account")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }
}
