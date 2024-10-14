package com.rescue.flutter_720yun.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

class HomeViewModel : ViewModel() {

    private var page: Int = 1

    private val _models = MutableLiveData<List<HomeListModel>>()
    private val _isLastPage = MutableLiveData(false)
    private val _isLoading = MutableLiveData(false)
    private val _changedModel = MutableLiveData<HomeListModel?>()
    private val _errorMsg = MutableLiveData<String>()
    private val _isRefreshing = MutableLiveData(false)

    val models: LiveData<List<HomeListModel>> = _models
    val isLastPage: LiveData<Boolean> = _isLastPage
    val isLoading: LiveData<Boolean> = _isLoading
    val changeModel: LiveData<HomeListModel?> = _changedModel
    val errorMsg: LiveData<String> = _errorMsg
    val isRefreshing: LiveData<Boolean> = _isRefreshing

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
                _isRefreshing.value = true
            }
            _isLoading.value = true
            try {
                val service = ServiceCreator.create<AppService>()
                val dic = paramDic
                dic["page"] = page
                dic["size"] = 10
                dic["order"] = 0
                Log.d("TAG", "dic is $dic")
                val response = service.getTopicList(dic).awaitResp()
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
                val likeMark = if (model?.liked == true) 0 else 1
                val dic = paramDic
                dic["like_mark"] = likeMark
                dic["topic_id"] = model?.topic_id
                val response = appService.topicLikeAction(dic).awaitResp()
                if (response.code == 200) {
                    model?.liked = response.data?.mark == 1
                    _changedModel.value = model
                }else{
                    val msg = BaseApplication.context.resources.getString(R.string.like_error)
                    _errorMsg.value = msg
                }
            }catch (e: Exception) {
                _errorMsg.value = "点赞失败"
            }finally {
                _isLoading.value = true
            }
        }

    }

    fun collectionActionNetworking(model: HomeListModel?) {
        viewModelScope.launch {
            if (_isLoading.value == true) {
                return@launch
            }
            try {
                val collectMark = if (model?.collectioned == true) 0 else 1
                val topicId = model?.topic_id
                val dic = paramDic
                dic["collect_mark"] = collectMark
                dic["topic_id"] = topicId
                val response = appService.topicCollectionAction(dic).awaitResp()
                if (response.code == 200) {
                    model?.collectioned = response.data?.mark == 1
                    _changedModel.value = model
                }else{
                    _errorMsg.value = BaseApplication.context.getString(R.string.collect_error)
                }

            }catch (e: Exception) {
                _errorMsg.value = BaseApplication.context.getString(R.string.network_request_error)
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadItem(model: HomeListModel?) {
        _changedModel.value = model
    }

    fun cleanChangedModel(){
        _changedModel.value = null
    }

    fun cleanIsRefreshing() {
        _isRefreshing.value = false
    }
}