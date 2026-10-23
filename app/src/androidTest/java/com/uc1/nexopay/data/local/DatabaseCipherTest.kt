package com.uc1.nexopay.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uc1.nexopay.security.SecureStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseCipherTest {

    @Test
    fun opensDatabaseAndPersistsTransaction() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = DatabaseProvider.get(context, SecureStore(context))
        val dao = database.transactionDao()
        val id = UUID.randomUUID().toString()

        dao.upsert(
            TransactionEntity(
                id = id,
                type = TransactionType.DEPOSIT,
                amountCents = 1000,
                balanceAfterCents = 1000,
                counterpartyAccountId = null,
                contractCode = null,
                serviceName = null,
                description = "Prueba de cifrado Fase C",
                referenceId = id,
                createdAt = System.currentTimeMillis()
            )
        )

        assertTrue(dao.getRecent().any { it.id == id })
    }
}
