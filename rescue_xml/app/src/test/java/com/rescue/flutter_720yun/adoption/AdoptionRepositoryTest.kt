package com.rescue.flutter_720yun.adoption

import com.google.gson.JsonParser
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.network.AdoptionService
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AdoptionRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repo: AdoptionRepository
    private val identity = AtomicReference(AdoptionAccount(1, "account-one", 1L))
    private var logouts = 0
    private lateinit var failedCall: CountDownLatch
    @Before fun setup() {
        server=MockWebServer(); server.start(); failedCall=CountDownLatch(1)
        val client=OkHttpClient.Builder().eventListener(object: EventListener() {
            override fun callFailed(call: Call, ioe: IOException) { failedCall.countDown() }
        }).build()
        val service=Retrofit.Builder().baseUrl(server.url("/")).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(AdoptionService::class.java)
        repo=AdoptionRepository(service, { identity.get() }, { logouts++ })
    }
    @After fun close() { server.shutdown() }
    private fun reply(data: String, http: Int=200, code: Int=http) {
        server.enqueue(MockResponse().setResponseCode(http).setHeader("Content-Type","application/json")
            .setBody("""{"code":$code,"message":"响应","data":$data}"""))
    }
    private suspend fun error(action: suspend ()->Unit): AdoptionError {
        try { action() } catch(e: AdoptionError) { return e }; throw AssertionError("Expected failure")
    }
    @Test fun missingProfileIsNormalAndBearerIsSeparate()=runBlocking {
        reply("""{"profile":null,"is_complete":false,"missing_fields":["age"],"version":null}""")
        val data=repo.profile(); assertNull(data.profile); assertFalse(data.is_complete)
        val request=server.takeRequest(); assertEquals("/api/v2/adoption-profile/me",request.path)
        assertEquals("Bearer account-one",request.getHeader("Authorization")); assertEquals(0,request.bodySize)
    }
    @Test fun profileWritesNumericAgeAndOnlyExistingVersion()=runBlocking {
        val body=ProfileWrite(28,"上海市","浦东新区","owned","employed","",null)
        repeat(2) { i ->
            reply("""{"profile":null,"is_complete":false,"version":${i+1}}""")
            repo.saveProfile(body.copy(version=if(i==0) null else 1))
            val request=server.takeRequest(); assertEquals("PUT",request.method)
            val json=JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertTrue(json["age"].asJsonPrimitive.isNumber); assertEquals(28,json["age"].asInt)
            assertFalse(json.has("token")); assertFalse(json.has("user_id"))
            if(i==0) assertFalse(json.has("version")) else assertEquals(1,json["version"].asInt)
            assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        }
    }
    @Test fun deniedPermissionDoesNotLogoutButUnauthorizedDoes()=runBlocking {
        reply("""{"error_code":"FORBIDDEN"}""",403)
        assertEquals(403,error { repo.profile() }.http); assertEquals(0,logouts)
        reply("{}",401); assertEquals(401,error { repo.profile() }.http); assertEquals(1,logouts)
    }
    @Test fun errorsRetainFieldsConflictAndHtml404()=runBlocking {
        reply("""{"error_code":"PROFILE_CHANGED","errors":{"age":["年龄错误"]}}""",409)
        val e=error { repo.profile() }; assertEquals("PROFILE_CHANGED",e.errorCode)
        assertEquals("年龄错误",e.fields["age"])
        server.enqueue(MockResponse().setResponseCode(404).setBody("<html>not deployed</html>"))
        assertEquals(404,error { repo.profile() }.http)
    }
    @Test fun staleAccountResponseCannotPopulateNewAccount()=runBlocking {
        server.enqueue(MockResponse().setBody("""{"code":200,"data":{"profile":null,"is_complete":false}}""").setBodyDelay(150,TimeUnit.MILLISECONDS))
        val pending=async(Dispatchers.IO) { repo.profile() }
        assertNotNull(server.takeRequest(2,TimeUnit.SECONDS)); identity.set(AdoptionAccount(2,"account-two",2L))
        try { pending.await(); fail("Old response accepted") } catch(e: CancellationException) { }
        assertEquals(0,logouts)
    }
    @Test fun coroutineCancellationCancelsHttpCall()=runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val pending=async(Dispatchers.IO) { repo.profile() }
        assertNotNull(server.takeRequest(2,TimeUnit.SECONDS)); pending.cancelAndJoin()
        assertTrue("Retrofit must cancel the underlying OkHttp call",failedCall.await(2,TimeUnit.SECONDS))
    }
    @Test fun retryUsesIdenticalIdempotencyHeaderAndBody()=runBlocking {
        val body=ApplicationWrite(4,"想了解这只猫")
        reply("{}",503); error { repo.apply(123,"same-request-key",body) }
        val first=server.takeRequest()
        reply("""{"application_id":501,"status":"applying","version":1}""")
        assertEquals(501,repo.apply(123,"same-request-key",body).application_id)
        val second=server.takeRequest(); assertEquals("same-request-key",second.getHeader("Idempotency-Key"))
        assertEquals(first.getHeader("Idempotency-Key"),second.getHeader("Idempotency-Key"))
        assertEquals(first.body.readUtf8(),second.body.readUtf8())
    }
    @Test fun ownerListKeepsFilterAndStringPreviewAndNullSnapshot()=runBlocking {
        reply("""{"items":[{"application_id":501,"version":1,"profile_snapshot":null,"applicant":{"user_id":88,"nickname":"小林","avatar":"https://img.test/a.png"},"topic":{"topic_id":123,"preview_img":"cat.png","is_delete":1}}],"page":2,"size":10,"total":11,"has_more":false}""")
        val data=repo.received(123,"ended",2); assertFalse(data.has_more); assertNull(data.items!![0].profile_snapshot)
        assertEquals("cat.png",data.items!![0].topic!!.preview_img)
        assertEquals("小林",data.items!![0].applicant!!.nickname)
        val url=server.takeRequest().requestUrl!!; assertEquals("ended",url.queryParameter("status"))
        assertEquals("2",url.queryParameter("page")); assertEquals("123",url.queryParameter("topic_id"))
    }
    @Test fun applicationContactComesFromGetWithoutSafetyOrDisclosurePost()=runBlocking {
        reply("""{"application_id":501,"status":"applying","version":7,"topic":{"topic_id":123,"is_complete":false,"is_delete":1,"getedcontact":true,"contact_info":"historical-contact"}}""")
        val app=repo.application(501)
        assertEquals("historical-contact",AdoptionContact.visible(app.topic))
        val request=server.takeRequest(); assertEquals("GET",request.method)
        assertEquals("/api/v2/adoption-applications/501",request.path); assertEquals(0,request.bodySize)
        assertEquals(1,server.requestCount)
    }
    @Test fun myApplicationsUsesSeparateEndpointAndCurrentTopicContact()=runBlocking {
        reply("""{"items":[{"application_id":501,"status":"ended","version":2,"topic":{"topic_id":123,"is_complete":false,"is_delete":1,"getedcontact":true,"contact_info":"new-contact"}}],"page":2,"size":10,"total":11,"has_more":false}""")
        val page=repo.mine("ended",2)
        assertEquals("new-contact",AdoptionContact.visible(page.items!![0].topic))
        val request=server.takeRequest(); assertEquals("GET",request.method)
        assertEquals("/api/v2/adoption-applications/mine",request.requestUrl!!.encodedPath)
        assertEquals("ended",request.requestUrl!!.queryParameter("status"))
        assertEquals("2",request.requestUrl!!.queryParameter("page"))
        assertEquals("Bearer account-one",request.getHeader("Authorization"))
        reply("""{"items":[],"page":1,"size":10,"total":0,"has_more":false}""")
        assertTrue(repo.mine("",1).items!!.isEmpty())
        assertNull(server.takeRequest().requestUrl!!.queryParameter("status"))
    }
    @Test fun contactDisplayFollowsAcquisitionAndTopicAvailability() {
        val topic=TopicSummary(123,null,null,null,false,1,getedcontact=true,contact_info="existing-contact")
        assertEquals("existing-contact",AdoptionContact.visible(topic))
        assertNull(AdoptionContact.visible(topic.copy(getedcontact=false)))
        assertNull(AdoptionContact.visible(topic.copy(is_complete=true)))
        assertNull(AdoptionContact.visible(topic.copy(is_delete=0)))
        assertNull(AdoptionContact.visible(topic.copy(unavailable=true)))
        assertNull(AdoptionContact.visible(topic.copy(contact_info=null)))
        assertNull(AdoptionContact.visible(null))
    }
    @Test fun approvalAndCompletionKeepApplicationAndWorkflowVersionsSeparate()=runBlocking {
        reply("""{"application_id":501,"version":9}"""); repo.action(501,ApplicationAction("communicate",8))
        val request=server.takeRequest(); assertEquals("PATCH",request.method)
        val action=JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(8,action["version"].asInt); assertFalse(action.has("refresh_contact_authorization"))
        reply("""{"topic_id":123,"workflow_status":"adopted","workflow_version":13}""")
        repo.topicAction(123,TopicAction("complete",true,12,501))
        val json=JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject
        assertEquals(12,json["version"].asInt); assertEquals(501,json["application_id"].asInt)
    }
    @Test fun classifiedCountsUseNativeFieldsWithoutFetchingHistory()=runBlocking {
        reply("""{"unread_count":5,"mine_unread_count":2,"received_unread_count":3}""")
        val counts=repo.classifiedUnread(); assertEquals(5,counts.unread_count)
        assertEquals(2,counts.mine_unread_count); assertEquals(3,counts.received_unread_count)
        assertEquals(1,server.requestCount)
    }
    @Test fun missingClassificationCountsRemainUnknown()=runBlocking {
        reply("""{"unread_count":5}""")
        val counts=repo.unread(); assertNull(counts.mine_unread_count); assertNull(counts.received_unread_count)
    }
    @Test fun nativeReminderScopeIsIndependentOfApplicationStatusAndVisiblePage()=runBlocking {
        reply("""{"items":[],"page":3,"size":10,"has_more":false,"unread_count":4,"mine_unread_count":0,"received_unread_count":4}""")
        val data=repo.reminderPage("received",33,3)
        assertTrue(data.items.isEmpty()); assertEquals(4,data.counts.received_unread_count)
        val url=server.takeRequest().requestUrl!!
        assertEquals("received",url.queryParameter("role")); assertEquals("33",url.queryParameter("topic_id"))
        assertEquals("true",url.queryParameter("unread")); assertEquals("3",url.queryParameter("page"))
        assertNull(url.queryParameter("status"))
    }
    @Test fun nativeRoleDoesNotFollowTargetAndQueriesNeverMarkRead()=runBlocking {
        reply("""{"items":[{"id":21,"role":"received","kind":"ended","target":"topic_detail","read":false,"message":"申请已结束","created_at":"2026-10-02T10:00:00+08:00"}],"page":1,"has_more":false,"unread_count":1,"mine_unread_count":0,"received_unread_count":1}""")
        val page=repo.reminderPage("received",null,1)
        assertEquals("received",page.items.single().role); assertEquals("ended",page.items.single().kind)
        assertEquals("GET",server.takeRequest().method); assertEquals(1,server.requestCount)
    }
    @Test fun legacyReminderClassificationScansAllHistoryAndNotificationPages()=runBlocking {
        reply("""{"items":[{"id":91,"read":false}],"page":1,"has_more":false}""") // Old backend ignores role/topic query.
        reply("""{"items":[{"application_id":1,"status":"applying"}],"page":1,"has_more":true}""")
        reply("""{"items":[{"application_id":88,"status":"ended"}],"page":2,"has_more":false}""")
        reply("""{"items":[{"id":1,"application_id":88,"topic_id":33,"target":"application_management","read":false},{"id":2,"application_id":99,"topic_id":33,"target":"topic_detail","read":false}],"page":1,"has_more":true}""")
        reply("""{"items":[{"id":2,"application_id":99,"topic_id":33,"target":"topic_detail","read":false},{"id":3,"application_id":99,"topic_id":34,"read":false},{"id":4,"application_id":99,"topic_id":33,"read":true}],"page":2,"has_more":false}""")
        val page=repo.reminderPage("received",33,1)
        assertEquals(listOf(2),page.items.map { it.id }); assertEquals(1,page.counts.received_unread_count)
        val requests=(1..5).map { server.takeRequest() }
        assertEquals("received",requests[0].requestUrl!!.queryParameter("role"))
        assertNull(requests[1].requestUrl!!.queryParameter("status")); assertEquals("2",requests[2].requestUrl!!.queryParameter("page"))
        assertNull(requests[3].requestUrl!!.queryParameter("role")); assertNull(requests[3].requestUrl!!.queryParameter("topic_id"))
        assertEquals("2",requests[4].requestUrl!!.queryParameter("page"))
    }
    @Test fun legacyGlobalCountsIncludeTerminalHistoryAndBothRoles()=runBlocking {
        reply("""{"unread_count":3}""")
        reply("""{"items":[{"application_id":88,"status":"ended"}],"page":1,"has_more":false}""")
        reply("""{"items":[{"id":1,"application_id":88,"read":false},{"id":2,"application_id":99,"target":"topic_detail","read":false},{"id":3,"application_id":100,"read":false}],"page":1,"has_more":false}""")
        val counts=repo.classifiedUnread()
        assertEquals(1,counts.mine_unread_count); assertEquals(2,counts.received_unread_count)
        assertEquals(3,counts.unread_count)
    }
    @Test fun singleReadUsesOnlyClickedIdAndPermissionFailureKeepsLogin()=runBlocking {
        reply("{}",403); assertEquals(403,error { repo.read(21) }.http); assertEquals(0,logouts)
        reply("{}"); repo.read(21)
        val requests=(1..2).map { server.takeRequest() }
        requests.forEach { assertEquals("POST",it.method); assertEquals("/api/v2/adoption-notifications/21/read",it.requestUrl!!.encodedPath) }
        assertEquals(2,server.requestCount)
    }
    @Test fun reminderMergingAndLegacyClassificationIgnoreKindAndTarget() {
        val items=listOf(
            AdoptionNotification(1,"mine",1,88,"application_management",false,null,kind="new"),
            AdoptionNotification(2,"received",1,99,"topic_detail",false,null,kind="ended"),
            AdoptionNotification(2,"received",1,99,"topic_detail",false,null,kind="ended"))
        val classified=NotificationClassification.classify(items,setOf(88))
        assertEquals(listOf("mine","received"),classified.map { it.role })
        assertEquals(2,NotificationClassification.counts(classified).unread_count)
    }
    @Test fun messageBadgeCountsApplicationTotalOnceAndIgnoresClassifiedDuplicates() {
        val categories=listOf("system","my_applications","received_applications","like","collection","comment")
        val values=listOf(1,2,3,2,3,4)
        val entries=categories.mapIndexed { index, category -> com.rescue.flutter_720yun.home.models.MessageListModel("", "", values[index], category) }
        assertEquals(15,com.rescue.flutter_720yun.message.models.MessageBadge.total(entries,5))
        assertEquals(10,com.rescue.flutter_720yun.message.models.MessageBadge.total(entries,null))
        assertEquals(0,com.rescue.flutter_720yun.message.models.MessageBadge.total(emptyList(),null))
    }
    @Test fun restoredAttemptReusesKeyButEditedOrExplicitNewAttemptDoesNot() {
        val fingerprint = ApplicationFingerprint.of(1,123,ApplicationWrite(4,"想了解"))
        val key = ApplicationAttempt.key(null,null,fingerprint)
        assertEquals(key,ApplicationAttempt.key(key,fingerprint,fingerprint))
        assertNotEquals(key,ApplicationAttempt.key(key,fingerprint,"changed-content"))
        assertNotEquals(key,ApplicationAttempt.key(null,null,fingerprint))
        assertEquals(36,key.length)
    }
    @Test fun fingerprintSurvivesRestorationButChangesForAnotherAccountOrContent() {
        val body=ApplicationWrite(4,"想了解")
        val original=ApplicationFingerprint.of(1,123,body)
        assertEquals(original,ApplicationFingerprint.of(1,123,body.copy()))
        assertNotEquals(original,ApplicationFingerprint.of(2,123,body))
        assertNotEquals(original,ApplicationFingerprint.of(1,124,body))
        assertNotEquals(original,ApplicationFingerprint.of(1,123,body.copy(profile_version=5)))
        assertNotEquals(original,ApplicationFingerprint.of(1,123,body.copy(statement="修改")))
        assertFalse(original.contains(body.statement))
    }
}
