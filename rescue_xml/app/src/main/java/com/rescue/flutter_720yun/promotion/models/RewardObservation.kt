package com.rescue.flutter_720yun.promotion.models

/** An immutable SDK attempt may observe its reward after the presenter has closed. */
class RewardObservation(private val account: Int,private val topic: Int,private val scene: String,private val key: String) {
    enum class Signal { REWARD, CLOSED, PLAY_ENDED, FAILED }
    var observed=false
        private set
    fun receive(signal: Signal,now: Long): PendingReward? {
        if(signal!=Signal.REWARD || observed) return null
        observed=true
        return PendingReward(account,topic,scene,key,now,now+PromotionPolicy.RECOVERY_WINDOW)
    }
}
