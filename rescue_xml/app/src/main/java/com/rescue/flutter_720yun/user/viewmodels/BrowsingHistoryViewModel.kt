package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel
import com.rescue.flutter_720yun.network.*
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.localized
import kotlinx.coroutines.CancellationException

data class BrowsingRecord(val id: Int, val topic: Int, val content: String, val address: String, val image: String?, val available: Boolean) {
    companion object {
        fun parse(value: JsonObject): BrowsingRecord? {
            val info=value.get("topicInfo")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            fun text(key: String)=runCatching { info[key]?.takeUnless { it.isJsonNull }?.asString }.getOrNull().orEmpty()
            val topic=text("topic_id").toIntOrNull()?.takeIf { it>0 } ?: return null
            val images=info.get("imgs")?.takeUnless { it.isJsonNull }
            val image=if(images?.isJsonArray==true) images.asJsonArray.mapNotNull { runCatching { it.asString }.getOrNull() }.firstOrNull { it.isNotBlank() }
                else runCatching { images?.asString?.split(',')?.firstOrNull { it.isNotBlank() } }.getOrNull()
            return BrowsingRecord(runCatching { value["history_id"].asInt }.getOrDefault(topic),topic,text("content"),text("address_info"),image,
                text("is_delete")!="0" && text("unavailable")!="true")
        }
    }
}
class BrowsingHistoryViewModel : AdoptionViewModel() {
    val rows=MutableLiveData<List<BrowsingRecord>>(emptyList())
    var page=0;private set
    var hasMore=true;private set
    fun load(refresh: Boolean=true) {
        if(busy.value==true || (!refresh && !hasMore)) return
        val next=if(refresh) 1 else page+1
        val revision=UserManager.sessionRevision.value
        run {
            val response=ServiceCreator.create<HomeService>().browsingHistory(mapOf("token" to UserManager.token,"page" to next,"size" to 10))
            if(revision!=UserManager.sessionRevision.value) throw CancellationException("Account changed")
            val body=response.body()
            if(response.code()==401 || body?.code==401) {
                UserManager.logout()
                org.greenrobot.eventbus.EventBus.getDefault().post(com.rescue.flutter_720yun.home.models.LoginEvent(null))
                throw CancellationException("Authentication expired")
            }
            if(!response.isSuccessful || body?.code!=200) throw AdoptionError(response.code(),message=body?.message ?: localized(R.string.load_failed_title,"网络加载失败"),code=body?.code ?: response.code())
            val raw=body.data
            val incoming=raw.mapNotNull(BrowsingRecord::parse)
            rows.value=if(refresh) incoming else rows.value.orEmpty()+incoming
            page=next;hasMore=raw.size>=10
        }
    }
    override fun clearPrivateState() { rows.value=emptyList();page=0;hasMore=true }
}
