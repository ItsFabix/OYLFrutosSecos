package com.example.oylfrutossecos.domain

/** Códigos WMO publicados por Open-Meteo: https://open-meteo.com/en/docs */
object WeatherConditions {
    fun description(code: Int): String = when (code) {
        0 -> "Despejado"
        1 -> "Mayormente despejado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45 -> "Niebla"
        48 -> "Niebla con escarcha"
        51 -> "Llovizna ligera"
        53 -> "Llovizna moderada"
        55 -> "Llovizna intensa"
        56 -> "Llovizna helada ligera"
        57 -> "Llovizna helada intensa"
        61 -> "Lluvia ligera"
        63 -> "Lluvia moderada"
        65 -> "Lluvia intensa"
        66 -> "Lluvia helada ligera"
        67 -> "Lluvia helada intensa"
        71 -> "Nieve ligera"
        73 -> "Nieve moderada"
        75 -> "Nieve intensa"
        77 -> "Nieve granulada"
        80 -> "Chubascos ligeros"
        81 -> "Chubascos moderados"
        82 -> "Chubascos intensos"
        85 -> "Chubascos de nieve ligeros"
        86 -> "Chubascos de nieve intensos"
        95 -> "Tormenta eléctrica"
        96 -> "Tormenta con granizo ligero"
        97 -> "Tormenta eléctrica intensa"
        99 -> "Tormenta con granizo intenso"
        else -> "Condición no disponible"
    }
}
