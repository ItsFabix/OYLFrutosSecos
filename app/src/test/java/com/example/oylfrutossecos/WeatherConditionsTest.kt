package com.example.oylfrutossecos

import com.example.oylfrutossecos.domain.WeatherConditions
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Verify the actual Spanish label displayed for each supported WMO condition, including unknown codes. */
@RunWith(Parameterized::class)
class WeatherConditionsTest(private val code: Int, private val expected: String) {
    @Test fun mapsConditionToSpanishLabel() { assertEquals(expected, WeatherConditions.description(code)) }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "WMO {0}: {1}")
        fun conditions(): List<Array<Any>> = listOf(
            0 to "Despejado", 1 to "Mayormente despejado", 2 to "Parcialmente nublado", 3 to "Nublado",
            45 to "Niebla", 48 to "Niebla con escarcha", 51 to "Llovizna ligera", 53 to "Llovizna moderada",
            55 to "Llovizna intensa", 56 to "Llovizna helada ligera", 57 to "Llovizna helada intensa",
            61 to "Lluvia ligera", 63 to "Lluvia moderada", 65 to "Lluvia intensa", 66 to "Lluvia helada ligera",
            67 to "Lluvia helada intensa", 71 to "Nieve ligera", 73 to "Nieve moderada", 75 to "Nieve intensa",
            77 to "Nieve granulada", 80 to "Chubascos ligeros", 81 to "Chubascos moderados", 82 to "Chubascos intensos",
            85 to "Chubascos de nieve ligeros", 86 to "Chubascos de nieve intensos", 95 to "Tormenta eléctrica",
            96 to "Tormenta con granizo ligero", 97 to "Tormenta eléctrica intensa", 99 to "Tormenta con granizo intenso",
            -1 to "Condición no disponible", 100 to "Condición no disponible"
        ).map { (code, label) -> arrayOf<Any>(code, label) }
    }
}
