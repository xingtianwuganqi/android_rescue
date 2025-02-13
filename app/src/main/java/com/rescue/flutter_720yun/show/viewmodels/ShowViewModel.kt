package com.rescue.flutter_720yun.show.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.FindPetModel
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.ShowService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.show.models.ShowPageModel
import com.rescue.flutter_720yun.show.models.UserShowCollectionModel
import com.rescue.flutter_720yun.util.CommonViewModelInterface
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class ShowViewModel : ViewModel(), CommonViewModelInterface {
    private val appService = ServiceCreator.create<ShowService>()

    private var page: Int = 1

    private var _isLoading = MutableLiveData(false)
    private var _isFirstLoading = MutableLiveData(true)
    private var _isLastPage = MutableLiveData(false)
    private var _refreshState = MutableLiveData<RefreshState>()
    private var _uiState = MutableLiveData<UiState<List<ShowPageModel>>>()
    private var _changeModel = MutableLiveData<ShowPageModel>()
    private var _errorMsg = MutableLiveData<String?>()

    override val isLoading: LiveData<Boolean>
        get() = _isLoading

    override val isFirstLoading: LiveData<Boolean>
        get() = _isFirstLoading

    override val isLastPage: LiveData<Boolean>
        get() = _isLastPage

    override val refreshState: LiveData<RefreshState>
        get() = _refreshState

    val uiState: LiveData<UiState<List<ShowPageModel>>> get() = _uiState

    val changeModel: LiveData<ShowPageModel> get() = _changeModel
    val errorMsg: LiveData<String?> get() = _errorMsg

    var showId: Int? = null

    fun showPageListNetworking(refresh: RefreshState) {
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
                if (showId != null) {
                    dic["show_id"] = showId
                }
                Log.d("TAG","dic is $dic")
                val response = appService.showPageList(dic).awaitResp()
                _isFirstLoading.value = false
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data, ShowPageModel::class.java)
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
                            val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
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

            }catch (_: Exception) {
                if (page == 1) {
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun userShowCollectionList(refresh: RefreshState) {
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
                Log.d("TAG","dic is $dic")
                val response = appService.userShowCollectionList(dic).awaitResp()
                _isFirstLoading.value = false
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val showCollectionList = convertAnyToList(response.data, UserShowCollectionModel::class.java)
                            val models = showCollectionList?.map {
                                it.showInfo!!
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
                            val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
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

            }catch (_: Exception) {
                if (page == 1) {
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
            }finally {
                _isLoading.value = false
            }
        }
    }


    fun showLikeNetworking(model: ShowPageModel) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                val dic = paramDic
                dic["like_mark"] = if (model.liked == true) 0 else 1
                dic["show_id"] = model.show_id
                val response = appService.showInfoLikeNetworking(dic).awaitResp()
                if (response.code == 200) {
                    model.liked = !(model.liked ?: false)
                    _changeModel.value = model
                }else{
                    val errorMessage = BaseApplication.context.resources.getString(R.string.like_error)
                    _errorMsg.value = errorMessage
                }
            }catch (e: Exception) {
                val errorMessage = BaseApplication.context.resources.getString(R.string.network_request_error)
                _errorMsg.value = errorMessage
            }finally {

            }
        }
    }

    fun showCollectionNetworking(model: ShowPageModel) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                val dic = paramDic
                dic["collect_mark"] = if (model.collectioned == true) 0 else 1
                dic["show_id"] = model.show_id
                val response = appService.showInfoCollectionNetworking(dic).awaitResp()
                if (response.code == 200) {
                    model.collectioned = !(model.collectioned ?: false)
                    _changeModel.value = model
                }else{
                    val errorMessage = BaseApplication.context.resources.getString(R.string.collect_error)
                    _errorMsg.value = errorMessage
                }
            }catch (e: Exception) {
                val errorMessage = BaseApplication.context.resources.getString(R.string.network_request_error)
                _errorMsg.value = errorMessage
            }finally {

            }
        }
    }
}