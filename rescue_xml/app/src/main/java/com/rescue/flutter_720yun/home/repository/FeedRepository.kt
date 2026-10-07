package com.rescue.flutter_720yun.home.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.home.models.*
import com.rescue.flutter_720yun.network.*
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.CancellationException

data class FeedPage(val items: List<HomeListModel>,val meta: FeedMeta)
class FeedRepository(private val service: HomeService=ServiceCreator.create()) {
    suspend fun page(city: String?,page: Int,snapshot: String?): FeedPage {
        val identity=AdoptionAccount(UserManager.userId,UserManager.token,UserManager.sessionRevision.value)
        val params=mutableMapOf<String,Any?>("page" to page,"size" to 10,"promotion_feed" to true)
        identity.token?.let { params["token"]=it }
        city?.let { params["address"]=it }
        snapshot?.let { params["snapshot_id"]=it }
        val response=if(city==null) service.promotionFeed(params) else service.localPromotionFeed(params)
        if(identity!=AdoptionAccount(UserManager.userId,UserManager.token,UserManager.sessionRevision.value)) throw CancellationException("Account changed")
        val body=response.body()
        if(response.code()!=200 || body?.code!=200) {
            val error=AdoptionErrors.parse(response.code(),if(response.isSuccessful) Gson().toJson(body) else response.errorBody()?.string())
            if(error.http==401 || error.code==401) {
                UserManager.logout();org.greenrobot.eventbus.EventBus.getDefault().post(LoginEvent(null))
            }
            throw error
        }
        val items=if(body.data?.isJsonArray==true) Gson().fromJson<List<HomeListModel>>(body.data,object:TypeToken<List<HomeListModel>>(){}.type) else emptyList()
        val meta=body.meta ?: throw AdoptionError(502,message="推广列表服务暂不可用，请稍后重试")
        return FeedPage(items,meta)
    }
}
