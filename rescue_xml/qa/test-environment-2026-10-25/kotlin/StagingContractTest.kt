package com.rescue.flutter_720yun.staging

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.google.gson.Gson
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.analytics.*
import com.rescue.flutter_720yun.findpet.*
import com.rescue.flutter_720yun.home.models.FeedCursor
import com.rescue.flutter_720yun.home.repository.FeedRepository
import com.rescue.flutter_720yun.network.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Explicit opt-in staging checks; no credentials, real account mutations or SDK reward submission. */
class StagingContractTest {
    private val base="http://test.rxswift.cn/"
    private val client=OkHttpClient.Builder().callTimeout(8,TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).build()
    private fun <T> service(type: Class<T>,gson: Gson)=Retrofit.Builder().baseUrl(base).client(client)
        .addConverterFactory(GsonConverterFactory.create(gson)).build().create(type)
    private fun find(identity: AdoptionAccount,invalidated: ()->Unit={})=FindPetRepository(service(FindPetService::class.java,FindPetServices.gson),{identity},invalidated)
    private val invalid=AdoptionAccount(1,"invalid-staging-test-token",0)
    @Before fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object: TaskExecutor() {
            override fun isMainThread()=true
            override fun postToMainThread(runnable: Runnable)=runnable.run()
            override fun executeOnDiskIO(runnable: Runnable)=runnable.run()
        })
    }
    @After fun cleanup() { client.dispatcher.cancelAll();ArchTaskExecutor.getInstance().setDelegate(null) }
    @Test fun publicFindListParsesWithProductionRepository() = runBlocking {
        val items=find(AdoptionAccount(null,null,0)).publicList(1)
        assertTrue(items.all { (it.findId ?: 0)>0 && it.contact==null })
    }
    @Test fun realFeedMetadataAndNextPageWorkWithProductionCursor() = runBlocking {
        val repo=FeedRepository(service(HomeService::class.java,Gson()))
        val cursor=FeedCursor()
        val first=repo.page(null,1,null);val firstItems=cursor.accept(first.meta,first.items)
        assertNotNull(cursor.snapshot)
        if(cursor.hasMore) {
            val next=repo.page(null,cursor.nextPage,cursor.snapshot)
            val nextItems=cursor.accept(next.meta,next.items)
            assertEquals(first.meta.snapshot_id,next.meta.snapshot_id)
            assertTrue(firstItems.map { it.topic_id }.toSet().intersect(nextItems.map { it.topic_id }.toSet()).isEmpty())
        }
    }
    @Test fun v3InvalidTokenIsRecognizedAsLoginExpired() = runBlocking {
        var invalidations=0
        try { find(invalid) { invalidations++ }.recommendations();fail("invalid token accepted") }
        catch(e: AdoptionError) { assertEquals(401,e.status);assertEquals("AUTH_REQUIRED",e.errorCode) }
        assertEquals(1,invalidations)
    }
    @Test fun v2InvalidTokenIsRecognizedAsLoginExpired() = runBlocking {
        var invalidations=0
        val repo=AdoptionRepository(service(AdoptionService::class.java,AdoptionServiceCreator.gson),{invalid},{invalidations++})
        try { repo.profile();fail("invalid token accepted") }
        catch(e: AdoptionError) { assertEquals(401,e.status) }
        assertEquals(1,invalidations)
    }
    @Test fun legacyInfoExpiredTokenMustTriggerLoginInsteadOfAccountRestriction() = runBlocking {
        var invalidations=0
        try { find(invalid) { invalidations++ }.info();fail("invalid token accepted") }
        catch(e: AdoptionError) {
            assertEquals("Original info business code401 must be treated as authentication failure even when HTTP403",401,e.status)
        }
        assertEquals(1,invalidations)
    }
    @Test fun actualAnalyticsTransportMustReachAcceptedEndpoint() {
        val completed=CountDownLatch(1);val http=AtomicInteger(0)
        val analyticsClient=OkHttpClient.Builder().callTimeout(5,TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .eventListener(object: EventListener() {
                override fun responseHeadersEnd(call: Call,response: Response) { http.set(response.code) }
                override fun callEnd(call: Call) { completed.countDown() }
                override fun callFailed(call: Call,ioe: java.io.IOException) { completed.countDown() }
            }).build()
        val event=AnalyticsEvent(UUID.randomUUID().toString(),"page_view",UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),1,Instant.now().toString(),"android","1.3.0","home",emptyMap())
        AnalyticsTransport(base,analyticsClient).send(AnalyticsEnvelope(event,AnalyticsIdentity(null,null)))
        assertTrue(completed.await(7,TimeUnit.SECONDS));assertEquals(200,http.get())
    }
}
