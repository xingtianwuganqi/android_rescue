package com.rescue.flutter_720yun.findpet

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.adoption.models.V2Response
import com.rescue.flutter_720yun.adoption.repository.AdoptionAccount
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import com.rescue.flutter_720yun.adoption.repository.AdoptionErrors
import com.rescue.flutter_720yun.network.V2EnvelopeAdapter
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object FindPetServices {
    val gson = GsonBuilder().registerTypeAdapterFactory(V2EnvelopeAdapter()).create()
    val service: FindPetService by lazy {
        Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create(gson)).build().create(FindPetService::class.java)
    }
}
class FindPetRepository(
    private val service: FindPetService = FindPetServices.service,
    private val account: () -> AdoptionAccount = { AdoptionAccount(UserManager.userId, UserManager.token, UserManager.sessionRevision.value) },
    private val invalidate: () -> Unit = { UserManager.logout(); org.greenrobot.eventbus.EventBus.getDefault().post(com.rescue.flutter_720yun.home.models.LoginEvent(null)) }
) {
    private fun identity(): AdoptionAccount = account().also {
        if (it.token.isNullOrBlank() || it.userId == null) throw AdoptionError(401, message = "请先登录")
    }
    private fun current(identity: AdoptionAccount) {
        if (identity != account()) throw CancellationException("Account changed")
    }
    private fun fail(identity: AdoptionAccount, error: AdoptionError): Nothing {
        current(identity)
        if (error.status == 401) invalidate()
        throw error
    }
    private suspend fun <T> request(call: suspend (String) -> Response<V2Response<T>>): T {
        val identity = identity()
        val response = try { call("Bearer ${identity.token}") }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { current(identity); throw AdoptionError(503, message = "推荐服务暂不可用，请稍后重试") }
        current(identity)
        val body = response.body()
        if (response.code() != 200 || body?.code != 200) {
            val text = if (body != null) JsonObject().apply { addProperty("code", body.code); addProperty("message", body.message); add("data", body.errorData) }.toString()
                else response.errorBody()?.string()
            fail(identity, AdoptionErrors.parse(response.code(), text))
        }
        return body.data ?: throw AdoptionError(502, message = "推荐数据无效，请重试")
    }
    suspend fun recommendations(page: Int = 1): PetRecommendations {
        require(page > 0)
        return request { service.recommendations(it, page) }.also { data ->
            if (data.state !in listOf("no_request", "open", "closed") || data.page != page || data.items == null || data.has_more == null || data.unread_count == null || data.unread_count < 0 || data.items.any { it.recommendation_id <= 0 })
                throw AdoptionError(502, message = "推荐数据无效，请重试")
        }
    }
    suspend fun read(id: Int): RecommendationRead {
        if (id <= 0) throw AdoptionError(400, message = "推荐参数无效")
        return request { service.read(it, id, JsonObject()) }.also {
            if (it.recommendation_id != id || !it.read) throw AdoptionError(502, message = "已读结果无效，请重试")
        }
    }
    private suspend fun legacy(allowMissing: Boolean = false, authenticated: Boolean = true, extra: Map<String, Any?> = emptyMap(), call: suspend (Map<String, Any?>) -> Response<JsonObject>): JsonObject? {
        val identity = if(authenticated) identity() else account()
        val fields=com.rescue.flutter_720yun.util.params.toMutableMap()
        identity.token?.takeIf { it.isNotBlank() }?.let { fields["token"]=it }
        val response = try { call(fields + extra) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { current(identity); throw AdoptionError(503, message = "找宠服务暂不可用，请重试") }
        current(identity)
        val json = response.body()
        val code = runCatching { json?.get("code")?.asInt }.getOrNull()
        if (response.code() == 200 && allowMissing && code == 207) return null
        if (response.code() != 200 || code != 200) {
            val error=AdoptionErrors.parse(response.code(), json?.toString() ?: response.errorBody()?.string())
            // Old Form APIs may return HTTP403 with business code401 for an expired token.
            // Normalize only that legacy authentication result; V2/V3 permission errors keep their HTTP status.
            val normalized=if(error.code==401 && error.status!=401)
                AdoptionError(401,error.errorCode,error.message,error.fields,401,error.retryAfter)
            else error
            fail(identity,normalized)
        }
        return json
    }
    suspend fun info(): MyPetRequest? {
        val result = legacy(allowMissing = true, call = service::info) ?: return null
        return runCatching { FindPetServices.gson.fromJson(result.get("data"), MyPetRequest::class.java) }.getOrNull()
            ?.takeIf { it.id > 0 } ?: throw AdoptionError(502, message = "找宠资料无效，请重试")
    }
    suspend fun save(type: Int, address: String, description: String, contact: String, cityId: String?) {
        val fields = mutableMapOf<String, Any?>("pet_type" to type, "address" to address, "desc" to description, "contact" to contact)
        cityId?.takeIf { it.isNotBlank() }?.let { fields["city_id"] = it }
        legacy(extra = fields, call = service::save)
    }
    suspend fun setEffective(open: Boolean) { legacy(extra = mapOf("effective" to if (open) 1 else 2), call = service::effective) }
    suspend fun publicList(page: Int, collections: Boolean = false): List<com.rescue.flutter_720yun.home.models.FindPetModel> {
        require(page>0)
        val data=legacy(authenticated=collections,extra=mapOf("page" to page,"size" to 10),call=if(collections) service::collections else service::list)?.get("data")
        if(data?.isJsonObject==true && data.asJsonObject.size()==0) return emptyList()
        if(data?.isJsonArray!=true) throw AdoptionError(502,message="找宠列表无效，请重试")
        return data.asJsonArray.mapNotNull { entry ->
            val raw=if(collections) entry.asJsonObject.get("findInfo")?.takeUnless { it.isJsonNull } ?: JsonObject().apply {
                add("findId",entry.asJsonObject.get("collect_id"));addProperty("effective",2);addProperty("is_delete",0)
            } else entry
            raw?.let { FindPetServices.gson.fromJson(it,com.rescue.flutter_720yun.home.models.FindPetModel::class.java) }?.also {
                if((it.findId ?: 0)<=0) throw AdoptionError(502,message="找宠信息编号无效")
                it.contact=null
                if(it.effective!=1 || it.is_delete!=1) { it.contact_info=null;it.getedcontact=false }
            }
        }
    }
    suspend fun interact(id: Int, collect: Boolean, selected: Boolean): Boolean {
        if(id<=0) throw AdoptionError(400,message="找宠信息编号无效")
        val mark=if(selected) 1 else 0
        val data=legacy(extra=mapOf("find_id" to id,(if(collect) "collect_mark" else "like_mark") to mark),call=if(collect) service::collect else service::like)?.getAsJsonObject("data")
        val result=runCatching { data?.get(if(collect) "collection" else "like")?.asInt }.getOrNull()
        if(result !in listOf(0,1)) throw AdoptionError(502,message="操作结果无效，请刷新")
        return result==1
    }
    suspend fun contact(id: Int): String {
        if(id<=0) throw AdoptionError(400,message="找宠信息编号无效")
        val data=legacy(extra=mapOf("topic_id" to id,"topic_type" to 2),call=service::contact)?.getAsJsonObject("data")
        return data?.get("contact")?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
            ?: throw AdoptionError(502,message="暂无可用的联系方式")
    }
    suspend fun recommendation(id: Int): PetRecommendation? {
        if(id<=0) throw AdoptionError(400,message="推荐参数无效")
        var page=1
        do {
            val data=recommendations(page)
            data.items.orEmpty().firstOrNull { it.recommendation_id==id }?.let { return it }
            if(data.has_more!=true) return null
            page++
        } while(true)
    }
    suspend fun notifications(): List<com.rescue.flutter_720yun.message.models.MessageSystemListModel> {
        val json=legacy(call=service::notifications) ?: throw AdoptionError(502,message="系统通知无效，请重试")
        val data=json.get("data")
        if(data?.isJsonObject==true && data.asJsonObject.size()==0) return emptyList()
        if(data?.isJsonArray!=true) throw AdoptionError(502,message="系统通知无效，请重试")
        return FindPetServices.gson.fromJson(data,Array<com.rescue.flutter_720yun.message.models.MessageSystemListModel>::class.java).toList()
    }
    suspend fun systemUnread(): com.rescue.flutter_720yun.message.models.MessageUnreadModel {
        val data=legacy(call=service::systemUnread)?.get("data")
        if(data?.isJsonObject!=true) throw AdoptionError(502,message="消息统计暂不可用")
        val counts=FindPetServices.gson.fromJson(data,com.rescue.flutter_720yun.message.models.MessageUnreadModel::class.java)
        return counts.copy(find_pet_unread=(counts.find_pet_unread ?: 0).coerceAtLeast(0))
    }
}
