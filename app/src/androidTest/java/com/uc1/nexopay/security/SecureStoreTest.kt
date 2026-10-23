package com.uc1.nexopay.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureStoreTest {

    @Test
    fun encryptsAndDecrypts() {
        val store = SecureStore(ApplicationProvider.getApplicationContext())
        val key = "test_${UUID.randomUUID()}"

        store.put(key, "Nexo#2026")
        assertEquals("Nexo#2026", store.get(key))

        store.remove(key)
        assertNull(store.get(key))
    }

    @Test
    fun generatesStableBytes() {
        val store = SecureStore(ApplicationProvider.getApplicationContext())
        val key = "bytes_${UUID.randomUUID()}"

        val first = store.getOrCreateBytes(key, 32)
        val second = store.getOrCreateBytes(key, 32)

        assertEquals(32, first.size)
        assertEquals(first.toList(), second.toList())

        store.remove(key)
    }
}
