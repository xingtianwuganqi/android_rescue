package com.rescue.flutter_720yun.analytics

import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.io.IOException

class AnalyticsTransportTest {
    private fun envelope(token: String?=null)=AnalyticsEnvelope(AnalyticsEvent(UUID.randomUUID().toString(),"page_view",
        UUID.randomUUID().toString(),UUID.randomUUID().toString(),1,"2026-10-22T10:00:00+08:00","android","1.3.0","home",emptyMap()),
        AnalyticsIdentity(if(token==null) null else 1,token))
    @Test fun directJsonUsesFrozenBearerAndNoBodyCredentials() {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("{}"));server.enqueue(MockResponse().setBody("{}"))
            val transport=AnalyticsTransport(server.url("/").toString())
            val anonymous=envelope();val signed=envelope("old-token")
            transport.send(anonymous);val a=server.takeRequest(3,TimeUnit.SECONDS)!!
            transport.send(signed);val b=server.takeRequest(3,TimeUnit.SECONDS)!!
            assertNull(a.getHeader("Authorization"));assertEquals("Bearer old-token",b.getHeader("Authorization"))
            assertEquals("POST",b.method);assertEquals("/api/v4/analytics/events/",b.path)
            assertTrue(b.getHeader("Content-Type")!!.startsWith("application/json"))
            val body=JsonParser.parseString(b.body.readUtf8()).asJsonObject
            assertEquals(signed.event.event_id,body.get("event_id").asString)
            for(key in listOf("token","user_id","events","source","ip")) assertFalse(body.has(key))
            assertEquals(2,server.requestCount)
        } finally { server.shutdown() }
    }
    @Test fun errorsAndDuplicateEndOnceWithoutRetryOrAccountSideEffects() {
        val server=MockWebServer();server.start()
        val statuses=listOf(200,400,401,403,413,415,429,503)
        val ended=CountDownLatch(statuses.size)
        val observer=object: EventListener() { override fun callEnd(call: Call) { ended.countDown() } }
        val client=OkHttpClient.Builder().retryOnConnectionFailure(false).eventListener(observer).build()
        try {
            val transport=AnalyticsTransport(server.url("/").toString(),client)
            statuses.forEach { status ->
                server.enqueue(MockResponse().setResponseCode(status).setHeader("Retry-After","1").setBody("{\"code\":$status,\"data\":{\"status\":\"duplicate\"}}"))
                transport.send(envelope("snapshot"))
            }
            assertTrue(ended.await(5,TimeUnit.SECONDS));assertEquals(statuses.size,server.requestCount)
            assertEquals(0,client.dispatcher.runningCallsCount())
        } finally { client.dispatcher.cancelAll();server.shutdown() }
    }
    @Test fun defaultsHaveFiveSecondDeadlineNoAuthenticatorAndNoRedirectRetry() {
        val t=AnalyticsTransport("http://localhost/")
        assertEquals(5000,t.client.callTimeoutMillis);assertFalse(t.client.retryOnConnectionFailure)
        assertEquals(Authenticator.NONE,t.client.authenticator)
        assertTrue(t.client.interceptors.isEmpty());assertFalse(t.client.followRedirects)
    }
    @Test fun disablingCanCancelInflightCallWithoutRetry() {
        val server=MockWebServer();server.start()
        val failed=CountDownLatch(1)
        val client=OkHttpClient.Builder().retryOnConnectionFailure(false).eventListener(object: EventListener() {
            override fun callFailed(call: Call,ioe: IOException) { failed.countDown() }
        }).build()
        try {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val t=AnalyticsTransport(server.url("/").toString(),client);t.send(envelope())
            assertNotNull(server.takeRequest(3,TimeUnit.SECONDS));t.cancelAll()
            assertTrue(failed.await(3,TimeUnit.SECONDS));assertEquals(1,server.requestCount)
        } finally { client.dispatcher.cancelAll();server.shutdown() }
    }
}
