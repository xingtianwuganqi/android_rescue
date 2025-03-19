package com.rescue.flutter_720yun.network
import android.util.Log
import com.rescue.flutter_720yun.ActivityController
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.LoginEvent
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.toastString
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.greenrobot.eventbus.EventBus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ServiceCreator {
    private const val BASE_URL = "http://test.rxswift.cn/"

    // 创建 Interceptor
    private val headerInterceptor = Interceptor { chain ->
        val request: Request = chain.request()
            .newBuilder()
//            .addHeader("Authorization", "Bearer your_token")  // 添加公共请求头
            .addHeader("Content-Type", "application/x-www-form-urlencoded")    // 添加Content-Type
            .build()
        chain.proceed(request)
    }

    // 创建 OkHttpClient 并添加 Interceptor
    private val okHttpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)// 完整请求超时市场，从发起到接收返回数据， 默认值0，不限定
        .connectTimeout(30, TimeUnit.SECONDS)// 与服务器建立连接时长，默认10s
        .readTimeout(30, TimeUnit.SECONDS)// 读取服务器返回的时长
        .writeTimeout(30, TimeUnit.SECONDS)// 向服务器写入数据的时长，默认10s
        .retryOnConnectionFailure(true)
        .followRedirects(false)
        .addInterceptor(headerInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
//        .client(okHttpClient)
        .build()

    fun <T> create(serviceClass: Class<T>): T = retrofit.create(serviceClass)

    inline fun <reified T> create(): T = create(T::class.java)

}

@OptIn(DelicateCoroutinesApi::class)
suspend fun <T> Call<T>.awaitResp(): T {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    Log.d("TAG", "response body ${response.body()}")
                    continuation.resume(response.body()!!)
                } else {
                    if (response.code() == 401 || response.code() == 403) {
                        BaseApplication.context.resources.getString(R.string.login_token_lose).toastString()
                        UserManager.logout()
                        EventBus.getDefault().post(LoginEvent(null))
                        GlobalScope.launch {
                            delay(1500)
                            ActivityController.finishToLast()
                        }

                    }else {
                        Log.d("TAG","response error Code ${response.code()}")
                        Log.d("TAG", "response error ${response.body()}")
                        continuation.resumeWithException(Exception("Response error"))
                    }
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }
}