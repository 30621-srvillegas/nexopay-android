package com.uc1.nexopay.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsReplacesAndOrders() = runBlocking {
        val older = transaction(createdAt = 1_000L, amountCents = 1_000L)
        val newer = transaction(createdAt = 2_000L, amountCents = 2_000L)

        dao.upsertAll(listOf(older, newer))
        assertEquals(2, dao.count())

        val recent = dao.observeRecent().first()
        assertEquals(newer.id, recent.first().id)

        dao.upsert(older.copy(amountCents = 9_999L))
        assertEquals(2, dao.count())
        assertEquals(9_999L, dao.getRecent().last().amountCents)
    }

    @Test
    fun deletesAll() = runBlocking {
        dao.upsert(transaction(createdAt = 1_000L, amountCents = 1_000L))
        dao.deleteAll()
        assertEquals(0, dao.count())
    }

    private fun transaction(createdAt: Long, amountCents: Long) =
        TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = TransactionType.DEPOSIT,
            amountCents = amountCents,
            balanceAfterCents = amountCents,
            counterpartyAccountId = null,
            contractCode = null,
            serviceName = null,
            description = "Prueba",
            referenceId = null,
            createdAt = createdAt
        )
}
