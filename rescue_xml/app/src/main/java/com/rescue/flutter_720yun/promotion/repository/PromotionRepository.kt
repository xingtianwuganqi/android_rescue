package com.rescue.flutter_720yun.promotion.repository

import com.rescue.flutter_720yun.adoption.models.V2Response
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.network.*
import com.rescue.flutter_720yun.promotion.models.*
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.CancellationException
import retrofit2.Response

class PromotionRepository(private val service: PromotionService=AdoptionServiceCreator.retrofit.create(PromotionService::class.java),
    private val account: () -> AdoptionAccount={ AdoptionAccount(UserManager.userId,UserManager.token,UserManager.sessionRevision.value) },
    private val invalidate: () -> Unit={ UserManager.logout(); org.greenrobot.eventbus.EventBus.getDefault().post(com.rescue.flutter_720yun.home.models.LoginEvent(null)) }) {
    private suspend fun <T> request(call: suspend (String)->Response<V2Response<T>>): T {
        val identity=account()
        val token=identity.token ?: throw AdoptionError(401,message="请先登录")
        val response=try { call("Bearer $token") } catch(e: com.google.gson.JsonParseException) {
            if(identity!=account()) throw CancellationException("Account changed")
            throw AdoptionError(502,message="服务暂不可用，请稍后重试")
        }
        if(identity!=account()) throw CancellationException("Account changed")
        val body=response.body()
        if(response.code()==200 && body==null) throw AdoptionError(502,message="服务暂不可用，请稍后重试")
        if(response.code()!=200 || body?.code!=200) {
            val text=if(response.isSuccessful) com.google.gson.JsonObject().apply {
                addProperty("code",body?.code);addProperty("message",body?.message);add("data",body?.errorData)
            }.toString() else response.errorBody()?.string()
            val error=AdoptionErrors.parse(response.code(),text)
            if(error.http==401 || error.code==401) invalidate()
            throw AdoptionError(error.http,error.errorCode,error.message,error.fields,error.code,
                response.headers()["Retry-After"]?.toLongOrNull() ?: error.retryAfter)
        }
        return body.data ?: throw AdoptionError(502,message="服务暂不可用，请稍后重试")
    }
    suspend fun state(topic: Int): PromotionState {
        require(topic>0)
        val state=request { service.state(it,topic) }
        if(state.topic_id!=topic) throw AdoptionError(502,message="服务返回了无效推广状态，请重试")
        return state
    }
    suspend fun submit(record: PendingReward): PromotionResult {
        require(record.account_id==account().userId && record.topic_id>0 && record.scene in listOf("my_posts","publish_success"))
        java.util.UUID.fromString(record.reward_attempt_id)
        val result=request { service.promote(it,record.topic_id,record.reward_attempt_id,PromotionWrite(record.reward_attempt_id,record.scene)) }
        if(result.topic_id!=record.topic_id || result.promotion_id<=0) throw AdoptionError(502,message="服务返回了无效推广结果，请稍后重试")
        return result
    }
}
