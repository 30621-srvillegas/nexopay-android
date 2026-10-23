package com.uc1.nexopay.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uc1.nexopay.data.remote.dto.LoginRequest
import com.uc1.nexopay.data.remote.dto.RefreshRequest
import com.uc1.nexopay.security.SecureStore
import com.uc1.nexopay.security.SessionManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkSmokeTest {

    @Test
    fun loginAndAuthenticatedCallsWork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val session = SessionManager(SecureStore(context)).apply { clear() }
        val api = ApiFactory.create(session) {}

        val login = api.login(LoginRequest("10020001", "Nexo#2026"))
        assertTrue("login HTTP ${login.code()}", login.isSuccessful)

        val body = login.body()!!
        assertTrue(body.accessToken.isNotBlank())
        assertTrue(body.refreshToken.isNotBlank())
        assertEquals("10020001", body.account.id)

        session.save(
            accountId = body.account.id,
            name = body.account.owner,
            accessToken = body.accessToken,
            refreshToken = body.refreshToken
        )

        val me = api.me()
        assertTrue("me HTTP ${me.code()}", me.isSuccessful)
        assertEquals("10020001", me.body()?.id)

        val balance = api.balance()
        assertTrue("balance HTTP ${balance.code()}", balance.isSuccessful)
        assertTrue(balance.body()!!.balanceCents >= 0)

        runCatching { api.logout(RefreshRequest(body.refreshToken)) }
        session.clear()
    }

    @Test
    fun authenticatorRefreshesInvalidAccessTokenOnce() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val session = SessionManager(SecureStore(context)).apply { clear() }
        val expired = AtomicBoolean(false)
        val api = ApiFactory.create(session) { expired.set(true) }

        val login = api.login(LoginRequest("10020002", "Nexo#2026"))
        assertTrue("login HTTP ${login.code()}", login.isSuccessful)

        val body = login.body()!!
        session.save(
            accountId = body.account.id,
            name = body.account.owner,
            accessToken = "token-invalido",
            refreshToken = body.refreshToken
        )

        val balance = api.balance()
        assertTrue("balance HTTP ${balance.code()}", balance.isSuccessful)
        assertNotEquals("token-invalido", session.accessToken)
        assertFalse(expired.get())

        val rotated = session.refreshToken
        assertNotNull(rotated)
        assertNotEquals(body.refreshToken, rotated)

        runCatching { api.logout(RefreshRequest(rotated!!)) }
        session.clear()
    }

    @Test
    fun invalidRefreshClearsSessionWithoutLoop() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val session = SessionManager(SecureStore(context)).apply { clear() }
        val expired = AtomicBoolean(false)
        val api = ApiFactory.create(session) { expired.set(true) }

        session.save(
            accountId = "10020001",
            name = "Sesión inválida",
            accessToken = "token-invalido",
            refreshToken = "refresh-invalido-0123456789"
        )

        val balance = api.balance()
        assertEquals(401, balance.code())
        assertTrue(expired.get())
        assertNull(session.session.value)
        assertNull(session.refreshToken)
    }
}
