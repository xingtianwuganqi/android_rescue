package com.rescue.flutter_720yun.analytics

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A single asynchronous attempt; responses have no business/account side effects. */
class AnalyticsTransport(baseUrl: String, internal val client: OkHttpClient = OkHttpClient.Builder()
    .callTimeout(5,TimeUnit.SECONDS).connectTimeout(5,TimeUnit.SECONDS).readTimeout(5,TimeUnit.SECONDS)
    .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()) {
    private val url=baseUrl.trimEnd('/')+"/api/v4/analytics/events/"
    private val gson=Gson()
    fun send(envelope: AnalyticsEnvelope) {
        val body=gson.toJson(envelope.event).toRequestBody("application/json; charset=utf-8".toMediaType())
        val builder=Request.Builder().url(url).post(body)
        if(envelope.identity.authenticated) builder.header("Authorization","Bearer ${envelope.identity.token}")
        client.newCall(builder.build()).enqueue(object: Callback {
            override fun onFailure(call: Call,e: IOException) = Unit
            override fun onResponse(call: Call,response: Response) { response.close() }
        })
    }
    fun cancelAll() { client.dispatcher.cancelAll() }
}
