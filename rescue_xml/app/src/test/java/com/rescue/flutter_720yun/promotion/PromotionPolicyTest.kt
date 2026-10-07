package com.rescue.flutter_720yun.promotion

import com.rescue.flutter_720yun.promotion.models.*
import org.junit.Test
import org.junit.Assert.*

class PromotionPolicyTest {
    @Test fun closedAndFinishedDoNotCreateRewardsButLateRealRewardRetainsOriginalTarget() {
        val attempt=RewardObservation(7,123,"publish_success","original-key")
        assertNull(attempt.receive(RewardObservation.Signal.PLAY_ENDED,10))
        assertNull(attempt.receive(RewardObservation.Signal.CLOSED,11))
        assertNull(attempt.receive(RewardObservation.Signal.FAILED,12))
        val record=attempt.receive(RewardObservation.Signal.REWARD,13)!!
        assertEquals(7,record.account_id);assertEquals(123,record.topic_id)
        assertEquals("publish_success",record.scene);assertEquals("original-key",record.reward_attempt_id)
        assertNull(attempt.receive(RewardObservation.Signal.REWARD,14))
        assertEquals(13+PromotionPolicy.RECOVERY_WINDOW,record.retry_until)
    }
    @Test fun recoveryRequiresOriginalAccountWindowAndBackoff() {
        val record=PendingReward(7,123,"my_posts","key",1,1000,next_retry_at=100)
        assertFalse(PromotionPolicy.recoverable(record,8,200))
        assertFalse(PromotionPolicy.recoverable(record,7,50))
        assertTrue(PromotionPolicy.recoverable(record,7,100))
        assertFalse(PromotionPolicy.recoverable(record,7,1000))
        assertFalse(PromotionPolicy.recoverable(record.copy(retry_state="stopped"),7,200))
        assertTrue(PromotionPolicy.retryable(503));assertTrue(PromotionPolicy.retryable(429))
        assertFalse(PromotionPolicy.retryable(409));assertFalse(PromotionPolicy.retryable(403))
    }
}
