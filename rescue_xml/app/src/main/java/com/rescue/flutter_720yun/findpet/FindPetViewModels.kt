package com.rescue.flutter_720yun.findpet

import androidx.lifecycle.MutableLiveData
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel

/** Transient invalidation signal only; read state comes from the V3 recommendation response. */
object FindPetChanges {
    val revision = MutableLiveData(0L)
    val requestRevision = MutableLiveData(0L)
    fun requestChanged() { requestRevision.value=(requestRevision.value ?: 0L)+1;changed() }
    var readWasUnread = false; private set
    fun changed(readWasUnread: Boolean = false) {
        this.readWasUnread=readWasUnread
        revision.value = (revision.value ?: 0L) + 1
    }
}
data class RecommendationDestination(val topicId: Int?, val available: Boolean)

class MyFindPetViewModel(val source: FindPetRepository = FindPetRepository()) : AdoptionViewModel() {
    val items = MutableLiveData<List<PetRecommendation>>(emptyList())
    val state = MutableLiveData<String?>()
    val unread = MutableLiveData<Int?>()
    val destination = MutableLiveData<RecommendationDestination?>()
    var page = 0; private set
    var hasMore = false; private set
    var initialized = false; private set
    var reading = false; private set
    fun load(refresh: Boolean = true) {
        if (reading || (!refresh && (busy.value == true || !hasMore))) return
        if (refresh) cancelWork()
        val next = if (refresh || !initialized) 1 else page + 1
        run {
            val result = source.recommendations(next)
            items.value = (if (next == 1) result.items.orEmpty() else items.value.orEmpty() + result.items.orEmpty()).distinctBy { it.recommendation_id }
            state.value = result.state; unread.value = result.unread_count
            page = next; hasMore = result.has_more == true; initialized = true
        }
    }
    fun open(item: PetRecommendation) = open(item.recommendation_id, item.topic_id, item.available)
    fun open(id: Int, topicId: Int, available: Boolean = true) {
        if (busy.value == true) return
        run {
            reading = true
            try {
                source.read(id)
                val wasUnread = items.value.orEmpty().any { it.recommendation_id == id && !it.read }
                items.value = items.value.orEmpty().map { if(it.recommendation_id==id) it.copy(read=true) else it }
                if (wasUnread) unread.value = unread.value?.let { (it - 1).coerceAtLeast(0) }
                FindPetChanges.changed(wasUnread)
                // Resolve external routes against current public history before opening the topic.
                // A subsequent refresh failure must not undo a successful read.
                val actual=source.recommendation(id)
                items.value = items.value.orEmpty().map { if (it.recommendation_id == id)
                    actual?.copy(read=true) ?: it.copy(read=true,topic_available=false,topic_status="unavailable",imgs=null,preview_img=null,content=null,city_name=null)
                    else it }
                val target=actual
                destination.value = RecommendationDestination(target?.topic_id?.takeIf { it>0 }, target?.available==true)
            } finally { reading = false }
        }
    }
    override fun clearUnavailableState() { /* A missing recommendation must not erase unrelated history. */ }
    override fun clearPrivateState() {
        items.value = emptyList(); state.value = null; unread.value = null; destination.value = null
        page = 0; hasMore = false; initialized = false; reading = false
    }
}

class PetRequestViewModel(val source: FindPetRepository = FindPetRepository()) : AdoptionViewModel() {
    val info = MutableLiveData<MyPetRequest?>()
    val saved = MutableLiveData(false)
    val writeCommitted = MutableLiveData(false)
    val refreshAfterWriteError = MutableLiveData<String?>()
    var loaded = false; private set
    var type = 1; var address = ""; var description = ""; var contact = ""; var cityId: String? = null
    private fun fill(result: MyPetRequest?, preserveDraft: Boolean = false) {
        if(!preserveDraft) {
            type = result?.pet_type ?: 1; address = result?.address.orEmpty(); description = result?.desc.orEmpty()
            contact = result?.contact.orEmpty(); cityId = result?.city_id
        }
        loaded=true;info.value=result
    }
    fun load() = run {
        if(writeCommitted.value==true) reconcile() else {
            loaded=false
            fill(source.info()?.takeIf { it.is_delete!=0 })
        }
    }
    fun validate(): String? = when {
        !loaded -> "请先成功加载找宠信息"
        type !in 1..2 -> "请选择宠物类型"
        description.isBlank() -> "请输入简短的描述"
        address.isBlank() -> "请选择地址"
        contact.isBlank() -> "请填写联系方式"
        else -> null
    }
    fun save() = run {
        if(writeCommitted.value==true) throw AdoptionError(400,message="操作已成功，请先刷新本人资料")
        validate()?.let { throw AdoptionError(400,message=it) }
        try { source.save(type,address,description.trim(),contact.trim(),cityId) }
        catch(e: AdoptionError) { resolveUnknown(e);throw e }
        writeCommitted.value=true;FindPetChanges.requestChanged();reconcile()
    }
    fun setEffective(open: Boolean) = run {
        if(!loaded || info.value==null || writeCommitted.value==true) throw AdoptionError(400,message="请先成功加载找宠信息")
        try { source.setEffective(open) } catch(e: AdoptionError) { resolveUnknown(e);throw e }
        writeCommitted.value=true;FindPetChanges.requestChanged();reconcile()
    }
    private suspend fun resolveUnknown(error: AdoptionError) {
        if(error.status>=500 || error.status==0) {
            loaded=false
            try { fill(source.info(),true) } catch(cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch(_: Exception) { }
            throw AdoptionError(0,message=if(loaded) "写入结果未知，已重新读取资料，请核对后再决定是否重试。" else "写入结果未知，请先重试读取本人资料，再决定是否提交。")
        }
    }
    private suspend fun reconcile() {
        loaded=false;refreshAfterWriteError.value=null
        try {
            val result=source.info() ?: throw AdoptionError(502,message="本人找宠信息暂不可用")
            fill(result);saved.value=true
        } catch(cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch(_: Exception) { refreshAfterWriteError.value="操作已成功，最新资料读取失败，请点击重试；无需再次提交。" }
    }
    override fun clearPrivateState() {
        info.value = null; saved.value = false; loaded = false;writeCommitted.value=false;refreshAfterWriteError.value=null
        type = 1; address = ""; description = ""; contact = ""; cityId = null
    }
}
