package com.rescue.flutter_720yun.adoption

import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.network.AdoptionServiceCreator
import org.junit.Assert.*
import org.junit.Test

class AdoptionDetailPolicyTest {
    private val gson = AdoptionServiceCreator.gson
    private fun state(workflow: String = "open", status: String? = null, canApply: Boolean = true, owner: Boolean = false, result: String? = null) =
        ApplicationState(123,workflow,1,1,canApply,owner,4,status,result,1,true)
    private fun app(status: String = "communicating", result: String? = null): AdoptionApplication = gson.fromJson("""{
        "application_id":4,"status":"$status","result":${result?.let { "\"$it\"" } ?: "null"},"version":1,
        "applicant":{"user_id":1},"topic":{"topic_id":123,"is_delete":1,"is_complete":false,"getedcontact":true,"contact_info":" wx123 "}
    }""",AdoptionApplication::class.java)
    @Test fun routingAlwaysPrioritizesOwnerAndWorkflowBeforeHistoricalApplication() {
        assertEquals(AdoptionDetailPolicy.Route.MANAGE,AdoptionDetailPolicy.route(state("closed",owner=true)))
        for (workflow in listOf("closed","adopted")) {
            assertEquals("送养已结束",AdoptionDetailPolicy.title(state(workflow,"communicating")))
            assertEquals(AdoptionDetailPolicy.Route.ENDED,AdoptionDetailPolicy.route(state(workflow,"communicating")))
        }
        assertEquals(AdoptionDetailPolicy.Route.UNAVAILABLE,AdoptionDetailPolicy.route(state("unexpected")))
        for (status in listOf("applying","communicating","ended")) assertEquals(AdoptionDetailPolicy.Route.APPLICATION,AdoptionDetailPolicy.route(state(status=status)))
        assertEquals(AdoptionDetailPolicy.Route.APPLY,AdoptionDetailPolicy.route(state()))
        assertEquals(AdoptionDetailPolicy.Route.BLOCKED,AdoptionDetailPolicy.route(state(canApply=false)))
    }
    @Test fun buttonTitlesMatchIos() {
        assertEquals("申请领养",AdoptionDetailPolicy.title(state()))
        assertEquals("申请中",AdoptionDetailPolicy.title(state(status="applying")))
        assertEquals("查看联系方式",AdoptionDetailPolicy.title(state(status="communicating")))
        assertEquals("沟通中",AdoptionDetailPolicy.title(state(status="communicating").copy(contact_authorized=false)))
        assertEquals("查看结果 / 重新申请",AdoptionDetailPolicy.title(state(status="ended",result="not_completed")))
        assertEquals("申请已结束",AdoptionDetailPolicy.title(state(status="ended",result="completed")))
    }
    @Test fun onlyUncompletedEndedApplicationOnOpenApplicableTopicCanReapply() {
        val ended = app("ended","not_completed")
        assertTrue(AdoptionDetailPolicy.canReapply(state(),ended))
        assertFalse(AdoptionDetailPolicy.canReapply(state("closed"),ended))
        assertFalse(AdoptionDetailPolicy.canReapply(state(canApply=false),ended))
        assertFalse(AdoptionDetailPolicy.canReapply(state(),app("ended","completed")))
        assertFalse(AdoptionDetailPolicy.canReapply(state(),app()))
        // Acquisition survives an ended application while the topic remains open.
        assertEquals("wx123",AdoptionContact.visible(ended.topic))
        assertNull(AdoptionContact.visible(ended.topic!!.copy(is_complete=true)))
    }
    @Test fun profileRequiresServerCompletenessValidFieldsAndVersion() {
        val profile = AdoptionProfile(20,"上海","浦东","owned","employed","",3)
        val data = ProfileData(profile,true,emptyList(),null)
        assertTrue(AdoptionDetailPolicy.profileReady(data))
        assertFalse(AdoptionDetailPolicy.profileReady(data.copy(is_complete=false)))
        assertFalse(AdoptionDetailPolicy.profileReady(data.copy(profile=profile.copy(age=9))))
        assertFalse(AdoptionDetailPolicy.profileReady(data.copy(profile=profile.copy(residence_city=" "))))
        assertFalse(AdoptionDetailPolicy.profileReady(data.copy(profile=profile.copy(housing_type="bad"))))
        assertFalse(AdoptionDetailPolicy.profileReady(data.copy(profile=profile.copy(version=0))))
        assertTrue(AdoptionDetailPolicy.profileReady(data.copy(profile=profile.copy(pet_experience="👨‍👩‍👧‍👦".repeat(200)))))
        assertFalse(AdoptionDetailPolicy.profileReady(data.copy(profile=profile.copy(pet_experience="文".repeat(201)))))
    }
    @Test fun flatAndNestedV2ResponsesKeepSameApplicationAndState() {
        val nested=gson.fromJson("""{"topic_id":123,"workflow_status":"open","workflow_version":7,"can_apply":true,"application":{"application_id":4,"status":"ended","result":"not_completed","version":2}}""",ApplicationState::class.java)
        assertEquals(4,nested.application_id); assertEquals("ended",nested.application_status)
        assertEquals("not_completed",nested.application_result); assertEquals(2,nested.application_version);assertEquals(7,nested.workflow_version)
        val flat=app()
        val wrapped=gson.fromJson("""{"application":${gson.toJson(flat)}}""",AdoptionApplication::class.java)
        assertEquals(flat,wrapped)
    }
}
