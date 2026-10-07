package com.rescue.flutter_720yun.user.viewmodels

import android.app.Service
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import com.google.gson.reflect.TypeToken
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.viewmodels.HomeViewModel
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.show.models.GambitListModel
import com.rescue.flutter_720yun.show.models.ShowPageModel
import com.rescue.flutter_720yun.util.CommonViewModelInterface
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class UserTopicViewModel<T>: ViewModel(), CommonViewModelInterface {

    val appService = ServiceCreator.create<UserService>()

    private val _isLoading = MutableLiveData(false)
    private val _isFirstLoading = MutableLiveData(false)
    private val _isLastPage = MutableLiveData(false)
    private val _refreshState = MutableLiveData<RefreshState>()
    private val _uiState = MutableLiveData<UiState<List<Any>>>()
    private val _errorMsg = MutableLiveData<String>()

    override val isLoading: LiveData<Boolean>
        get() = _isLoading

    override val isFirstLoading: LiveData<Boolean>
        get() = _isFirstLoading

    override val isLastPage: LiveData<Boolean>
        get() = _isLastPage

    override val refreshState: LiveData<RefreshState>
        get() = _refreshState

    val uiState: LiveData<UiState<List<Any>>> get() = _uiState
    val errorMsg: LiveData<String> get() = _errorMsg

    var userId: Int? = null

    var from: Int? = null // 0: 帖子，1:秀宠
    private var page: Int = 1

    fun loadDataNetworking(refresh: RefreshState) {
        if (from == 0) {
            loadUserTopicListNetworking(refresh)
        }else{
            loadUserShowListNetworking(refresh)
        }
    }

    private fun loadUserTopicListNetworking(refresh: RefreshState) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                if (refresh == RefreshState.REFRESH) {
                    page = 1
                    _isLastPage.value = false
                }
                if (refresh == RefreshState.MORE && _isLastPage.value == true) {
                    return@launch
                }
                if (_isFirstLoading.value == true) {
                    _uiState.value = UiState.FirstLoading
                }
                _isLoading.value = true
                _refreshState.value = refresh
                val dic = paramDic
                dic["page"] = page
                dic["size"] = 10
                dic["userId"] = userId

                val session=com.rescue.flutter_720yun.util.UserManager.sessionRevision.value
                val requestedUser=userId
                val own=requestedUser!=null && requestedUser==com.rescue.flutter_720yun.util.UserManager.userId
                val response = (if(own) appService.ownPublishNetworking(dic) else appService.userPublishNetworking(dic)).awaitResp()
                if(session!=com.rescue.flutter_720yun.util.UserManager.sessionRevision.value || requestedUser!=userId) return@launch
                _isFirstLoading.value = false
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data,
                                    HomeListModel::class.java)
                            (homeList ?: emptyList())

                        }
                        is Map<*, *> -> {
                            emptyList()
                        }// data 为 {}，返回空列表
                        else -> {
                            emptyList()
                        }
                    }
                    _isLastPage.value=items.size<10
                    if(own) items.filter { it.is_complete==true && it.topic_id!=null }.forEach { item ->
                        item.workflow_status=runCatching { com.rescue.flutter_720yun.adoption.repository.AdoptionRepository().state(item.topic_id!!).workflow_status }.getOrNull()
                    }
                    if(session!=com.rescue.flutter_720yun.util.UserManager.sessionRevision.value) return@launch
                    if (items.isNotEmpty()) {
                        _uiState.value = UiState.Success(items)
                        page += 1
                    }else{
                        if (page == 1) {
                            val noMoreData =
                                BaseApplication.context.resources.getString(R.string.no_data)
                            _uiState.value = UiState.Error(noMoreData)
                        }
                    }
                }else{
                    if (page == 1) {
                        val noMoreData =
                            BaseApplication.context.resources.getString(R.string.no_data)
                        _uiState.value = UiState.Error(noMoreData)
                    }
                }
            }catch (e: Exception) {
                if (page == 1) {
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
            }finally {
                _isLoading.value = false
            }
        }
    }


    private fun loadUserShowListNetworking(refresh: RefreshState) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                if (refresh == RefreshState.REFRESH) {
                    page = 1
                    _isLastPage.value = false
                }
                if (refresh == RefreshState.MORE && _isLastPage.value == true) {
                    return@launch
                }
                if (_isFirstLoading.value == true) {
                    _uiState.value = UiState.FirstLoading
                }
                _isLoading.value = true
                _refreshState.value = refresh
                val dic = paramDic
                dic["page"] = page
                dic["size"] = 10
                dic["userId"] = userId

                val response = appService.userShowPublishNetworking(dic).awaitResp()
                _isFirstLoading.value = false
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data,
                                ShowPageModel::class.java)
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
                        _uiState.value = UiState.Success(items)
                        page += 1
                    }else{
                        if (page == 1) {
                            val noMoreData =
                                BaseApplication.context.resources.getString(R.string.no_data)
                            _uiState.value = UiState.Error(noMoreData)
                        }
                    }
                }else{
                    if (page == 1) {
                        val noMoreData =
                            BaseApplication.context.resources.getString(R.string.no_data)
                        _uiState.value = UiState.Error(noMoreData)
                    }
                }
            }catch (e: Exception) {
                if (page == 1) {
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun cleanData() {
        _refreshState.value = RefreshState.REFRESH
        _uiState.value = UiState.Success(listOf())
        val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
        _uiState.value = UiState.Error(noMoreData)
    }
}
