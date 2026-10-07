package com.rescue.flutter_720yun.adoption.qa

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.google.gson.Gson
import com.rescue.flutter_720yun.adoption.fragment.ApplicationSheetViewModel
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.adoption.viewmodels.*
import com.rescue.flutter_720yun.home.models.UserInfo
import com.rescue.flutter_720yun.home.models.AddressItem
import com.rescue.flutter_720yun.network.AdoptionService
import com.rescue.flutter_720yun.network.AdoptionServiceCreator
import com.rescue.flutter_720yun.util.UserManager
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.io.File

/** Real ViewModels + real Retrofit against scripted HTTP; does not claim server or UI validation. */
class AdoptionFlowAuditTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: AdoptionRepository
    private val store = ViewModelStore()
    private var oldUser: UserInfo? = null
    private val open = """{"topic_id":123,"workflow_status":"open","workflow_version":7,"can_apply":true,"can_manage":false}"""
    private val completeProfile = """{"profile":{"age":28,"residence_city":"上海市","residence_district":"浦东新区","housing_type":"owned","employment_status":"employed"},"is_complete":true,"version":2}"""
    private val applying = """{"application_id":501,"status":"applying","version":1,"applicant":{"user_id":1},"topic":{"topic_id":123,"is_delete":1,"is_complete":false}}"""
    private val activeState = """{"topic_id":123,"workflow_status":"open","workflow_version":7,"can_apply":false,"application_id":501,"application_status":"applying"}"""
    private val profile get() = AdoptionServiceCreator.gson.fromJson(completeProfile, ProfileData::class.java)

    @Before fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        oldUser = UserManager.userInfo
        userField().set(UserManager, Gson().fromJson("""{"id":1,"token":"qa-only"}""", UserInfo::class.java))
        server = MockWebServer().apply { start() }
        val service = Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create(AdoptionServiceCreator.gson))
            .build().create(AdoptionService::class.java)
        repository = AdoptionRepository(service, { AdoptionAccount(1,"qa-only",0) }, {})
    }
    @After fun teardown() {
        store.clear()
        server.shutdown()
        userField().set(UserManager, oldUser)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
    private fun userField() = UserManager::class.java.getDeclaredField("_userInfo").apply { isAccessible = true }
    private fun <T : AdoptionViewModel> tracked(vm: T): T {
        AdoptionViewModel::class.java.getDeclaredField("repository").apply { isAccessible = true }.set(vm, repository)
        store.put(vm.javaClass.name, vm)
        return vm
    }
    private fun detail() = tracked(AdoptionDetailViewModel(SavedStateHandle())).apply { topic = 123 }
    private fun reply(data: String, http: Int = 200, code: Int = http) {
        server.enqueue(MockResponse().setResponseCode(http).setHeader("Content-Type", "application/json")
            .setBody("""{"code":$code,"message":"QA response","data":$data}"""))
    }
    private fun idle(vm: AdoptionViewModel) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while(vm.busy.value == true && System.nanoTime() < deadline) Thread.sleep(5)
        assertFalse("ViewModel did not finish", vm.busy.value == true)
    }
    private fun reminderIdle(vm: AdoptionApplicationsViewModel) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while((vm.reminderBusy.value == true || vm.readingId.value != null) && System.nanoTime() < deadline) Thread.sleep(5)
        assertFalse("Reminder did not finish", vm.reminderBusy.value == true || vm.readingId.value != null)
    }
    private fun requests() = List(server.requestCount) { server.takeRequest(1, TimeUnit.SECONDS)!! }

    @Test fun missingProfileStopsBeforeAnyApplicationWrite() {
        val vm = detail()
        reply(open); reply("""{"profile":null,"is_complete":false,"version":null}""")
        vm.prepare(); idle(vm)
        assertEquals(AdoptionDetailEvent.EditProfile, vm.event.value)
        assertEquals(listOf("GET", "GET"), requests().map { it.method })
    }
    @Test fun completeProfileOnlyPreparesConfirmationUntilUserSubmits() {
        val vm = detail()
        reply(open); reply(completeProfile)
        vm.prepare(); idle(vm)
        assertTrue(vm.event.value is AdoptionDetailEvent.Confirm)
        assertEquals(2, server.requestCount)
        assertTrue(requests().all { it.method == "GET" })
    }
    @Test fun closedOrUnknownWorkflowStopsBeforeProfileAndWrite() {
        for(status in listOf("closed", "adopted", "unexpected")) {
            val vm = detail()
            reply(open.replace("\"open\"", "\"$status\""))
            vm.prepare(); idle(vm)
            assertEquals(409, vm.error.value?.status)
            assertNull(vm.event.value)
        }
        assertEquals(3, server.requestCount)
        assertTrue(requests().all { it.requestUrl!!.encodedPath.endsWith("application-state") })
    }
    @Test fun submitUsesEmptyStatementAndRefreshesRealApplication() {
        val vm = detail()
        reply(open); reply(applying); reply(activeState)
        vm.submit(profile, ""); idle(vm)
        assertEquals(AdoptionDetailEvent.Applied, vm.event.value)
        assertEquals(501, vm.state.value?.application_id)
        val requests = requests()
        assertEquals(listOf("GET", "POST", "GET"), requests.map { it.method })
        assertEquals("""{"profile_version":2,"statement":""}""", requests[1].body.readUtf8())
        assertEquals(36, requests[1].getHeader("Idempotency-Key")!!.length)
    }
    @Test fun failedSubmitRetryKeepsOriginalKeyAndBody() {
        val vm = detail()
        reply(open); reply("{}", 503)
        vm.submit(profile, ""); idle(vm)
        assertNull(vm.event.value)
        reply(open); reply(applying); reply(activeState)
        vm.submit(profile, ""); idle(vm)
        val writes = requests().filter { it.method == "POST" }
        assertEquals(2, writes.size)
        assertEquals(writes[0].getHeader("Idempotency-Key"), writes[1].getHeader("Idempotency-Key"))
        assertEquals(writes[0].body.readUtf8(), writes[1].body.readUtf8())
    }
    @Test fun existingActiveApplicationPreventsSecondPost() {
        val vm = detail()
        reply(activeState)
        vm.submit(profile, ""); idle(vm)
        assertNull(vm.event.value)
        assertEquals(501, vm.state.value?.application_id)
        assertEquals(listOf("GET"), requests().map { it.method })
    }
    @Test fun endedIdempotentReplayMustNotReportNewApplicationSuccess() {
        val vm = detail()
        reply(open); reply("{}", 503)
        vm.submit(profile, ""); idle(vm)
        // First write actually committed, then the owner ended it before the client recovered.
        val endedState = open.dropLast(1) + """, "application_id":501,"application_status":"ended","application_result":"not_completed"}"""
        reply(endedState)
        reply(applying.replace("\"applying\"", "\"ended\"").dropLast(1) + """, "result":"not_completed"}""")
        reply(endedState)
        vm.submit(profile, ""); idle(vm)
        assertNotEquals("Ended replay is not a new active application", AdoptionDetailEvent.Applied, vm.event.value)
        assertTrue(vm.event.value is AdoptionDetailEvent.Notice)
        assertEquals("ended", vm.state.value?.application_status)
        val firstWrites = requests().filter { it.method == "POST" }
        assertEquals(firstWrites[0].getHeader("Idempotency-Key"), firstWrites[1].getHeader("Idempotency-Key"))
        assertEquals(2, firstWrites.size)
        // Only the next explicit user confirmation starts a new operation.
        reply(endedState); reply(applying.replace("501", "502")); reply(activeState.replace("501", "502"))
        vm.submit(profile, ""); idle(vm)
        assertEquals(AdoptionDetailEvent.Applied, vm.event.value)
        assertEquals(502, vm.state.value?.application_id)
        val nextRequests = List(3) { server.takeRequest(1, TimeUnit.SECONDS)!! }
        assertNotEquals(firstWrites.last().getHeader("Idempotency-Key"), nextRequests[1].getHeader("Idempotency-Key"))
    }
    @Test fun failedPostSuccessRefreshMustNotLeaveCanApplyState() {
        val vm = detail()
        reply(open); reply(applying); reply("{}", 503)
        vm.submit(profile, ""); idle(vm)
        assertFalse("POST succeeded but stale pre-submit state still permits applying", vm.state.value?.can_apply == true)
        assertEquals(501, vm.state.value?.application_id)
        assertEquals("applying", vm.state.value?.application_status)
        assertNull(vm.error.value)
        assertTrue(vm.event.value is AdoptionDetailEvent.Notice)
    }
    @Test fun failedFilterChangeMustNotShowPreviousFilterApplications() {
        val vm = tracked(AdoptionApplicationsViewModel(SavedStateHandle()))
        reply("""{"items":[$applying],"has_more":false,"page":1}""")
        vm.load(); idle(vm)
        assertEquals("applying", vm.items.value!!.single().status)
        vm.status = "ended"
        reply("{}", 503)
        vm.load(); idle(vm)
        assertTrue("Ended filter still shows applying rows after load failure", vm.items.value.orEmpty().all { it.status == "ended" })
    }
    @Test fun profileSaveMustNeverSubmitApplication() {
        val vm = tracked(AdoptionProfileViewModel())
        reply(completeProfile); vm.load(); idle(vm)
        vm.age = "29"
        reply(completeProfile.replace("\"version\":2", "\"version\":3")); vm.save(); idle(vm)
        assertEquals(true, vm.saved.value)
        assertEquals(listOf("GET", "PUT"), requests().map { it.method })
    }
    @Test fun businessDenialsMustNotBecomeApplicationSuccess() {
        for(code in listOf("PROFILE_REQUIRED", "PROFILE_CHANGED", "IDEMPOTENCY_CONFLICT", "VERSION_CONFLICT", "TOPIC_CLOSED", "BLACKLISTED")) {
            val vm = detail()
            val status = if(code == "BLACKLISTED") 403 else 409
            reply(open); reply("""{"error_code":"$code"}""", http = 200, code = status)
            vm.submit(profile, ""); idle(vm)
            assertNull(code, vm.event.value)
            assertEquals(code, vm.error.value?.errorCode)
            assertEquals(status, vm.error.value?.status)
        }
        assertEquals(12, server.requestCount)
    }
    @Test fun failedReadKeepsReminderAndCountsForRetry() {
        val vm = tracked(AdoptionApplicationsViewModel(SavedStateHandle()))
        reply("""{"items":[{"id":21,"role":"received","read":false,"message":"新的申请"}],"page":1,"has_more":false,"unread_count":1,"mine_unread_count":0,"received_unread_count":1}""")
        vm.loadReminders(); reminderIdle(vm)
        reply("{}", 503)
        vm.readReminder(vm.reminders.value!!.single()); reminderIdle(vm)
        assertEquals(21, vm.reminders.value!!.single().id)
        assertEquals(1, vm.reminderCounts.value?.received_unread_count)
        assertNotNull(vm.reminderError.value)
        assertNull(vm.readMessage.value)
        assertEquals(listOf("GET", "POST"), requests().map { it.method })
    }
    @Test fun failedReminderAppendKeepsSuccessfulPageAndCounts() {
        val vm = tracked(AdoptionApplicationsViewModel(SavedStateHandle()))
        reply("""{"items":[{"id":21,"role":"received","read":false}],"page":1,"has_more":true,"unread_count":11,"mine_unread_count":0,"received_unread_count":11}""")
        vm.loadReminders(); reminderIdle(vm)
        reply("{}", 503)
        vm.loadReminders(false); reminderIdle(vm)
        assertEquals(21, vm.reminders.value!!.single().id)
        assertEquals(11, vm.reminderCounts.value?.received_unread_count)
        assertTrue(vm.reminderHasMore)
        assertEquals("2", requests().last().requestUrl!!.queryParameter("page"))
    }
    @Test fun everyCityMustOfferACompletableDistrictSelection() {
        val regions = Gson().fromJson(File(System.getProperty("qa.adoption.location")).readText(), Array<AddressItem>::class.java)
        for(city in regions.flatMap { it.children.orEmpty() }) {
            val choices = AdoptionRegions.districts(city)
            assertTrue("Mandatory district picker has no options: ${city.name}", choices.isNotEmpty())
            if(city.children.isNullOrEmpty()) assertEquals(listOf("全市"), choices.map { it.name })
            else assertEquals(city.children, choices)
        }
    }
    @Test fun newFilterAfterFailureMustRestartAtPageOne() {
        val vm = tracked(AdoptionApplicationsViewModel(SavedStateHandle()))
        reply("""{"items":[$applying],"has_more":true,"page":1}""")
        vm.load(); idle(vm)
        reply("""{"items":[],"has_more":true,"page":2}""")
        vm.load(false); idle(vm)
        vm.status = "ended"
        reply("{}", 503); vm.load(); idle(vm)
        reply("""{"items":[],"has_more":false,"page":1}""")
        vm.load(false); idle(vm)
        assertEquals("1", requests().last().requestUrl!!.queryParameter("page"))
        assertTrue(vm.items.value.isNullOrEmpty())
    }
    @Test fun sameFilterRefreshFailureKeepsSuccessfulData() {
        val vm = tracked(AdoptionApplicationsViewModel(SavedStateHandle()))
        reply("""{"items":[$applying],"has_more":true,"page":1}""")
        vm.load(); idle(vm)
        reply("{}", 503); vm.load(); idle(vm)
        assertEquals(501, vm.items.value!!.single().application_id)
        assertEquals(1, vm.page)
        assertTrue(vm.hasMore)
    }
    @Test fun invalidApplyReplyMustNotReportSuccess() {
        val vm = detail()
        reply(open); reply("{}"); vm.submit(profile, ""); idle(vm)
        assertNull(vm.event.value)
        assertNull(vm.state.value)
        assertEquals(502, vm.error.value?.status)
        assertEquals(2, server.requestCount)
    }
    @Test fun communicatingReplayDisplaysActualStatus() {
        val vm = detail()
        reply(open); reply(applying.replace("applying", "communicating"))
        reply(activeState.replace("applying", "communicating"))
        vm.submit(profile, ""); idle(vm)
        assertEquals("communicating", vm.state.value?.application_status)
        assertTrue(vm.event.value is AdoptionDetailEvent.Notice)
        assertNotEquals(AdoptionDetailEvent.Applied, vm.event.value)
    }
    @Test fun sheetFailureCanBeRetriedWithoutReopening() {
        val vm = tracked(ApplicationSheetViewModel())
        reply("{}", 503); vm.load(501); idle(vm)
        assertNotNull(vm.error.value)
        assertNull(vm.application.value)
        reply(applying); reply(activeState)
        vm.load(501); idle(vm)
        assertNull(vm.error.value)
        assertEquals(501, vm.application.value?.application_id)
        assertEquals(listOf("GET", "GET", "GET"), requests().map { it.method })
    }
}
