package com.rescue.flutter_720yun.home.viewmodels

import android.util.Log
import androidx.compose.runtime.key
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.UserCollectionModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.SharedPreferencesUtil
import com.rescue.flutter_720yun.util.UiState
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
    private var _refreshState = MutableLiveData<RefreshState>()
    private var _isFirstLoading = MutableLiveData(true)
    private val _uiState = MutableLiveData<UiState<List<HomeListModel>>>()

    val uiState: LiveData<UiState<List<HomeListModel>>> get() = _uiState

    val models: LiveData<List<HomeListModel>> get() = _models
    val isLastPage: LiveData<Boolean> get() = _isLastPage
    val isLoading: LiveData<Boolean> get() = _isLoading
    val changeModel: LiveData<HomeListModel?> get() = _changedModel
    val errorMsg: LiveData<String> get() = _errorMsg
    val isFirstLoading: LiveData<Boolean> get() = _isFirstLoading
    val refreshState: LiveData<RefreshState> get() = _refreshState
    var searchKeyword: String? = null
    var cityName: String? = null
    var pageType: String = "0" // 0:首页 1：搜索 2：同城

    private val appService = ServiceCreator.create<HomeService>()

    fun loadListData(refresh: RefreshState) {
        viewModelScope.launch {
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
            try {
                val dic = paramDic
                dic["page"] = page
                dic["size"] = 10
                dic["order"] = 0
                val response = appService.getTopicList(dic).awaitResp()
                _isFirstLoading.value = false
                if (response.code == 200) {
                    var items = when (response.data) {
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
                    val topicIds = SharedPreferencesUtil.getStringSet("black_home_list", BaseApplication.context)
                    items = items.filter {
                        topicIds?.contains("${it.topic_id}") != true
                    }
                    if (items.isNotEmpty()) {
                        page += 1
                        _uiState.value = UiState.Success(items)
                    }else{
                        if (page == 1) {
                            val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                            _uiState.value = UiState.Error(noMoreData)
                        }else{
                            _isLastPage.value = true
                        }
                    }
                }else{
                    if (page == 1) {
                        val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
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

    fun searchListNetworking(keyword: String, refresh: RefreshState) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                _isLoading.value = true
                if (refresh == RefreshState.REFRESH) {
                    page = 1
                    _isLastPage.value = false
                    searchKeyword = keyword
                }
                if (refresh == RefreshState.MORE && _isLastPage.value == true) {
                    return@launch
                }
                if (_isFirstLoading.value == true) {
                    _uiState.value = UiState.FirstLoading
                }
                _refreshState.value = refresh
                val dic = paramDic
                dic["keyword"] = keyword
                dic["page"] = page
                dic["size"] = 10
                val response = appService.searchList(dic).awaitResp()
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
                        _uiState.value = UiState.Success(items)
                    }else{
                        if (page == 1) {
                            val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                            _uiState.value = UiState.Error(noMoreData)
                        }else{
                            _isLastPage.value = true
                        }
                    }
                }else{
                    if (page == 1) {
                        val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                        _uiState.value = UiState.Error(noMoreData)
                    }
                }
            }catch (e: Exception) {
                if (page == 1) {
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
            }finally {
                _isLoading.value = false
                if (_isFirstLoading.value == true && _uiState.value is UiState.Success) {
                    _isFirstLoading.value = false
                }
            }
        }
    }

    fun localListNetworking(address: String, refresh: RefreshState) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                _isLoading.value = true
                if (refresh == RefreshState.REFRESH) {
                    page = 1
                    _isLastPage.value = false
                    cityName = address
                }
                if (refresh == RefreshState.MORE && _isLastPage.value == true) {
                    return@launch
                }
                if (_isFirstLoading.value == true) {
                    _uiState.value = UiState.FirstLoading
                }
                _refreshState.value = refresh

                val dic = paramDic
                dic["address"] = address
                dic["page"] = page
                dic["size"] = 10
                Log.d("TAG","dic is $dic")
                val response = appService.localTopicList(dic).awaitResp()
                _isFirstLoading.value = false
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
                        _uiState.value = UiState.Success(items)
                    }else{
                        if (page == 1) {
                            val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                            _uiState.value = UiState.Error(noMoreData)
                        }else{
                            _isLastPage.value = true
                        }
                    }
                }else{
                    if (page == 1) {
                        val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                        _uiState.value = UiState.Error(noMoreData)
                    }
                }
            }catch (e: Exception) {
                if (page == 1) {
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
            }finally {
                _isLoading.value = false
            }
        }
    }


    // 用户收藏
    fun loadUserCollection(refresh: RefreshState) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                _isLoading.value = true
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
                _refreshState.value = refresh

                val dic = paramDic
                dic["page"] = page
                dic["size"] = 10
                Log.d("TAG","dic is $dic")
                val response = appService.userCollectionList(dic).awaitResp()
                _isFirstLoading.value = false
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val collectionList = convertAnyToList(response.data, UserCollectionModel::class.java)
                            val models = collectionList?.map {
                                it.topicInfo!!
                            }
                            models ?: emptyList()
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
                        _uiState.value = UiState.Success(items)
                    }else{
                        if (page == 1) {
                            val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                            _uiState.value = UiState.Error(noMoreData)
                        }else{
                            _isLastPage.value = true
                        }
                    }
                }else{
                    if (page == 1) {
                        val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                        _uiState.value = UiState.Error(noMoreData)
                    }
                }
            }catch (e: Exception) {
                if (page == 1) {
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_more_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
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
                _errorMsg.value = BaseApplication.context.resources.getString(R.string.like_error)
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

}