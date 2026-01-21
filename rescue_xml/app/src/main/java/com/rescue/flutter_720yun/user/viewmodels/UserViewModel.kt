package com.rescue.flutter_720yun.user.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.UserInfo
import com.rescue.flutter_720yun.home.models.UserInfoModel
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.show.models.ShowPageModel
import com.rescue.flutter_720yun.util.CommonViewModelInterface
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class UserViewModel: ViewModel(), CommonViewModelInterface {
    val appService = ServiceCreator.create<UserService>()
    private val _userInfo = MutableLiveData<UserInfoModel?>()
    val userInfo: LiveData<UserInfoModel?> get() = _userInfo
    private val _userIdLiveData = MutableLiveData<Int>()
    val userIdLiveData: LiveData<Int> = _userIdLiveData.distinctUntilChanged()


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

    private var page: Int = 1

    fun setUserId(userId: Int) {
        _userIdLiveData.value = userId
    }

    fun userIdGetUserInfoNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["userId"] = _userIdLiveData.value
                Log.d("TAG", "$dic")
                val response = appService.userIdGetUserInfo(dic).awaitResp()
                if (response.code == 200) {
                    _userInfo.value = response.data
                }
            }catch (e: Exception) {
                Log.d("TAG", "$e")
            }
        }
    }

    fun cleanUserInfo() {
        _userInfo.value = null
    }
//
//    fun loadDataNetworking(refresh: RefreshState) {
//        if (from == 0) {
//            loadUserTopicListNetworking(refresh)
//        }else{
//            loadUserShowListNetworking(refresh)
//        }
//    }

    fun loadUserTopicListNetworking(refresh: RefreshState) {
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
                dic["userId"] = _userIdLiveData.value
                Log.d("TAG","$dic")
                val response = appService.userPublishNetworking(dic).awaitResp()
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


    fun loadUserShowListNetworking(refresh: RefreshState) {
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
                dic["userId"] = _userIdLiveData.value
                Log.d("TAG","$dic")
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

    fun deleteItem(model: HomeListModel) {
        when (_uiState.value) {
            is UiState.Success -> {
                val list = (_uiState.value as UiState.Success<List<Any>>).data
                if (list.first() is HomeListModel) {
                    val items = list.map {
                        it as HomeListModel
                    }
                    val newItem = items.filter {
                        it.topic_id != model.topic_id
                    }
                    _uiState.value = UiState.Success(newItem)
                }else if (list.first() is ShowPageModel) {
                    val items = list.map {
                        it as ShowPageModel
                    }
                    _uiState.value = UiState.Success(items)
                }
            }else -> {

            }
        }
    }

    fun deleteShowInfo(show: ShowPageModel) {
        when (_uiState.value) {
            is UiState.Success -> {
                val list = (_uiState.value as UiState.Success<List<Any>>).data
                if (list.first() is HomeListModel) {
                    val items = list.map {
                        it as HomeListModel
                    }

                    _uiState.value = UiState.Success(items)
                }else if (list.first() is ShowPageModel) {
                    val items = list.map {
                        it as ShowPageModel
                    }
                    val newItem = items.filter {
                        it.show_id != show.show_id
                    }
                    _uiState.value = UiState.Success(items)
                }
            }else -> {

        }
        }
    }
}