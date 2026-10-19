package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.repository.DemoPickupPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoPickupPointsTest {
    @Test fun listHasTwoUniqueDemoLocations() {
        val all = DemoPickupPoints.all
        assertEquals(2, all.size)
        assertEquals(2, all.map { it.id }.distinct().size)
        assertTrue(all.all { it.title.contains("demo") })
    }

    @Test fun urlsKeepSafeCoordinatesAndHttps() {
        val point = DemoPickupPoints.all.first()
        val embed = DemoPickupPoints.embedUrl(point)
        assertTrue(embed.startsWith("https://www.openstreetmap.org/export/embed.html?"))
        assertTrue(embed.contains("marker=-33.43720%2C-70.65060"))
        assertTrue(DemoPickupPoints.directionsUrl(point).startsWith("https://www.google.com/maps/dir/"))
    }
}
