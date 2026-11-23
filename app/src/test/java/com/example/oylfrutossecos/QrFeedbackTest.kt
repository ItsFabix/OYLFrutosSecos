package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.domain.*
import org.junit.Assert.*
import org.junit.Test

class QrFeedbackTest {
    private val product = Product(101, "Almendras", "Frutos secos", "", 2000, 250, 5, "almonds", remoteId = 8)
    private fun read(raw: String?, unavailable: Boolean = false) = qrFeedback(QrScanEvent.Read(raw), listOf(product), unavailable)!!

    @Test fun acceptedFormatsRecognizeRemoteIdAndReturnLocalNavigationId() {
        listOf("OYL:8", "oyl:id:8", "8").forEach {
            assertEquals(QrFeedbackKind.VALID, read(it).kind)
            assertEquals(101L, read(it).productId)
            assertTrue(read(it).message.contains(product.name))
        }
    }
    @Test fun unknownTextsEmptyPayloadsAndUrlsAreIncompatibleAndNotReflectedInMessages() {
        listOf(null, "", "hola", "OYL:0", "OYL:ABC", "https://unknown.test/private-token").forEach {
            val result = read(it)
            assertEquals(QrFeedbackKind.INCOMPATIBLE, result.kind)
            assertNull(result.productId)
            assertFalse(result.message.contains("private-token"))
        }
    }
    @Test fun validFormatMissingProductIsDistinctFromIncompatible() {
        assertEquals(QrFeedbackKind.NOT_FOUND, read("OYL:9").kind)
        assertEquals(QrFeedbackKind.NOT_FOUND, read("OYL:101").kind) // local ID must not collide with REST IDs
    }
    @Test fun unavailableCatalogDoesNotClaimThatProductIsMissingOrVerified() {
        assertEquals(QrFeedbackKind.CATALOG_UNAVAILABLE, read("OYL:8", true).kind)
        assertEquals(QrFeedbackKind.CATALOG_UNAVAILABLE, read("OYL:9", true).kind)
        assertEquals(QrFeedbackKind.INCOMPATIBLE, read("texto", true).kind)
    }
    @Test fun cancellationAndInitialStateHaveNoFeedback() {
        assertNull(qrFeedback(QrScanEvent.Cancelled, listOf(product), false))
        assertNull(qrFeedback(null, emptyList(), true))
    }
    @Test fun scannerFailureHasSpecificRetryableFeedback() {
        val result = qrFeedback(QrScanEvent.Failed, emptyList(), false)!!
        assertEquals(QrFeedbackKind.FAILED, result.kind)
        assertTrue(result.message.contains("Google Play"))
        assertNull(result.productId)
    }
}
