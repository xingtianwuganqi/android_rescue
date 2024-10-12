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
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private var page: Int = 1

    private val _models = MutableLiveData<List<HomeListModel>>()
    private val _isLastPage = MutableLiveData(false)
    private val _isLoading = MutableLiveData(false)
    private val _changedModel = MutableLiveData<HomeListModel?>()
    private val _errorMsg = MutableLiveData<String>()

    val models: LiveData<List<HomeListModel>> = _models
    val isLastPage: LiveData<Boolean> = _isLastPage
    val isLoading: LiveData<Boolean> = _isLoading
    val changeModel: LiveData<HomeListModel?> = _changedModel
    val errorMsg: LiveData<String> = _errorMsg

    private val appService = ServiceCreator.create<AppService>()

    fun loadListData(refresh: RefreshState) {
        viewModelScope.launch {
            if (_isLoading.value == true) {
                return@launch
            }
            if (refresh == RefreshState.MORE && _isLastPage.value == true) {
                return@launch
            }
            if (refresh == RefreshState.REFRESH) {
                page = 1
                _isLastPage.value = false
            }
            _isLoading.value = true
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
                _isLastPage.value = true
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun likeActionNetworking(model: HomeListModel?) {

        viewModelScope.launch {
            if (_isLoading.value == true) {
                return@launch
            }
            try {
                var likeMark = if (model?.liked == true) 0 else 1
                val dic = paramDic
                dic["like_mark"] = likeMark
                val response = appService.topicLikeAction(dic).awaitResponse()
                if (response.code == 200) {
                    model?.liked = !(model?.liked ?: false)
                    _changedModel.value = model
                }else{
                    _errorMsg.value = "点赞失败"
                }
            }catch (e: Exception) {
                _errorMsg.value = "点赞失败"
            }finally {
                _isLoading.value = true
            }
        }

    }

    fun loadingFinish() {
        _isLoading.value = false
    }

    fun cleanChangedModel(){
        _changedModel.value = null
    }

}