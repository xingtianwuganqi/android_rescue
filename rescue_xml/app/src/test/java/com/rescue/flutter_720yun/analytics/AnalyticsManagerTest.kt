package com.rescue.flutter_720yun.analytics

import com.google.gson.JsonParser
import com.rescue.flutter_720yun.home.models.UserInfo
import com.rescue.flutter_720yun.util.UserManager
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Production singleton, local HTTP, and in-memory state; never calls deployed APIs. */
class AnalyticsManagerTest {
    private lateinit var server: MockWebServer
    private var oldCore: Any?=null
    private var oldTransport: Any?=null
    private var oldExposure: Any?=null
    private var oldUser: Any?=null
    private val core=AnalyticsCore(object: AnalyticsStateStore {
        var state: AnalyticsState?=null
        override fun read()=state
        override fun write(state: AnalyticsState) { this.state=state }
    })
    private fun field(name: String)=AnalyticsManager::class.java.getDeclaredField(name).apply { isAccessible=true }
    private fun user()=UserManager::class.java.getDeclaredField("_userInfo").apply { isAccessible=true }
    private fun account(id: Int?,token: String?=null) {
        user().set(UserManager,id?.let { UserInfo(it,null,null,null,null,null,null,token) })
        AnalyticsManager.identityChanged()
    }
    @Before fun setup() {
        oldCore=field("core").get(AnalyticsManager);oldTransport=field("transport").get(AnalyticsManager)
        oldExposure=field("exposure").get(AnalyticsManager);oldUser=user().get(UserManager)
        server=MockWebServer().apply { start() }
        field("core").set(AnalyticsManager,core)
        field("transport").set(AnalyticsManager,AnalyticsTransport(server.url("/").toString()))
        field("exposure").set(AnalyticsManager,AnalyticsExposure())
        account(null)
    }
    @After fun cleanup() {
        AnalyticsManager.setEnabled(false)
        field("core").set(AnalyticsManager,oldCore);field("transport").set(AnalyticsManager,oldTransport)
        field("exposure").set(AnalyticsManager,oldExposure);user().set(UserManager,oldUser)
        server.shutdown()
    }
    private fun reply() { server.enqueue(MockResponse().setBody("{\"code\":200,\"data\":{\"status\":\"accepted\"}}")) }
    private fun take()=server.takeRequest(3,TimeUnit.SECONDS)!!
    @Test fun startupIsAnonymousAndHandoffsDoNotOpenTwice() {
        reply();AnalyticsManager.foreground();AnalyticsManager.foreground()
        val request=take();assertNull(request.getHeader("Authorization"))
        assertEquals("app_open",JsonParser.parseString(request.body.readUtf8()).asJsonObject.get("event_name").asString)
        assertEquals(1,server.requestCount)
    }
    @Test fun loginKeepsAnonymousPathAndFreezesNewCredential() {
        reply();AnalyticsManager.track("login_click",interactive=true);val before=take()
        account(1,"signed-in")
        reply();AnalyticsManager.track("login_success",properties=mapOf("login_method" to "password"));val after=take()
        assertNull(before.getHeader("Authorization"));assertEquals("Bearer signed-in",after.getHeader("Authorization"))
        val a=JsonParser.parseString(before.body.readUtf8()).asJsonObject
        val b=JsonParser.parseString(after.body.readUtf8()).asJsonObject
        assertEquals(a.get("anonymous_id"),b.get("anonymous_id"));assertEquals(a.get("session_id"),b.get("session_id"))
    }
    @Test fun repeatedPageRenderIsIgnoredButReturningCounts() {
        repeat(3) { reply() }
        AnalyticsManager.showScreen("a","home");take();AnalyticsManager.showScreen("a","home")
        AnalyticsManager.showScreen("b","messages");take();AnalyticsManager.showScreen("a","home");take()
        assertEquals(3,server.requestCount)
    }
    @Test fun formExposureEmitsMatchingPageAndTypeOnlyOnce() {
        reply();reply();AnalyticsManager.showScreen("form","publish_find_pet")
        val names=List(2) { JsonParser.parseString(take().body.readUtf8()).asJsonObject }.associateBy { it.get("event_name").asString }
        assertEquals("publish_find_pet",names["publish_page_view"]!!.get("page").asString)
        assertEquals("find_pet",names["publish_page_view"]!!.getAsJsonObject("properties").get("publish_type").asString)
        AnalyticsManager.showScreen("form","publish_find_pet");assertEquals(2,server.requestCount)
    }
    @Test fun messageListTypeIsNestedEnumeratedProperty() {
        reply();AnalyticsManager.showScreen("like","message_list",mapOf("message_list_type" to "like"))
        val body=JsonParser.parseString(take().body.readUtf8()).asJsonObject
        assertEquals("message_list",body.get("page").asString)
        assertFalse(body.has("message_list_type"));assertEquals("like",body.getAsJsonObject("properties").get("message_list_type").asString)
    }
    @Test fun rewardUsesOriginalAttemptAndIgnoresDuplicateAndOtherAccount() {
        account(1,"original");val id=UUID.randomUUID().toString()
        reply();AnalyticsManager.rewardSuccess(1,123,"my_posts",id)
        val body=JsonParser.parseString(take().body.readUtf8()).asJsonObject
        assertEquals(id,body.get("event_id").asString)
        assertEquals("my_posts",body.getAsJsonObject("properties").get("ad_placement").asString)
        AnalyticsManager.rewardSuccess(1,123,"my_posts",id);account(2,"new-account")
        AnalyticsManager.rewardSuccess(1,123,"my_posts",UUID.randomUUID().toString())
        assertEquals(1,server.requestCount)
    }
    @Test fun internalDisableDoesNotBackfillEvents() {
        AnalyticsManager.setEnabled(false);AnalyticsManager.track("app_open")
        AnalyticsManager.setEnabled(true);reply();AnalyticsManager.track("page_view","home");take()
        assertEquals(1,server.requestCount)
    }
    @Test fun analytics401DoesNotChangeBusinessIdentity() {
        account(1,"current-token");val revision=UserManager.sessionRevision.value
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        AnalyticsManager.track("page_view","profile");take()
        assertEquals(1,UserManager.userId);assertEquals("current-token",UserManager.token)
        assertEquals(revision,UserManager.sessionRevision.value)
    }
}
