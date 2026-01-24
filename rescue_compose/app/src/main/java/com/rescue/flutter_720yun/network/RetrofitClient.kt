package com.rescue.flutter_720yun.network

import android.os.Build
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object RetrofitClient {

    private const val BASE_URL = "http://pet-test.rxswift.cn"
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    // token、header 以后在这里加
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

suspend fun <T> safeApiCall(
    apiCall: suspend () -> BaseResponse<T>
): ResultWrapper<T> {
    return try {
        val response = apiCall()
        if (response.isSuccess() && response.data != null) {
            ResultWrapper.Success(response.data)
        } else {
            ResultWrapper.Error(
                code = response.code,
                message = response.message
            )
        }
    } catch (e: Exception) {
        ResultWrapper.Error(message = e.message ?: "网络异常")
    }
}

