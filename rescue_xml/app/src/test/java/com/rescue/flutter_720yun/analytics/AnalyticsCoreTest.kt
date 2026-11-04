package com.rescue.flutter_720yun.analytics

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AnalyticsCoreTest {
    private class Store : AnalyticsStateStore {
        var value: AnalyticsState?=null
        override fun read()=value
        override fun write(state: AnalyticsState) { value=state }
    }
    private val store=Store()
    private var time=1_792_555_200_000L
    private val anon=AnalyticsIdentity(null,null)
    private val alice=AnalyticsIdentity(1,"first-token")
    private val bob=AnalyticsIdentity(2,"second-token")
    private fun core()=AnalyticsCore(store,{time})
    private fun event(c: AnalyticsCore,who: AnalyticsIdentity=anon,interactive: Boolean=false)=
        c.create("page_view","home",emptyMap(),who,"1.3.0",interactive)!!
    @Test fun coldOpenIsOnceAndRestoredSessionStillRecordsColdOpen() {
        val c=core();assertTrue(c.enterForeground(anon));assertFalse(c.enterForeground(anon))
        val a=event(c)
        val restored=core();assertTrue(restored.enterForeground(anon))
        assertEquals(a.event.session_id,event(restored).event.session_id)
    }
    @Test fun shortBackgroundKeepsSessionAndDoesNotOpenAgain() {
        val c=core();c.enterForeground(anon);val first=event(c)
        assertTrue(c.leaveForeground(anon));assertFalse(c.leaveForeground(anon))
        time+=AnalyticsCore.IDLE_MS-1
        assertFalse(c.enterForeground(anon));assertEquals(first.event.session_id,event(c).event.session_id)
    }
    @Test fun exactlyThirtyMinutesBackgroundOpensNewSessionWithSequenceOne() {
        val c=core();c.enterForeground(anon);val first=event(c);c.leaveForeground(anon)
        time+=AnalyticsCore.IDLE_MS
        assertTrue(c.enterForeground(anon));val second=event(c)
        assertNotEquals(first.event.session_id,second.event.session_id);assertEquals(1,second.event.sequence)
    }
    @Test fun shortBackgroundKeepsSessionEvenWhenPreviousInteractionWasLongAgo() {
        val c=core();c.enterForeground(anon);val first=event(c)
        time+=AnalyticsCore.IDLE_MS-1000;c.leaveForeground(anon);time+=2000
        assertFalse(c.enterForeground(anon));assertEquals(first.event.session_id,event(c).event.session_id)
    }
    @Test fun foregroundIdleChangesSessionOnlyAtNextInteraction() {
        val c=core();c.enterForeground(anon);val first=event(c)
        time+=AnalyticsCore.IDLE_MS
        val automatic=event(c);assertEquals(first.event.session_id,automatic.event.session_id)
        val next=event(c,interactive=true)
        assertNotEquals(first.event.session_id,next.event.session_id);assertEquals(1,next.event.sequence)
    }
    @Test fun continuousInteractionDoesNotHardRotateAtThirtyMinutes() {
        val c=core();c.enterForeground(anon);val first=event(c)
        repeat(8) { time+=10*60*1000;c.interaction(anon) }
        assertEquals(first.event.session_id,event(c).event.session_id)
    }
    @Test fun automaticCompletionsCannotExtendIdleTime() {
        val c=core();c.enterForeground(anon);val first=event(c)
        repeat(3) { time+=10*60*1000;event(c) }
        c.interaction(anon)
        assertNotEquals(first.event.session_id,event(c).event.session_id)
    }
    @Test fun anonymousLoginKeepsInstallationAndSessionWithNewToken() {
        val c=core();c.enterForeground(anon);val first=event(c)
        c.identityChanged(alice)
        val logged=c.create("login_success","",mapOf("login_method" to "password"),alice,"1")!!
        assertEquals(first.event.anonymous_id,logged.event.anonymous_id)
        assertEquals(first.event.session_id,logged.event.session_id)
        assertEquals(2,logged.event.sequence);assertEquals("first-token",logged.identity.token)
        assertNull(first.identity.token)
    }
    @Test fun tokenRotationDoesNotChangeSession() {
        val c=core();c.enterForeground(alice);val first=event(c,alice)
        c.identityChanged(alice.copy(token="renewed"))
        assertEquals(first.event.session_id,event(c,alice).event.session_id)
    }
    @Test fun logoutRotatesSessionButNotInstallation() {
        val c=core();c.enterForeground(alice);val first=event(c,alice)
        c.identityChanged(anon);val after=event(c)
        assertNotEquals(first.event.session_id,after.event.session_id)
        assertEquals(first.event.anonymous_id,after.event.anonymous_id);assertEquals(1,after.event.sequence)
    }
    @Test fun accountSwitchRotatesSessionAndPreservesFrozenOldIdentity() {
        val c=core();c.enterForeground(alice);val first=event(c,alice)
        c.identityChanged(bob);val after=event(c,bob)
        assertNotEquals(first.event.session_id,after.event.session_id)
        assertEquals("first-token",first.identity.token);assertEquals("second-token",after.identity.token)
    }
    @Test fun persistedAccountSwitchIsRecognizedAfterRestart() {
        val c=core();c.enterForeground(alice);val first=event(c,alice)
        val restored=core();restored.enterForeground(bob)
        assertNotEquals(first.event.session_id,event(restored,bob).event.session_id)
    }
    @Test fun sequenceIsSerializedUnderConcurrentCreation() {
        val c=core();c.enterForeground(anon)
        val pool=Executors.newFixedThreadPool(6)
        try {
            val futures=(1..100).map { pool.submit<AnalyticsEnvelope> { event(c) } }
            val events=futures.map { it.get(5,TimeUnit.SECONDS).event }
            assertEquals((1..100).toList(),events.map { it.sequence }.sorted())
            assertEquals(100,events.map { it.event_id }.distinct().size)
        } finally { pool.shutdownNow() }
    }
    @Test fun integerSequenceExhaustionRotatesSession() {
        val c=core();c.enterForeground(anon);val first=event(c)
        store.value=store.value!!.copy(sequence=Int.MAX_VALUE)
        val next=event(core())
        assertNotEquals(first.event.session_id,next.event.session_id);assertEquals(1,next.event.sequence)
    }
    @Test fun disabledCollectionDoesNotCreateOrPersistEvents() {
        val c=core();c.setEnabled(false)
        assertFalse(c.enterForeground(anon));assertNull(c.create("app_open","",emptyMap(),anon,"1"))
        assertNull(store.value)
        c.setEnabled(true);assertEquals(1,event(c).event.sequence)
    }
    @Test fun logoutWhileDisabledStillEndsExistingSession() {
        val c=core();c.enterForeground(alice);val first=event(c,alice)
        c.setEnabled(false);c.identityChanged(anon);c.identityChanged(alice);c.setEnabled(true)
        assertNotEquals(first.event.session_id,event(c,alice).event.session_id)
    }
    @Test fun pageExposureDedupesConfigPermissionsAndRefreshButCountsReturnAndListSwitch() {
        val e=AnalyticsExposure()
        assertTrue(e.show("instance:profile","profile"))
        assertFalse(e.show("instance:profile","profile"))
        assertTrue(e.show("instance:message:like","message_list"))
        assertFalse(e.show("instance:message:like","message_list"))
        assertTrue(e.show("instance:message:collect","message_list"))
        assertTrue(e.show("instance:profile","profile"))
    }
    @Test fun unsupportedPageDoesNotInventReferrer() {
        val e=AnalyticsExposure();e.show("a","home");assertFalse(e.show("b",null));assertNull(e.lastPage)
        assertTrue(e.show("a","home"))
    }
    @Test fun payloadHasOnlyWhitelistedTopLevelKeysAndIntegerObjectId() {
        val c=core();c.enterForeground(alice)
        val e=c.create("publish_success","publish_adoption",mapOf("publish_type" to "adoption","object_id" to Long.MAX_VALUE),alice,"1.3.0")!!.event
        val json=Gson().toJsonTree(e).asJsonObject
        assertEquals(setOf("event_id","event_name","anonymous_id","session_id","sequence","occurred_at","platform","app_version","page","properties"),json.keySet())
        assertEquals(Long.MAX_VALUE,json.getAsJsonObject("properties").get("object_id").asLong)
        assertEquals("android",e.platform);assertEquals(time,OffsetDateTime.parse(e.occurred_at).toInstant().toEpochMilli())
        UUID.fromString(e.event_id);UUID.fromString(e.anonymous_id);UUID.fromString(e.session_id)
    }
    @Test fun requiredAuthAndSuccessObjectIdsCannotBeFaked() {
        val c=core()
        for(name in listOf("login_success","publish_success","logOff","click_video_ad","video_ad_success"))
            assertNull(c.create(name,"",emptyMap(),anon,"1"))
        for(id in listOf<Any>(0,-1,1.0,"123",true)) assertNull(c.create("publish_success","publish_adoption",mapOf("publish_type" to "adoption","object_id" to id),alice,"1"))
        assertNull(c.create("publish_success","publish_adoption",mapOf("publish_type" to "adoption"),alice,"1"))
    }
    @Test fun messageListsRequireEnumeratedSubtypeAndOtherPagesRejectIt() {
        for(type in AnalyticsContract.lists) assertTrue(AnalyticsContract.valid("page_view","message_list",mapOf("message_list_type" to type),false))
        assertFalse(AnalyticsContract.valid("page_view","message_list",emptyMap(),false))
        assertFalse(AnalyticsContract.valid("page_view","message_list",mapOf("message_list_type" to 15),false))
        assertFalse(AnalyticsContract.valid("page_view","adoption_profile",mapOf("message_list_type" to "like"),false))
    }
    @Test fun publishFormPageAndTypeMustMatch() {
        for(type in listOf("adoption","find_pet","show_pet")) {
            assertTrue(AnalyticsContract.valid("publish_page_view","publish_$type",mapOf("publish_type" to type),false))
            assertFalse(AnalyticsContract.valid("publish_page_view","home",mapOf("publish_type" to type),false))
        }
    }
    @Test fun unknownFieldsFreeTextAndWrongCaseAreRejected() {
        assertFalse(AnalyticsContract.valid("logoff","",emptyMap(),true))
        assertFalse(AnalyticsContract.valid("page_view","HomeActivity",emptyMap(),false))
        assertFalse(AnalyticsContract.valid("login_failed","login",mapOf("reason_code" to "password=secret"),false))
        for(key in listOf("user_id","token","source","ip","password")) assertFalse(AnalyticsContract.valid("app_open","",mapOf(key to "secret"),false))
    }
    @Test fun loginFailureRequiresReason() {
        assertFalse(AnalyticsContract.valid("login_failed","login",emptyMap(),false))
        assertTrue(AnalyticsContract.valid("login_failed","login",mapOf("reason_code" to "network_error"),false))
    }
    @Test fun rewardUsesOriginalAttemptUuidAndSceneWithNoInventedPage() {
        val c=core();val id=UUID.randomUUID().toString()
        for(scene in listOf("my_posts","publish_success")) {
            val p=mapOf("ad_placement" to scene,"object_id" to 123L,"reward_attempt_id" to id)
            val e=c.create("video_ad_success","",p,alice,"1",eventId=id)!!
            assertEquals(id,e.event.event_id);assertEquals(id,e.event.properties["reward_attempt_id"])
            assertFalse(AnalyticsContract.valid("video_ad_success",scene,p,true))
        }
    }
    @Test fun byteLimitsAreMeasuredInUtf8() {
        val e=event(core()).event
        assertFalse(AnalyticsContract.withinSize(e.copy(properties=mapOf("x" to "猫".repeat(1000)))))
        assertFalse(AnalyticsContract.withinSize(e.copy(page="x".repeat(9000))))
        assertTrue(AnalyticsContract.withinSize(e))
    }
    @Test fun preferenceWriteFailureDoesNotBreakBusinessInteractionOrReuseSequences() {
        val c=AnalyticsCore(object: AnalyticsStateStore {
            override fun read(): AnalyticsState?=null
            override fun write(state: AnalyticsState) { error("disk failure") }
        },{time})
        assertTrue(c.enterForeground(anon))
        assertEquals(1,event(c).event.sequence);c.interaction(anon)
        assertEquals(2,event(c).event.sequence)
    }

}
