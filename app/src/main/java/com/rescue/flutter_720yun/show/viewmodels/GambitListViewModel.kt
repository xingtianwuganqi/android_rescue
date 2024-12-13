package com.rescue.flutter_720yun.show.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.ShowService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.show.models.GambitListModel
import com.rescue.flutter_720yun.show.models.ShowPageModel
import com.rescue.flutter_720yun.util.CommonViewModelInterface
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class GambitListViewModel: ViewModel(), CommonViewModelInterface {

    private val appService = ServiceCreator.create<ShowService>()
    private var _isLoading = MutableLiveData<Boolean>()
    private var _isFirstLoading = MutableLiveData(true)
    private var _isLastPage = MutableLiveData<Boolean>()
    private var _refreshState = MutableLiveData<RefreshState>()
    private var _uiState = MutableLiveData<UiState<List<GambitListModel>>>()

    override val isLoading: LiveData<Boolean>
        get() = _isLoading

    override val isFirstLoading: LiveData<Boolean>
        get() = _isFirstLoading

    override val isLastPage: LiveData<Boolean>
        get() = _isLastPage

    override val refreshState: LiveData<RefreshState>
        get() = _refreshState

    val uiState: LiveData<UiState<List<GambitListModel>>> get() = _uiState
    var selectModel: GambitListModel? = null

    fun gambitListNetworking() {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }

                if (_isFirstLoading.value == true) {
                    _uiState.value = UiState.FirstLoading
                }
                _isLoading.value = true
                val dic = paramDic
                val response = appService.gambitList(dic).awaitResp()
                _isFirstLoading.value = false
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data, GambitListModel::class.java)
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
                    }else{
                        val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                        _uiState.value = UiState.Error(noMoreData)
                    }
                }else{
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
            }catch (e: Exception) {
                val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                _uiState.value = UiState.Error(noMoreData)
            }finally {
                _isLoading.value = false
            }
        }
    }

}