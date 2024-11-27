package com.rescue.flutter_720yun.home.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.FindPetModel
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.CommonViewModelInterface
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class FindPetViewModel(
) : ViewModel(), CommonViewModelInterface {
    private val appService = ServiceCreator.create<HomeService>()
    private var page: Int = 1

    private var _isLoading = MutableLiveData(false)
    private var _isFirstLoading = MutableLiveData(true)
    private var _isLastPage = MutableLiveData(false)
    private var _refreshState = MutableLiveData<RefreshState>()
    private var _uiState = MutableLiveData<UiState<List<FindPetModel>>>()
    private var _changeModel = MutableLiveData<FindPetModel>()
    private var _errorMsg = MutableLiveData<String?>()

    override val isLoading: LiveData<Boolean>
        get() = _isLoading

    override val isFirstLoading: LiveData<Boolean>
        get() = _isFirstLoading

    override val isLastPage: LiveData<Boolean>
        get() = _isLastPage

    override val refreshState: LiveData<RefreshState>
        get() = _refreshState

    val uiState: LiveData<UiState<List<FindPetModel>>> get() = _uiState

    val changeModel: LiveData<FindPetModel> get() = _changeModel
    val errorMsg: LiveData<String?> get() = _errorMsg


    fun findPetListNetworking(refresh: RefreshState) {
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
                val response = appService.findPetList(dic).awaitResp()
                _isFirstLoading.value = false
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data, FindPetModel::class.java)
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


    fun getFindPetContactNetworking(model: FindPetModel) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                _isLoading.value = true
                val dic = paramDic
                dic["topic_id"] = model.findId
                dic["topic_type"] = 2
                val response = appService.getTopicContact(dic).awaitResp()
                if (response.code == 200) {
                    model.getedcontact = true
                    model.contact_info = response.data.contact
                    _changeModel.value = model
                }else{
                    _errorMsg.value = response.message
                }
            }catch (e: Exception) {
                _isLoading.value = false
                _errorMsg.value = BaseApplication.context.resources.getString(R.string.network_request_error)
            }finally {
                _isLoading.value = false
            }
        }
    }
}