package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AdoptionServiceCreator {
    val gson = com.google.gson.GsonBuilder().registerTypeAdapterFactory(V2EnvelopeAdapter()).registerTypeAdapter(
        com.rescue.flutter_720yun.adoption.models.TopicSummary::class.java,
        com.rescue.flutter_720yun.adoption.models.PreviewImageAdapter()).create()
    val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(BuildConfig.ADOPTION_BASE_URL.trimEnd('/') + "/")
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
    }
    val service: AdoptionService by lazy { retrofit.create(AdoptionService::class.java) }
}
