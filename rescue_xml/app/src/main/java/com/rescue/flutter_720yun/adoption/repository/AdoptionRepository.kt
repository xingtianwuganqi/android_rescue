package com.rescue.flutter_720yun.adoption.repository

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.network.AdoptionService
import com.rescue.flutter_720yun.network.AdoptionServiceCreator
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.CancellationException
import retrofit2.Response

class AdoptionError(val http: Int, val errorCode: String? = null, override val message: String,
    val fields: Map<String, String> = emptyMap(), val code: Int = http, val retryAfter: Long? = null) : Exception(message) { val status: Int get()=if(http==200) code else http }

object AdoptionErrors {
    fun parse(http: Int, text: String?): AdoptionError {
        val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
        val data = runCatching { json?.getAsJsonObject("data") }.getOrNull()
        fun string(obj: JsonObject?, key: String): String? = runCatching { obj?.get(key)?.takeUnless { it.isJsonNull }?.asString }.getOrNull()
        val errors = runCatching { data?.getAsJsonObject("errors")?.entrySet()?.associate { (key, value) ->
            key to if(value.isJsonArray) value.asJsonArray.joinToString("；") { it.asString } else value.asString
        } }.getOrNull().orEmpty()
        return AdoptionError(http, string(data, "error_code"), string(json, "message") ?: when(http) {
            401 -> "登录已失效，请重新登录"; 403 -> "当前账号无权执行此操作"; 404 -> "资源或新版服务暂不可用"
            409 -> "状态已变化，请刷新后重试"; else -> "服务暂不可用，请稍后重试"
        }, errors, code=runCatching { json?.get("code")?.asInt }.getOrNull() ?: http,
            retryAfter=runCatching { data?.get("retry_after")?.asLong }.getOrNull())
    }
}

data class AdoptionAccount(val userId: Int?, val token: String?, val revision: Long?)

class AdoptionRepository(
    private val service: AdoptionService = AdoptionServiceCreator.service,
    private val account: () -> AdoptionAccount = { AdoptionAccount(UserManager.userId, UserManager.token, UserManager.sessionRevision.value) },
    private val invalidateLogin: () -> Unit = {
        UserManager.logout()
        org.greenrobot.eventbus.EventBus.getDefault().post(com.rescue.flutter_720yun.home.models.LoginEvent(null))
    }
) {
    private suspend fun <T> request(call: suspend (String) -> Response<V2Response<T>>): T {
        val identity = account()
        val token = identity.token ?: throw AdoptionError(401, message = "请先登录")
        val response = try { call("Bearer $token") } catch(e: com.google.gson.JsonParseException) {
            if(identity != account()) throw CancellationException("Account changed")
            throw AdoptionError(502,message="服务暂不可用，请稍后重试")
        } // Retrofit suspend requests cancel the underlying Call.
        if (identity != account())
            throw CancellationException("Account changed")
        val body = response.body()
        if(response.code()==200 && body==null) throw AdoptionError(502,message="服务暂不可用，请稍后重试")
        if (response.code() != 200 || body?.code != 200) {
            val error = if(response.isSuccessful && body != null)
                AdoptionErrors.parse(response.code(), com.google.gson.JsonObject().apply {
                    addProperty("code",body.code);addProperty("message",body.message);add("data",body.errorData)
                }.toString())
            else AdoptionErrors.parse(response.code(), response.errorBody()?.string())
            if(error.status == 401) invalidateLogin()
            throw error
        }
        return body.data ?: throw AdoptionError(502, message = "服务返回了无效数据，请重试")
    }
    suspend fun preview(topic: Int): String? {
        val identity=account()
        val params=mutableMapOf<String,Any?>("topic_id" to topic)
        identity.token?.let { params["token"]=it }
        val result=com.rescue.flutter_720yun.network.ServiceCreator.create<com.rescue.flutter_720yun.network.HomeService>().topicPreview(params)
        ensureAccount(identity)
        if(result.code()!=200 || result.body()?.code!=200) return null
        return (result.body()?.data?.imgs.orEmpty()+result.body()?.data?.preview_img.orEmpty()).firstOrNull { it.isNotBlank() }
    }
    suspend fun profile() = request { service.profile(it) }
    suspend fun saveProfile(body: ProfileWrite) = request { service.saveProfile(it, body) }
    suspend fun state(topic: Int) = request { service.state(it, topic) }
    suspend fun apply(topic: Int, key: String, body: ApplicationWrite) = request { service.apply(it, topic, key, body) }
    suspend fun received(topic: Int?, status: String, page: Int) = request { service.received(it, topic, status.takeIf(String::isNotBlank), page) }
    suspend fun application(id: Int) = request { service.application(it, id) }
    suspend fun action(id: Int, body: ApplicationAction, key: String? = null) = request { service.action(it, id, body, key) }
    suspend fun mine(status: String, page: Int) = request { service.mine(it, status.takeIf { value -> value.isNotBlank() }, page) }
    suspend fun topicAction(topic: Int, body: TopicAction, key: String = java.util.UUID.randomUUID().toString()) = request { service.topicAction(it, topic, body, key) }
    suspend fun notifications(page: Int) = request { service.notifications(it, page) }
    suspend fun unread() = request { service.unread(it) }
    suspend fun read(id: Int) = request { service.read(it, id) }

    private fun ensureAccount(identity: AdoptionAccount) {
        if(identity != account()) throw CancellationException("Account changed")
    }
    /** Raw counts cannot express client visibility. Load every page before publishing counts. */
    private suspend fun visibleUnread(): List<AdoptionNotification> {
        val identity=account()
        val notifications=linkedMapOf<Int,AdoptionNotification>()
        var page=1
        do {
            ensureAccount(identity)
            val batch=request { service.notifications(it,page++,unread=true) }
            val items=batch.items ?: throw AdoptionError(502,message="提醒数据暂不可用，请点击重试")
            items.forEach { notifications[it.id]=it }
            val more=batch.has_more
        } while(more)
        val own=mutableSetOf<Int>()
        if(notifications.values.any { !it.read && it.role !in listOf("mine","received") }) {
            page=1
            do {
                ensureAccount(identity)
                val batch=mine("",page++) // Include terminal applications and all history pages.
                own.addAll((batch.items ?: throw AdoptionError(502,message="申请归属暂不可用，请点击重试")).map { it.application_id })
                val more=batch.has_more
            } while(more)
        }
        ensureAccount(identity)
        return NotificationClassification.visibleUnread(NotificationClassification.classify(notifications.values.toList(),own))
    }
    suspend fun classifiedUnread(): UnreadCount = NotificationClassification.counts(visibleUnread())
    suspend fun reminderPage(role: String, topic: Int?, page: Int): ReminderPage {
        require(role in listOf("mine","received") && page>0)
        val identity=account()
        val filtered=visibleUnread().filter { it.role==role && (topic==null || it.topic_id==topic) }
        ensureAccount(identity)
        val start=(page-1).toLong()*10
        return ReminderPage(if(start>=filtered.size) emptyList() else filtered.drop(start.toInt()).take(10),
            page,start+10<filtered.size,NotificationClassification.counts(filtered))
    }
}
