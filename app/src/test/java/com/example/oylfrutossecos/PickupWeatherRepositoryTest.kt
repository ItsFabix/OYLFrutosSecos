package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.remote.OpenMeteoService
import com.example.oylfrutossecos.data.repository.DemoPickupPoints
import com.example.oylfrutossecos.data.repository.PickupWeatherRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PickupWeatherRepositoryTest {
    private val server = MockWebServer()
    private var now = 0L
    private lateinit var repository: PickupWeatherRepository
    private val point = DemoPickupPoints.all.first()
    private val body = """{"current":{"time":"2026-11-23T11:30","temperature_2m":21.7,"weather_code":2}}"""

    @Before fun start() {
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(OpenMeteoService::class.java)
        repository = PickupWeatherRepository(api) { now }
    }
    @After fun close() { server.shutdown() }
    private fun response(json: String = body) = MockResponse().setHeader("Content-Type", "application/json").setBody(json)

    @Test fun retrofitRequestsExistingCoordinatesCurrentVariablesAndCelsiusWithoutAdminCredential() = runBlocking {
        server.enqueue(response())
        val weather = repository.current(point)
        assertEquals(21.7, weather.temperatureCelsius, 0.001)
        assertEquals(2, weather.weatherCode)
        assertEquals(11, weather.observedAt.hour)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/v1/forecast", request.requestUrl!!.encodedPath())
        assertEquals(point.latitude.toString(), request.requestUrl!!.queryParameter("latitude"))
        assertEquals(point.longitude.toString(), request.requestUrl!!.queryParameter("longitude"))
        assertEquals("temperature_2m,weather_code", request.requestUrl!!.queryParameter("current"))
        assertEquals("celsius", request.requestUrl!!.queryParameter("temperature_unit"))
        assertEquals("auto", request.requestUrl!!.queryParameter("timezone"))
        assertNull(request.getHeader("X-OYL-ADMIN-KEY"))
    }

    @Test fun sameCoordinatesReuseCacheUntilTenMinutesThenFetchAgain() = runBlocking {
        server.enqueue(response())
        val first = repository.current(point)
        now = PickupWeatherRepository.CACHE_MILLIS - 1
        assertEquals(first, repository.current(point))
        assertEquals(1, server.requestCount)
        now++
        server.enqueue(response(body.replace("21.7", "23.0")))
        assertEquals(23.0, repository.current(point).temperatureCelsius, 0.001)
        assertEquals(2, server.requestCount)
    }

    @Test fun cacheIsSeparatedByPointCoordinates() = runBlocking {
        server.enqueue(response())
        repository.current(point)
        val other = DemoPickupPoints.all.last()
        server.enqueue(response(body.replace("21.7", "19.5")))
        assertEquals(19.5, repository.current(other).temperatureCelsius, 0.001)
        assertEquals(21.7, repository.current(point).temperatureCelsius, 0.001)
        assertEquals(2, server.requestCount)
        server.takeRequest()
        assertEquals(other.latitude.toString(), server.takeRequest().requestUrl!!.queryParameter("latitude"))
    }

    @Test fun httpFailureIsNotCachedAndCanBeRetried() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        try { repository.current(point); fail("Debe informar el fallo HTTP") }
        catch (expected: HttpException) { assertEquals(503, expected.code()) }
        server.enqueue(response())
        assertEquals(21.7, repository.current(point).temperatureCelsius, 0.001)
        assertEquals(2, server.requestCount)
    }

    @Test fun incompleteResponsesNeverBecomeZeroDegreesOrClearSky() = runBlocking {
        for (invalid in listOf("{}", """{"current":{"time":"2026-11-23T11:30","weather_code":0}}""",
            """{"current":{"time":"2026-11-23T11:30","temperature_2m":20}}""")) {
            server.enqueue(response(invalid))
            try { repository.current(point); fail("No debe mostrar datos incompletos") }
            catch (_: IllegalStateException) { }
        }
        server.enqueue(response())
        assertEquals(2, repository.current(point).weatherCode)
        assertEquals(4, server.requestCount)
    }
}
