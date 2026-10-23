package com.uc1.nexopay.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uc1.nexopay.di.AppContainer
import com.uc1.nexopay.ui.dashboard.DashboardEvent
import com.uc1.nexopay.ui.dashboard.DashboardViewModel
import com.uc1.nexopay.ui.login.LoginEvent
import com.uc1.nexopay.ui.login.LoginViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelSmokeTest {

    @Test
    fun loginDashboardAndDoubleTapProtection() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer(context)
        container.sessionManager.clear()

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val loginVm = LoginViewModel(container)

        try {
            loginVm.restoreSession()
            val restored = withTimeout(10_000) {
                loginVm.state.first { !it.isRestoring }
            }
            assertNull(restored.error)

            val loginEvent = CompletableDeferred<LoginEvent>()
            val loginCollector = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                loginVm.events.collect { loginEvent.complete(it) }
            }

            loginVm.login("10020001", "Nexo#2026")
            assertTrue(withTimeout(15_000) { loginEvent.await() } is LoginEvent.Success)
            assertEquals("10020001", container.sessionManager.accountId)

            val dashboardVm = DashboardViewModel(
                repository = container.repository,
                sessionManager = container.sessionManager,
                socket = container.socket
            )

            val sessionExpired = CompletableDeferred<DashboardEvent>()
            val dashboardCollector = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                dashboardVm.events.collect {
                    if (it is DashboardEvent.SessionExpired) sessionExpired.complete(it)
                }
            }

            try {
                dashboardVm.onStart()
                val loaded = withTimeout(20_000) {
                    dashboardVm.state.first {
                        it.balanceCents == 150_000L && !it.isLoading
                    }
                }
                assertEquals("10020001", loaded.accountId)
                assertEquals("Ana Torres", loaded.owner)

                dashboardVm.deposit(100)
                dashboardVm.deposit(100)

                withTimeout(20_000) {
                    dashboardVm.state.first {
                        !it.isOperating &&
                            it.balanceCents == 150_100L &&
                            it.transactions.count { tx -> tx.type == "DEPOSIT" } == 1
                    }
                }

                dashboardVm.logout()
                withTimeout(15_000) { sessionExpired.await() }
                assertNull(container.sessionManager.session.value)
                withTimeout(10_000) {
                    dashboardVm.state.first { it.transactions.isEmpty() }
                }
            } finally {
                dashboardVm.onStop()
                dashboardCollector.cancel()
            }

            loginCollector.cancel()
        } finally {
            scope.cancel()
            container.close()
            container.sessionManager.clear()
        }
    }
}
