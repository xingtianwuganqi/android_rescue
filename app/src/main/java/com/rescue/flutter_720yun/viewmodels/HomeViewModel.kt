package com.rescue.flutter_720yun.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResponse
import com.rescue.flutter_720yun.ui.home.HomePagingSource
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic

class HomeViewModel : ViewModel() {

    private var appService = ServiceCreator.create<AppService>()
    private var page: Int = 1


    private val _models = MutableLiveData<List<HomeListModel>>()
    private val _isLastPage = MutableLiveData(false)
    private val _isLoading = MutableLiveData(false)

    val models: LiveData<List<HomeListModel>> = _models
    val isLastPage: LiveData<Boolean> = _isLastPage
    val isLoading: LiveData<Boolean> = _isLoading

    suspend fun loadListData(refresh: RefreshState) {
        if (_isLoading.value == true) {
            return
        }
        if (refresh == RefreshState.MORE && _isLastPage.value == true) {
            return
        }
        if (refresh == RefreshState.REFRESH) {
            page = 1
            _isLastPage.value = false
        }
        _isLoading.value = true
        Log.d("TAG", "load list page is $page")
        try {
            val service = ServiceCreator.create<AppService>()
            var response = service.getTopicList(page, 10, 0).awaitResponse()
            if (response.code == 200) {
                val items = when (response.data) {
                    is List<*> -> {
                        val homeList = convertAnyToList(response.data, HomeListModel::class.java)
                        (homeList ?: emptyList())
                    }
                    is Map<*, *> -> {
                        emptyList()
                    }// data 为 {}，返回空列表
                    else -> {
                        emptyList()
                    }
                }
                Log.d("TAG","fuck first Id is ${items.first().topic_id}")
                if (items.isNotEmpty()) {
                    page += 1
                    _models.value = items.toList()
                }else{
                    _isLastPage.value = true
                }
            }else{
                _models.value = emptyList()
            }
        }catch (e: Exception) {
            Log.d("TAG", "load list error $e")
            _isLastPage.value = true
        }finally {
            _isLoading.value = false
        }


    }

    suspend fun likeActionNetworking(model: HomeListModel) {
        var like_mark = if (model.liked == true) 1 else 0
        val dic = paramDic
        dic["like_mark"] = like_mark
        val response = appService.topicLikeAction(dic).awaitResponse()
        if (response.code == 200) {
            model.liked = !(model.liked ?: false)
        }else{

        }
    }

    fun loadingFinish() {
        _isLoading.value = false
    }
}