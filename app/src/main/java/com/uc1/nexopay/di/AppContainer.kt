package com.uc1.nexopay.di

import android.content.Context
import com.uc1.nexopay.data.local.AppDatabase
import com.uc1.nexopay.data.local.DatabaseProvider
import com.uc1.nexopay.data.remote.ApiFactory
import com.uc1.nexopay.data.remote.NexoApi
import com.uc1.nexopay.data.remote.WalletSocketManager
import com.uc1.nexopay.data.repository.WalletRepository
import com.uc1.nexopay.security.SecureStore
import com.uc1.nexopay.security.SessionManager
import com.uc1.nexopay.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val secureStore: SecureStore = SecureStore(appContext)
    val sessionManager: SessionManager = SessionManager(secureStore)
    val settingsStore: SettingsStore = SettingsStore(appContext)

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        DatabaseProvider.get(appContext, secureStore)
    }

    val api: NexoApi by lazy {
        ApiFactory.create(sessionManager) {
            sessionManager.clear()
        }
    }

    val repository: WalletRepository by lazy {
        WalletRepository(api, database, sessionManager)
    }

    val socket: WalletSocketManager by lazy {
        WalletSocketManager(repository, sessionManager, applicationScope)
    }

    fun close() {
        socket.disconnect()
        applicationScope.coroutineContext[Job]?.cancel()
    }
}
