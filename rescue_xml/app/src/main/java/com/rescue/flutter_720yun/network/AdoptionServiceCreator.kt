package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AdoptionServiceCreator {
    val service: AdoptionService by lazy {
        Retrofit.Builder().baseUrl(BuildConfig.ADOPTION_BASE_URL.trimEnd('/') + "/")
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(AdoptionService::class.java)
    }
}
