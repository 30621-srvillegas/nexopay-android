package com.uc1.nexopay.ui

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uc1.nexopay.NexoPayApp
import com.uc1.nexopay.R
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.ui.dashboard.TransactionAdapter
import com.uc1.nexopay.ui.login.LoginActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class FrontendRegressionTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun focusedLoginFieldsRemainAboveKeyboardAndLoaderIsCentered() {
        // No cierra ni borra una sesión existente para ejecutar esta comprobación.
        val app = instrumentation.targetContext.applicationContext as NexoPayApp
        val hasSavedSession = app.container.sessionManager.refreshToken != null
        assumeTrue("Probar el login sin una sesión guardada", !hasSavedSession)

        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            for (fieldId in listOf(R.id.inputAccount, R.id.inputPassword)) {
                var keyboardHeight = 0
                scenario.onActivity { activity ->
                    val root = activity.findViewById<View>(R.id.root)
                    keyboardHeight = root.height / 2
                    activity.findViewById<View>(fieldId).requestFocus()
                    // Simula un teclado ocupando media pantalla, independientemente del IME del emulador.
                    ViewCompat.dispatchApplyWindowInsets(root, WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboardHeight))
                        .setVisible(WindowInsetsCompat.Type.ime(), true)
                        .build())
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val root = activity.findViewById<View>(R.id.root)
                    val field = activity.findViewById<View>(fieldId)
                    val rootPosition = IntArray(2).also { root.getLocationOnScreen(it) }
                    val fieldPosition = IntArray(2).also { field.getLocationOnScreen(it) }
                    assertEquals(keyboardHeight, root.paddingBottom)
                    assertTrue("El campo debe quedar encima del teclado",
                        fieldPosition[1] + field.height <= rootPosition[1] + root.height - keyboardHeight)
                    assertTrue("El campo no debe quedar fuera del borde superior",
                        fieldPosition[1] >= rootPosition[1] + root.paddingTop)
                }
            }
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(R.id.root)
                ViewCompat.dispatchApplyWindowInsets(root, WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                    .setVisible(WindowInsetsCompat.Type.ime(), false).build())
                activity.findViewById<View>(R.id.progressLogin).visibility = View.VISIBLE
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val progress = activity.findViewById<View>(R.id.progressLogin)
                val parent = progress.parent as View
                assertEquals(parent.width / 2f, progress.x + progress.width / 2f, 1f)
            }
        }
    }

    @Test
    fun newMovementRequestsRevealButEditsAndOlderMovementsDoNot() {
        val reveals = AtomicInteger()
        lateinit var adapter: TransactionAdapter
        instrumentation.runOnMainSync {
            adapter = TransactionAdapter().apply { onNewFirstItem = { reveals.incrementAndGet() } }
        }
        fun submit(rows: List<TransactionEntity>) {
            val committed = CountDownLatch(1)
            instrumentation.runOnMainSync { adapter.submitList(rows) { committed.countDown() } }
            assertTrue("La actualización de lista debe completarse", committed.await(5, TimeUnit.SECONDS))
        }
        val original = transaction("original", 2)
        submit(listOf(original))
        reveals.set(0)
        submit(listOf(original.copy(description = "Comprobante actualizado")))
        submit(listOf(original, transaction("older", 1)))
        assertEquals(0, reveals.get())
        val newest = transaction("newest", 3)
        submit(listOf(newest, original))
        assertEquals(1, reveals.get())
        assertEquals(newest.id, adapter.currentList.first().id)
        submit(listOf(newest.copy(description = "Mismo movimiento recibido por REST y WS"), original))
        assertEquals(1, reveals.get())
    }

    private fun transaction(id: String, time: Long) = TransactionEntity(
        id = id, type = "TRANSFER_RECEIVED", amountCents = 100, balanceAfterCents = 100,
        counterpartyAccountId = "10020002", contractCode = null, serviceName = null,
        description = null, referenceId = id, createdAt = time
    )
}
