package com.rescue.flutter_720yun.adoption

import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.network.AdoptionServiceCreator
import org.junit.Test
import org.junit.Assert.*

class AdoptionContractTest {
    @Test fun previewArraysAndCommaStringsNormalizeWithoutLosingPage() {
        val gson=AdoptionServiceCreator.gson
        for(preview in listOf("[\"\",\"cat.jpg\",\"dog.jpg\"]", "\" , cat.jpg, dog.jpg\"")) {
            val topic=gson.fromJson("""{"topic_id":123,"preview_img":$preview,"is_delete":1}""",TopicSummary::class.java)
            assertEquals("cat.jpg",topic.preview_img)
        }
        val state=gson.fromJson("""{"topic_id":123,"version":7,"can_manage":true}""",ApplicationState::class.java)
        assertEquals(7,state.workflow_version)
    }
    @Test fun onlyExactOwnerRejectionCombinationIsRejected() {
        assertEquals("已结束 · 被拒绝",AdoptionLabels.status("ended","not_completed","owner_ended","拒绝沟通申请"))
        assertFalse(AdoptionLabels.status("ended","not_completed","applicant_ended","拒绝沟通申请").contains("被拒绝"))
        assertFalse(AdoptionLabels.status("ended","not_completed","owner_ended","").contains("被拒绝"))
        assertFalse(AdoptionLabels.status("ended","not_completed",null,"拒绝沟通申请").contains("被拒绝"))
    }
}
