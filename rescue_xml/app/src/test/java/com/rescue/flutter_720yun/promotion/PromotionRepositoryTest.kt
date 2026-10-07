package com.rescue.flutter_720yun.promotion

import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.network.*
import com.rescue.flutter_720yun.promotion.models.*
import com.rescue.flutter_720yun.promotion.repository.PromotionRepository
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class PromotionRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repo: PromotionRepository
    private val account=AtomicReference(AdoptionAccount(1,"one",1L))
    private var logouts=0
    private val pending=PendingReward(1,123,"my_posts","0835f97e-8e42-4cb0-92ce-9b587796dd90",100,100000)
    @Before fun setup() {
        server=MockWebServer();server.start()
        val service=Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(AdoptionServiceCreator.gson)).build().create(PromotionService::class.java)
        repo=PromotionRepository(service,{ account.get() },{ logouts++ })
    }
    @After fun close() { server.shutdown() }
    private suspend fun error(action: suspend ()->Unit): AdoptionError {
        try { action() } catch(e: AdoptionError) { return e };throw AssertionError("Expected refusal")
    }
    private fun reply(http: Int,code: Int=http,data: String="{}",headers: Map<String,String> = emptyMap()) {
        server.enqueue(MockResponse().setResponseCode(http).setBody("""{"code":$code,"message":"响应","data":$data}""").apply {
            headers.forEach { (key,value) -> setHeader(key,value) }
        })
    }
    @Test fun retryUsesSameUuidTargetSceneAndJsonOnly()=runBlocking {
        reply(503,data="""{"error_code":"RETRY_LATER"}""");error { repo.submit(pending) }
        val first=server.takeRequest()
        reply(200,data="""{"topic_id":123,"promotion_id":9,"is_active":true,"created":false}""")
        assertEquals(9,repo.submit(pending).promotion_id)
        val second=server.takeRequest()
        assertEquals("Bearer one",second.getHeader("Authorization"))
        assertEquals(pending.reward_attempt_id,second.getHeader("Idempotency-Key"))
        assertTrue(second.getHeader("Content-Type")!!.startsWith("application/json"))
        val body=first.body.readUtf8()
        assertEquals(body,second.body.readUtf8())
        assertEquals("""{"reward_attempt_id":"${pending.reward_attempt_id}","scene":"my_posts"}""",body)
        assertEquals("/api/v2/topics/123/promotion",second.path)
    }
    @Test fun http200BusinessRefusalRetainsErrorFieldsAndActualHttp()=runBlocking {
        reply(200,409,"""{"error_code":"DAILY_LIMIT_REACHED","errors":{"scene":["错误"]}}""")
        val failure=error { repo.submit(pending) }
        assertEquals(200,failure.http);assertEquals(409,failure.code)
        assertEquals("DAILY_LIMIT_REACHED",failure.errorCode);assertEquals("错误",failure.fields["scene"])
        assertEquals(0,logouts)
    }
    @Test fun rateLimitKeepsRetryAfterAndPermissionNeverLogsOut()=runBlocking {
        reply(429,data="""{"error_code":"RATE_LIMITED","retry_after":8}""",headers=mapOf("Retry-After" to "12"))
        assertEquals(12L,error { repo.submit(pending) }.retryAfter)
        reply(403,data="""{"error_code":"NOT_OWNER"}""")
        assertEquals("NOT_OWNER",error { repo.submit(pending) }.errorCode);assertEquals(0,logouts)
    }
    @Test fun oldAccountUnauthorizedCannotLogOutNewAccount()=runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":401,"data":{"error_code":"AUTH_REQUIRED"}}""")
            .setBodyDelay(200,TimeUnit.MILLISECONDS))
        val request=async(Dispatchers.IO) { repo.state(123) }
        assertNotNull(server.takeRequest(2,TimeUnit.SECONDS));account.set(AdoptionAccount(2,"two",2L))
        try { request.await();fail("Stale request accepted") } catch(e: CancellationException) { }
        assertEquals(0,logouts)
    }
    @Test fun restoreRejectsAnotherAccountBeforeMakingRequest()=runBlocking {
        account.set(AdoptionAccount(2,"two",2L))
        try { repo.submit(pending);fail("Wrong account") } catch(e: IllegalArgumentException) { }
        assertEquals(0,server.requestCount)
    }
    @Test fun successfulEnvelopeWithMissingTargetCannotClearReward()=runBlocking {
        reply(200)
        assertEquals(502,error { repo.submit(pending) }.http)
    }
    @Test fun malformedSuccessIsNotAnAward()=runBlocking {
        server.enqueue(MockResponse().setBody("<html>proxy</html>"))
        assertEquals(502,error { repo.state(123) }.http)
    }
}
