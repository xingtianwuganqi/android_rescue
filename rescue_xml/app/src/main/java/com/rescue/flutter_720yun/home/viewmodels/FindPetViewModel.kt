package com.rescue.flutter_720yun.home.viewmodels

import androidx.lifecycle.*
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import com.rescue.flutter_720yun.findpet.FindPetRepository
import com.rescue.flutter_720yun.home.models.FindPetModel
import com.rescue.flutter_720yun.util.*
import kotlinx.coroutines.*

data class FindPetPageChange(val added: Int, val refresh: Boolean)
class FindPetViewModel(
    val source: FindPetRepository = FindPetRepository(),
    private val blocked: ()->Set<String> = { SharedPreferencesUtil.getStringSet("find_pet_list",BaseApplication.context).orEmpty() }
) : ViewModel(), CommonViewModelInterface {
    val items=MutableLiveData<List<FindPetModel>>(emptyList())
    val uiState=MutableLiveData<UiState<List<FindPetModel>>>()
    private val loading=MutableLiveData(false)
    override val isLoading: LiveData<Boolean> = loading
    override val isFirstLoading: LiveData<Boolean> = MutableLiveData(true)
    private val last=MutableLiveData(false)
    override val isLastPage: LiveData<Boolean> = last
    private val refresh=MutableLiveData(RefreshState.REFRESH)
    override val refreshState: LiveData<RefreshState> = refresh
    val pageChange=MutableLiveData<FindPetPageChange?>()
    val errorMsg=MutableLiveData<String?>()
    val actionError=MutableLiveData<AdoptionError?>()
    val copiedContact=MutableLiveData<String?>()
    val operations=MutableLiveData<Set<String>>(emptySet())
    var collections=false
    var page=0; private set
    private var generation=0L
    private var job: Job?=null
    private val actions=mutableListOf<Job>()
    private var session=UserManager.sessionRevision.value
    private val observer=Observer<Long> { current ->
        if(current!=session) {
            session=current;stop();page=0;last.value=false;items.value=emptyList();uiState.value=UiState.Success(emptyList())
            if(!collections || UserManager.isLogin) findPetListNetworking(RefreshState.REFRESH)
        }
    }
    init { UserManager.sessionRevision.observeForever(observer) }
    fun findPetListNetworking(state: RefreshState) {
        val reload=state==RefreshState.REFRESH || page==0
        if(!reload && (loading.value==true || last.value==true)) return
        if(reload) { job?.cancel();generation++ }
        val request=generation;val next=if(reload) 1 else page+1
        loading.value=true;refresh.value=if(reload) RefreshState.REFRESH else RefreshState.MORE;errorMsg.value=null
        if(items.value.isNullOrEmpty()) uiState.value=UiState.FirstLoading
        job=viewModelScope.launch {
            try {
                val result=source.publicList(next,collections)
                if(request!=generation) return@launch
                val previous=if(reload) emptyList() else items.value.orEmpty()
                val visible=result.filter { it.effective==1 && it.is_delete==1 && ((UserManager.userId!=null && it.user_id==UserManager.userId) || it.findId.toString() !in blocked()) }
                val combined=(previous+visible).distinctBy { it.findId }
                page=next;last.value=result.isEmpty()
                items.value=combined;uiState.value=UiState.Success(combined)
                pageChange.value=FindPetPageChange(combined.size-previous.size,reload)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(request==generation) {
                if(items.value.isNullOrEmpty()) uiState.value=UiState.Error("网络加载失败\n请检查网络后重试")
                else errorMsg.value="网络加载失败，请稍后重试"
                if(e is AdoptionError && e.status==401) actionError.value=e
            } }
            finally { if(request==generation) loading.value=false }
        }
    }
    fun hidden(id: Int) { if(UserManager.userId!=null && items.value.orEmpty().any { it.findId==id && it.user_id==UserManager.userId }) return;items.value=items.value.orEmpty().filter { it.findId!=id };uiState.value=UiState.Success(items.value.orEmpty()) }
    fun interact(model: FindPetModel, collect: Boolean) = operation(model,if(collect) "collect" else "like") { id ->
        val selected=source.interact(id,collect,if(collect) model.collection!=true else model.liked!=true)
        update(id) { current -> if(collect) current.copy(collection=selected,collectionNum=((current.collectionNum ?: 0)+(if(selected) 1 else 0)-(if(current.collection==true) 1 else 0)).coerceAtLeast(0))
            else current.copy(liked=selected,likeNum=((current.likeNum ?: 0)+(if(selected) 1 else 0)-(if(current.liked==true) 1 else 0)).coerceAtLeast(0)) }
    }
    fun getFindPetContactNetworking(model: FindPetModel) = operation(model,"contact") { id ->
        val contact=source.contact(id)
        update(id) { it.copy(contact_info=contact,getedcontact=true) }
        copiedContact.value=contact
    }
    private fun operation(model: FindPetModel, kind: String, call: suspend (Int)->Unit) {
        val id=model.findId?.takeIf { it>0 } ?: return
        if(model.effective!=1 || model.is_delete!=1) { errorMsg.value="找宠信息已关闭或不可用";return }
        val key="$id:$kind"
        if(key in operations.value.orEmpty()) return
        val revision=UserManager.sessionRevision.value
        operations.value=operations.value.orEmpty()+key
        actions+=viewModelScope.launch {
            try { call(id) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(revision==UserManager.sessionRevision.value) {
                if(e is AdoptionError) {
                    actionError.value=e
                    if(kind=="contact") update(id) { it.copy(getedcontact=false,contact_info=null) }
                } else errorMsg.value="操作失败，请重试"
            } }
            finally { if(revision==UserManager.sessionRevision.value) operations.value=operations.value.orEmpty()-key }
        }
    }
    private fun update(id: Int, change: (FindPetModel)->FindPetModel) {
        items.value=items.value.orEmpty().map { if(it.findId==id) change(it) else it }
        uiState.value=UiState.Success(items.value.orEmpty())
    }
    fun stop() {
        generation++;job?.cancel();actions.forEach { it.cancel() };actions.clear()
        loading.value=false;operations.value=emptySet();copiedContact.value=null;actionError.value=null
    }
    override fun onCleared() { UserManager.sessionRevision.removeObserver(observer);stop();super.onCleared() }
}
