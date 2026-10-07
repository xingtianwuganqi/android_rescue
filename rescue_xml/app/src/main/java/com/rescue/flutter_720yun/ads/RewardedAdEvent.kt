package com.rescue.flutter_720yun.ads

sealed class RewardedAdEvent {
    class Loaded(val show: () -> Boolean): RewardedAdEvent()
    object Reward: RewardedAdEvent()
    object Closed: RewardedAdEvent()
    object PlayEnded: RewardedAdEvent()
    object Failed: RewardedAdEvent()
    object Destroyed: RewardedAdEvent()
}
