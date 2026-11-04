package com.rescue.flutter_720yun.promotion

import com.google.gson.JsonParser
import com.rescue.flutter_720yun.network.AdoptionServiceCreator
import com.rescue.flutter_720yun.promotion.models.*
import org.junit.Assert.*
import org.junit.Test

class PromotionUiPolicyTest {
    @Test fun featureQuotaAndTerminalStateDisableWatchButOmittedSummaryAllowsPreflight() {
        assertTrue(PromotionUiPolicy.enabled(true,true,false,"open",null,false))
        val omitted=AdoptionServiceCreator.gson.fromJson("{}",PromotionSummary::class.java)
        assertNull(omitted.can_promote)
        assertTrue(PromotionUiPolicy.enabled(true,true,false,"open",omitted,false))
        assertFalse(PromotionUiPolicy.enabled(false,true,false,"open",null,false))
        assertFalse(PromotionUiPolicy.enabled(true,false,false,"open",null,false))
        assertFalse(PromotionUiPolicy.enabled(true,true,false,"closed",null,false))
        assertFalse(PromotionUiPolicy.enabled(true,true,false,"adopted",null,false))
        assertFalse(PromotionUiPolicy.enabled(true,true,true,"open",null,false))
        assertFalse(PromotionUiPolicy.enabled(true,true,false,"open",null,true))
        for(reason in listOf("FEATURE_DISABLED","DAILY_LIMIT_REACHED","TOPIC_NOT_OPEN")) {
            val summary=PromotionSummary(can_promote=false,reason=reason)
            assertFalse(PromotionUiPolicy.enabled(true,true,false,"open",summary,false))
        }
        assertEquals("推广暂未开放",PromotionUiPolicy.title(false,"open",PromotionSummary(can_promote=false,reason="FEATURE_DISABLED")))
        assertEquals("今日次数已用完",PromotionUiPolicy.title(false,"open",PromotionSummary(can_promote=false,reason="DAILY_LIMIT_REACHED")))
        assertEquals("完成领养",PromotionUiPolicy.title(false,"adopted",null))
        assertEquals("结束领养",PromotionUiPolicy.title(false,"closed",null))
        assertEquals("观看视频，延长推广",PromotionUiPolicy.title(false,"open",PromotionSummary(is_active=true,can_promote=true)))
    }
    @Test fun configuredDurationAcceptsOnlyPositiveIntegralNumbers() {
        fun value(json: String)=PromotionConfiguration.parse(JsonParser.parseString(json).asJsonObject)
        assertEquals(1800,value("""{"key":"BOOST_DURATION_SECONDS","value":1800}"""))
        assertNull(value("""{"key":"OTHER","value":1800}"""))
        for(input in listOf("true","false","\"1800\"","null","0","-1","2.5","99999999999999999999"))
            assertNull(value("""{"key":"BOOST_DURATION_SECONDS","value":$input}"""))
        assertEquals("30分钟",PromotionUiPolicy.duration(1800))
        assertEquals("1小时1分钟1秒",PromotionUiPolicy.duration(3661))
        assertEquals("1小时",PromotionUiPolicy.duration(0))
        assertEquals("1秒",PromotionUiPolicy.duration(1))
    }
    @Test fun anUnsubmittedRewardBlocksAnotherWatchEvenDuringBackoffOnlyForItsOriginalTarget() {
        val record=PendingReward(7,123,"my_posts","old",10,1000,next_retry_at=500)
        assertTrue(PromotionUiPolicy.pending(record,7,123,100))
        assertFalse(PromotionPolicy.recoverable(record,7,100))
        assertFalse(PromotionUiPolicy.pending(record,8,123,100))
        assertFalse(PromotionUiPolicy.pending(record,7,124,100))
        assertFalse(PromotionUiPolicy.pending(record,7,123,1000))
        assertFalse(PromotionUiPolicy.pending(record,7,123,5))
        assertFalse(PromotionPolicy.recoverable(record.copy(next_retry_at=0),7,5))
        assertFalse(PromotionUiPolicy.pending(record.copy(retry_state="stopped"),7,123,100))
    }
    @Test fun duplicateOrStaleLoadedCallbacksCannotCancelOrReplayAnAlreadyPlayingVideo() {
        assertTrue(PromotionUiPolicy.acceptsLoaded("preparing",true,true))
        for(phase in listOf("playing","reward_observed","submitting","succeeded","cancelled","failed"))
            assertFalse(PromotionUiPolicy.acceptsLoaded(phase,true,true))
        assertFalse(PromotionUiPolicy.acceptsLoaded("preparing",false,true))
        assertFalse(PromotionUiPolicy.acceptsLoaded("preparing",true,false))
    }
    @Test fun successMessageRequiresCurrentActiveStateAndValidExpiry() {
        val state=PromotionState(123,true,null,1800,null,PromotionSummary(is_active=true,effective_until="2026-11-04T12:00:00Z"),null,null)
        assertTrue(PromotionDisplay.confirmation(state).contains("30分钟"))
        assertTrue(PromotionDisplay.confirmation(state.copy(promotion=PromotionSummary(is_active=false))).contains("当前推广已结束"))
        assertEquals("推广请求已处理，请刷新查看当前状态",PromotionDisplay.confirmation(state.copy(promotion=PromotionSummary(is_active=true))))
        assertTrue(PromotionDisplay.confirmation(null).contains("暂时无法确认"))
    }
}
