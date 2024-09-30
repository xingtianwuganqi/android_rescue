package com.rescue.flutter_720yun.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.ui.home.HomePagingSource

class HomeViewModel : ViewModel() {

    private var appService = ServiceCreator.create<AppService>()
    var items = Pager(PagingConfig(pageSize = 10)) {
        HomePagingSource(appService)
    }.flow.cachedIn(viewModelScope)

    // 手动刷新列表
    fun refreshPagingData() {
        items = Pager(PagingConfig(pageSize = 10)) {
            HomePagingSource(appService)
        }.flow.cachedIn(viewModelScope)
    }
//    private val _models = MutableLiveData<List<HomeListModel>>()
//    val models: LiveData<List<HomeListModel>> = _models
//
//    private fun getList(): List<Int> {
//        val list = List(10) {
//            it
//        }
//        return list
//    }
//
//    fun fetchData(loadCoach: Boolean) {
//        if (loadCoach) {
//            if (_models.value != null) {
//                return
//            }else{
//                loadData()
//            }
//        }else{
//            loadData()
//        }
//
//    }
//
//    fun loadData() {
//        val service = ServiceCreator.create<AppService>()
//        service.getTopicList(1, 20, 0).enqueue(object : Callback<BaseListResp<HomeListModel>>{
//            override fun onResponse(
//                p0: Call<BaseListResp<HomeListModel>>,
//                p1: Response<BaseListResp<HomeListModel>>
//            ) {
//                if (p1.isSuccessful && p1.body() != null) {
//                    _models.value = p1.body()!!.data
//                }else{
//
//                }
//            }
//
//            override fun onFailure(p0: Call<BaseListResp<HomeListModel>>, p1: Throwable) {
//
//            }
//        })
//    }
}