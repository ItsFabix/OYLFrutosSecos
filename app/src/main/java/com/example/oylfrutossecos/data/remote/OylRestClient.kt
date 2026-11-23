package com.example.oylfrutossecos.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.oylfrutossecos.BuildConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OylRestClient {
    // Solo emulador Android: 10.0.2.2 representa el PC anfitrión.
    // En un Android físico o producción se necesita la IP/URL de un servidor real.
    val service: OylRestService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.OYL_API_BASE_URL)
            .client(OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OylRestService::class.java)
    }
}
