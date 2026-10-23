package com.uc1.nexopay.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiUtilsTest {

    @Test
    fun formatsCentsAsSoles() {
        assertEquals("S/ 1,500.00", UiUtils.formatCents(150_000))
        assertEquals("S/ 0.05", UiUtils.formatCents(5))
    }

    @Test
    fun parsesAmountsWithBothSeparatorStyles() {
        assertEquals(123_456L, UiUtils.parseAmountToCents("1,234.56"))
        assertEquals(123_456L, UiUtils.parseAmountToCents("1.234,56"))
        assertEquals(2_550L, UiUtils.parseAmountToCents("S/ 25,50"))
        assertEquals(101L, UiUtils.parseAmountToCents("1.005"))
    }

    @Test
    fun rejectsInvalidAmounts() {
        assertNull(UiUtils.parseAmountToCents(""))
        assertNull(UiUtils.parseAmountToCents("abc"))
        assertNull(UiUtils.parseAmountToCents("0"))
        assertNull(UiUtils.parseAmountToCents("-5"))
        assertNull(UiUtils.parseAmountToCents("0.001"))
    }
}
