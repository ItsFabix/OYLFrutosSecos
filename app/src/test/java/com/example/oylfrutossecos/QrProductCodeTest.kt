package com.example.oylfrutossecos

import com.example.oylfrutossecos.domain.QrProductCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrProductCodeTest {
    @Test fun reconoceQrDeProducto() {
        assertEquals(1L, QrProductCode.parse("OYL:1"))
        assertEquals(25L, QrProductCode.parse("oyl:id:25"))
        assertEquals(8L, QrProductCode.parse("8"))
    }

    @Test fun rechazaQrDesconocidosOMalFormados() {
        assertNull(QrProductCode.parse(""))
        assertNull(QrProductCode.parse("https://otro-sitio.com"))
        assertNull(QrProductCode.parse("OYL:0"))
        assertNull(QrProductCode.parse("OYL:abc"))
        assertNull(QrProductCode.parse("OYL:999999999999999999999999"))
        assertNull(QrProductCode.parse("OYL:1;DROP TABLE products"))
    }
}
