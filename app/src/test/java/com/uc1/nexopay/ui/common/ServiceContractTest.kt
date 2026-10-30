package com.uc1.nexopay.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceContractTest {
    @Test
    fun buildsCodesForEverySupportedServiceWithoutDroppingLeadingZeros() {
        for (prefix in listOf("LUZ", "AGUA", "NET", "GAS")) {
            assertEquals("$prefix-001", ServiceContract.build(prefix, "001"))
        }
        assertEquals("NET-001234", ServiceContract.build("NET", " 001234 "))
    }

    @Test
    fun acceptsBackendLengthBoundariesAndOtherContractNumbers() {
        assertEquals("AGUA-987", ServiceContract.build("AGUA", "987"))
        assertEquals("GAS-123456789012", ServiceContract.build("GAS", "123456789012"))
        assertNull(ServiceContract.build("LUZ", "12"))
        assertNull(ServiceContract.build("LUZ", "1234567890123"))
    }

    @Test
    fun rejectsMissingServiceUnknownPrefixesAndInvalidContractCharacters() {
        assertNull(ServiceContract.build(null, "001"))
        assertNull(ServiceContract.build("INTERNET", "001"))
        for (number in listOf("", "ABC123", "abc", "LUZ-001", "12 34", "12/34", "ábc", "001\n002", "１２３", "١٢٣", "+123", "12.3")) {
            assertNull(ServiceContract.build("LUZ", number))
        }
    }
}
