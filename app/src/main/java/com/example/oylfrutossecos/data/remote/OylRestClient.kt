package com.example.oylfrutossecos.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OylRestClient {
    // Solo emulador Android: 10.0.2.2 representa el PC anfitrión.
    // En un Android físico o producción se necesita la IP/URL de un servidor real.
    val service: OylRestService by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OylRestService::class.java)
    }
}
