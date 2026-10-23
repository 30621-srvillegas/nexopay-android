package com.uc1.nexopay.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uc1.nexopay.di.AppContainer
import com.uc1.nexopay.ui.dashboard.DashboardViewModel
import com.uc1.nexopay.ui.login.LoginViewModel

class NexoViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(container)

            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    repository = container.repository,
                    sessionManager = container.sessionManager,
                    socket = container.socket
                )

            else -> error("ViewModel no soportado: ${modelClass.name}")
        }
        return viewModel as T
    }
}
