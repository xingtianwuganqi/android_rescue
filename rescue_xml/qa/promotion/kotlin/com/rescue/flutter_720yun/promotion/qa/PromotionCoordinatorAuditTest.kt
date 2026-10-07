package com.rescue.flutter_720yun.promotion.qa

import android.content.SharedPreferences
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.google.gson.Gson
import com.rescue.flutter_720yun.ads.RewardedAdEvent
import com.rescue.flutter_720yun.adoption.repository.AdoptionAccount
import com.rescue.flutter_720yun.home.models.UserInfo
import com.rescue.flutter_720yun.network.AdoptionServiceCreator
import com.rescue.flutter_720yun.network.PromotionService
import com.rescue.flutter_720yun.promotion.RewardedTopicPromotionCoordinator as Coordinator
import com.rescue.flutter_720yun.promotion.models.*
import com.rescue.flutter_720yun.promotion.repository.*
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Exercises the production singleton and HTTP transport. Android storage and SDK signals are simulated. */
class PromotionCoordinatorAuditTest {
    private lateinit var server: MockWebServer
    private lateinit var store: PendingRewardStore
    private val disk = mutableMapOf<String, String>()
    private val memory = mutableMapOf<String, String>()
    private var failCommits = 0
    private var oldUser: UserInfo? = null
    private var oldRevision: Long? = null
    private var previousStoreDelegate: Any? = null
    private var previousRepository: Any? = null
    private val state = """{"topic_id":123,"can_promote":true,"duration_seconds":3600,"remaining_today":2,"promotion":{"promotion_id":9,"is_active":true,"effective_until":"2026-10-05T08:00:00Z"},"server_time":"2026-10-05T07:00:00Z"}"""
    private val success = """{"topic_id":123,"promotion_id":9,"is_active":true,"created":false,"effective_until":"2026-10-05T08:00:00Z"}"""

    @Before fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object: TaskExecutor() {
            override fun isMainThread() = true
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
        })
        oldUser = UserManager.userInfo; oldRevision = UserManager.sessionRevision.value
        server = MockWebServer().apply { start() }
        val service = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(AdoptionServiceCreator.gson))
            .build().create(PromotionService::class.java)
        val repository = PromotionRepository(service, {
            AdoptionAccount(UserManager.userId, UserManager.token, UserManager.sessionRevision.value)
        }, { changeAccount(null, (UserManager.sessionRevision.value ?: 0) + 1) })
        val unsafe = unsafe()
        store = unsafe.javaClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, PendingRewardStore::class.java) as PendingRewardStore
        PendingRewardStore::class.java.getDeclaredField("gson").apply { isAccessible = true }.set(store, Gson())
        PendingRewardStore::class.java.getDeclaredField("prefs").apply { isAccessible = true }.set(store, prefs())
        previousStoreDelegate = field("store\$delegate").get(Coordinator)
        previousRepository = field("repository").get(Coordinator)
        replace("store\$delegate", lazyOf(store))
        replace("repository", repository)
        resetTasks()
        changeAccount(1, 100)
        Coordinator.initialize()
        Coordinator.updates.value = Coordinator.Update(1, null, "idle")
    }
    @After fun cleanup() {
        resetTasks()
        disk.clear(); memory.clear()
        changeAccount(null, 999)
        replace("store\$delegate", previousStoreDelegate)
        replace("repository", previousRepository)
        userField().set(UserManager, oldUser)
        // Avoid notifying the singleton after restoring its Android-backed store.
        androidx.lifecycle.LiveData::class.java.getDeclaredField("mData").apply { isAccessible = true }.set(UserManager.sessionRevision, oldRevision)
        server.shutdown()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
    private fun prefs(): SharedPreferences = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
        when(method.name) {
            "getString" -> synchronized(disk) { memory[args!![0]] ?: args[1] }
            "edit" -> editor()
            else -> throw UnsupportedOperationException(method.name)
        }
    } as SharedPreferences
    private fun editor(): SharedPreferences.Editor {
        val writes = mutableMapOf<String, String>()
        lateinit var editor: SharedPreferences.Editor
        editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { _, method, args ->
            when(method.name) {
                "putString" -> { writes[args!![0] as String] = args[1] as String; editor }
                "commit" -> synchronized(disk) {
                    // Android updates its in-memory preferences even when the disk commit fails.
                    memory.putAll(writes)
                    if(failCommits > 0) { failCommits--; false } else { disk.putAll(writes); true }
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as SharedPreferences.Editor
        return editor
    }
    private fun field(name: String) = Coordinator::class.java.getDeclaredField(name).apply { isAccessible = true }
    private fun unsafe() = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
    private fun replace(name: String, value: Any?) {
        val unsafe = unsafe()
        val field = field(name)
        val base = unsafe.javaClass.getMethod("staticFieldBase", java.lang.reflect.Field::class.java).invoke(unsafe, field)
        val offset = unsafe.javaClass.getMethod("staticFieldOffset", java.lang.reflect.Field::class.java).invoke(unsafe, field)
        unsafe.javaClass.getMethod("putObjectVolatile", Any::class.java, Long::class.javaPrimitiveType, Any::class.java).invoke(unsafe, base, offset, value)
    }
    private fun userField() = UserManager::class.java.getDeclaredField("_userInfo").apply { isAccessible = true }
    private fun changeAccount(id: Int?, revision: Long) {
        userField().set(UserManager, id?.let { Gson().fromJson("""{"id":$it,"token":"qa-$it"}""", UserInfo::class.java) })
        UserManager.sessionRevision.value = revision
    }
    private fun resetTasks() {
        (field("scope").get(Coordinator) as CoroutineScope).coroutineContext.cancelChildren()
        for(name in listOf("retryWakeJobs", "submissionJobs", "persistenceWakeJobs", "pendingPersistence")) (field(name).get(Coordinator) as MutableMap<*, *>).clear()
        (field("submitting").get(Coordinator) as MutableSet<*>).clear()
        field("active").set(Coordinator, null)
    }
    private fun attempt(topic: Int = 123): Coordinator.Attempt = Coordinator.Attempt(1, UserManager.sessionRevision.value, topic, "my_posts", UUID.randomUUID().toString()).also {
        field("active").set(Coordinator, it)
    }
    private fun event(attempt: Coordinator.Attempt, event: RewardedAdEvent) {
        Coordinator::class.java.getDeclaredMethod("event", Coordinator.Attempt::class.java, RewardedAdEvent::class.java)
            .apply { isAccessible = true }.invoke(Coordinator, attempt, event)
    }
    private fun reply(data: String, http: Int = 200, retryAfter: String? = null) {
        server.enqueue(MockResponse().setResponseCode(http).setBody("""{"code":$http,"message":"QA response","data":$data}""").apply {
            retryAfter?.let { setHeader("Retry-After", it) }
        })
    }
    private fun waitFor(phase: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6)
        while((Coordinator.updates.value?.phase != phase || Coordinator.busy) && System.nanoTime() < deadline) Thread.sleep(5)
        assertEquals("Coordinator state", phase, Coordinator.updates.value?.phase)
        assertFalse("Coordinator still busy", Coordinator.busy)
    }
    private fun record(next: Long = 0, until: Long = System.currentTimeMillis() + 60000) =
        PendingReward(1,123,"my_posts",UUID.randomUUID().toString(),System.currentTimeMillis(),until,next)

    @Test fun rewardSuccessPersistsBeforePostAndClearsOnlyAfterConfirmation() {
        val a = attempt()
        server.enqueue(MockResponse().setBody("""{"code":200,"data":$success}""").setBodyDelay(150, TimeUnit.MILLISECONDS))
        reply(state)
        event(a, RewardedAdEvent.Reward)
        assertEquals(a.key, store.records().single().reward_attempt_id)
        assertFalse(disk.values.single().contains("qa-1"))
        waitFor("succeeded")
        assertTrue(store.records().isEmpty())
        assertEquals(2, server.requestCount)
        val post = server.takeRequest()
        assertEquals("POST", post.method)
        assertEquals(a.key, post.getHeader("Idempotency-Key"))
        assertEquals("Bearer qa-1", post.getHeader("Authorization"))
    }
    @Test fun finishedClosedFailedAndDestroyedMustNeverInventReward() {
        val a = attempt()
        listOf(RewardedAdEvent.PlayEnded, RewardedAdEvent.Closed, RewardedAdEvent.Failed, RewardedAdEvent.Destroyed).forEach { event(a, it) }
        assertTrue(store.records().isEmpty())
        assertEquals(0, server.requestCount)
    }
    @Test fun duplicateRewardSubmitsOnlyOnce() {
        val a = attempt()
        reply(success); reply(state)
        event(a, RewardedAdEvent.Reward); event(a, RewardedAdEvent.Reward)
        waitFor("succeeded")
        assertEquals(2, server.requestCount)
        assertTrue(store.records().isEmpty())
    }
    @Test fun lateRewardAfterCloseRetainsOriginalAttempt() {
        val a = attempt()
        event(a, RewardedAdEvent.Closed)
        reply(success); reply(state)
        event(a, RewardedAdEvent.Reward)
        waitFor("succeeded")
        assertEquals(a.key, server.takeRequest().getHeader("Idempotency-Key"))
    }
    @Test fun switchedAccountNeverPostsOrDisplaysOldRewardUntilOwnerReturns() {
        val a = attempt()
        changeAccount(2, 101)
        event(a, RewardedAdEvent.Reward)
        assertEquals(0, server.requestCount)
        assertEquals(1, store.records().single().account_id)
        assertEquals(2, Coordinator.updates.value?.account)
        reply(success); reply(state)
        changeAccount(1, 102)
        waitFor("succeeded")
        val request = server.takeRequest()
        assertEquals(a.key, request.getHeader("Idempotency-Key"))
        assertEquals("Bearer qa-1", request.getHeader("Authorization"))
    }
    @Test fun rateLimitWaitsAndRetriesWithSameUuidWithoutNewVideo() {
        val a = attempt()
        reply("""{"error_code":"RATE_LIMITED","retry_after":1}""", 429, "1")
        reply(state); reply(success); reply(state)
        event(a, RewardedAdEvent.Reward)
        waitFor("retry_pending")
        Thread.sleep(100)
        assertEquals(1, server.requestCount)
        assertEquals(a.key, store.records().single().reward_attempt_id)
        waitFor("succeeded")
        val requests = List(4) { server.takeRequest() }
        assertEquals(listOf("POST", "GET", "POST", "GET"), requests.map { it.method })
        assertEquals(requests[0].getHeader("Idempotency-Key"), requests[2].getHeader("Idempotency-Key"))
        assertEquals(requests[0].body.readUtf8(), requests[2].body.readUtf8())
    }
    @Test fun restoreUsesPersistedUuidAndFreshCurrentStateInsteadOfHistoricalHour() {
        val record = record()
        store.put(record)
        reply(success)
        reply(state.replace("\"is_active\":true", "\"is_active\":false"))
        Coordinator.restore(); waitFor("succeeded")
        assertEquals(record.reward_attempt_id, server.takeRequest().getHeader("Idempotency-Key"))
        assertTrue(Coordinator.updates.value?.message!!.contains("当前推广已结束"))
    }
    @Test fun expiredRewardOnlyQueriesStatusAndNeverPosts() {
        store.put(record(until = System.currentTimeMillis() - 1))
        reply(state)
        Coordinator.restore(); waitFor("failed")
        assertEquals("expired", store.records().single().retry_state)
        assertEquals(1, server.requestCount)
        assertEquals("GET", server.takeRequest().method)
    }
    @Test fun businessRefusalMustRefreshEligibilityWithoutClaimingSuccess() {
        val a = attempt()
        reply("""{"error_code":"DAILY_LIMIT_REACHED"}""", 409)
        reply(state.replace("\"can_promote\":true", "\"can_promote\":false"))
        event(a, RewardedAdEvent.Reward); waitFor("failed")
        assertEquals("stopped", store.records().single().retry_state)
        assertEquals("A business refusal must refresh eligibility", 2, server.requestCount)
    }
    @Test fun rewardMustBeDurablySavedBeforeRestoringAfterCommitFailure() {
        val a = attempt()
        failCommits = 1
        event(a, RewardedAdEvent.Reward)
        assertEquals(0, server.requestCount)
        assertEquals(a.key, store.records().single().reward_attempt_id)
        // Storage recovers. onResume restores from memory, but must retry the disk write before POST.
        server.enqueue(MockResponse().setBody("""{"code":200,"data":$success}""").setBodyDelay(150, TimeUnit.MILLISECONDS))
        reply(state)
        Coordinator.restore()
        assertTrue("Recovered reward was submitted without retrying its failed disk commit", disk["records"]?.contains(a.key) == true)
    }
    @Test fun failedBackoffPersistenceMustNotAllowImmediateRateLimitReplay() {
        val a = attempt()
        server.enqueue(MockResponse().setResponseCode(429)
            .setHeader("Retry-After", "60")
            .setBody("""{"code":429,"data":{"error_code":"RATE_LIMITED","retry_after":60}}""")
            .setBodyDelay(150, TimeUnit.MILLISECONDS))
        reply(state); reply(success); reply(state)
        event(a, RewardedAdEvent.Reward)
        failCommits = 1 // Initial reward committed; fail the subsequent persisted retry time.
        waitFor("retry_pending")
        // A real SharedPreferences instance retains the backoff in memory, but the process
        // restart reconstructs it from the last successful disk commit.
        resetTasks()
        synchronized(disk) { memory.clear(); memory.putAll(disk) }
        Coordinator.restore()
        Thread.sleep(100)
        assertEquals("Failed disk write erased Retry-After protection", 1, server.requestCount)
    }
    @Test fun firstFailedSaveAutomaticallyRetriesWithoutAnotherReward() {
        val a = attempt()
        failCommits = 1
        reply(success); reply(state)
        event(a, RewardedAdEvent.Reward)
        assertEquals(0, server.requestCount)
        waitFor("succeeded")
        assertEquals(2, server.requestCount)
        assertEquals(a.key, server.takeRequest().getHeader("Idempotency-Key"))
    }
    @Test fun persistentStorageFailureNeverSubmitsUnsavedReward() {
        val a = attempt()
        failCommits = 100
        event(a, RewardedAdEvent.Reward)
        Coordinator.restore()
        Thread.sleep(1100)
        assertEquals("storage_pending", Coordinator.updates.value?.phase)
        assertEquals(0, server.requestCount)
        assertTrue(disk.isEmpty())
        assertEquals(a.key, store.records().single().reward_attempt_id)
    }
    @Test fun failedRetryWritesStillLeaveDurableRestartGuard() {
        val a = attempt()
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "60")
            .setBody("""{"code":429,"data":{"error_code":"RATE_LIMITED","retry_after":60}}""")
            .setBodyDelay(150, TimeUnit.MILLISECONDS))
        event(a, RewardedAdEvent.Reward)
        failCommits = 2
        waitFor("storage_pending")
        resetTasks()
        synchronized(disk) { memory.clear(); memory.putAll(disk) }
        Coordinator.restore()
        Thread.sleep(100)
        assertEquals(1, server.requestCount)
        assertTrue(store.records().single().next_retry_at > System.currentTimeMillis()+60000)
    }
    @Test fun transportFailureRestoresOriginalUuidAfterDueTime() {
        val a = attempt()
        reply("{}", 503)
        event(a, RewardedAdEvent.Reward); waitFor("retry_pending")
        val pending = store.records().single()
        assertEquals("pending", pending.retry_state)
        assertEquals(a.key, pending.reward_attempt_id)
        assertTrue(pending.next_retry_at > System.currentTimeMillis())
        resetTasks() // Simulate recreation of in-memory jobs; the committed record remains.
        store.put(pending.copy(next_retry_at = System.currentTimeMillis() - 1))
        reply(state); reply(success); reply(state)
        Coordinator.restore(); waitFor("succeeded")
        val requests = List(4) { server.takeRequest() }
        assertEquals(listOf("POST", "GET", "POST", "GET"), requests.map { it.method })
        assertEquals(a.key, requests[2].getHeader("Idempotency-Key"))
    }
    @Test fun successWithRefreshFailureClearsRewardWithoutPromisingAnotherHour() {
        val a = attempt()
        reply(success); reply("{}", 503)
        event(a, RewardedAdEvent.Reward); waitFor("succeeded")
        assertTrue(store.records().isEmpty())
        assertTrue(Coordinator.updates.value?.message!!.contains("请刷新确认"))
        assertEquals(2, server.requestCount)
    }
    @Test fun repeatedRestoreWhileSubmittingDoesNotDuplicatePost() {
        val pending = record()
        store.put(pending)
        server.enqueue(MockResponse().setBody("""{"code":200,"data":$success}""").setBodyDelay(150, TimeUnit.MILLISECONDS))
        reply(state)
        Coordinator.restore(); Coordinator.restore(); Coordinator.restore()
        waitFor("succeeded")
        assertEquals(2, server.requestCount)
    }
    @Test fun loadedCallbackFromOldAccountCannotShowVideo() {
        val a = attempt()
        changeAccount(2, 101)
        var shows = 0
        event(a, RewardedAdEvent.Loaded { shows++; true })
        assertEquals(0, shows)
        assertEquals(0, server.requestCount)
        assertEquals(2, Coordinator.updates.value?.account)
    }
}
