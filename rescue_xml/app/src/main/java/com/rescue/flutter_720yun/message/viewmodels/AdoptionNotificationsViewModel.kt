package com.rescue.flutter_720yun.message.viewmodels
import androidx.lifecycle.MutableLiveData
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel
class AdoptionNotificationsViewModel:AdoptionViewModel() {
    val items=MutableLiveData<List<AdoptionNotification>>(emptyList())
    var page=0;var hasMore=true
    fun load(refresh:Boolean=true) {
        if(refresh) { cancelWork();items.value=emptyList();page=0;hasMore=true }
        if(!hasMore) return
        val next=page+1
        run { val data=repository.notifications(next);items.value=if(refresh) data.items.orEmpty() else items.value.orEmpty()+data.items.orEmpty()
            page=next;hasMore=data.has_more }
    }
    fun read(item:AdoptionNotification,go:()->Unit)=run { repository.read(item.id)
        items.value=items.value.orEmpty().map { if(it.id==item.id) it.copy(read=true) else it };go() }
    override fun clearPrivateState() { items.value=emptyList();page=0;hasMore=true }
}
