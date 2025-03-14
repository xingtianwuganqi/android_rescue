package com.rescue.flutter_720yun.home.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.ReportTypeModel
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.CommonViewModelInterface
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class TopicReportViewModel: ViewModel(), CommonViewModelInterface {

    private val appService = ServiceCreator.create<HomeService>()

    private var _isLoading = MutableLiveData(false)
    private var _isFirstLoading = MutableLiveData(true)
    private var _isLastPage = MutableLiveData(false)
    private var _refreshState = MutableLiveData<RefreshState>()
    private var _uiState = MutableLiveData<UiState<List<ReportTypeModel>>>()
    private var _errorMsg = MutableLiveData<String?>()
    private var _pushSuccess = MutableLiveData<Boolean>()

    override val isLoading: LiveData<Boolean>
        get() = _isLoading

    override val isFirstLoading: LiveData<Boolean>
        get() = _isFirstLoading

    override val isLastPage: LiveData<Boolean>
        get() = _isLastPage

    override val refreshState: LiveData<RefreshState>
        get() = _refreshState

    val uiState: LiveData<UiState<List<ReportTypeModel>>> get() = _uiState
    val pushSuccess get() = _pushSuccess
    val errorMsg get() = _errorMsg

    var reportId: Int? = null
    var reportType: Int? = null
    var userId: Int? = null

    fun reportListNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                val response = appService.reportTypeList(dic).awaitResp()
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val reportList = convertAnyToList(response.data, ReportTypeModel::class.java)
                            (reportList ?: emptyList())
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
            }
        }
    }

    fun reportActionNetworking(violation: Int) {
        viewModelScope.launch {
            try {
                val dic = paramDic
                if (reportId != null) {
                    dic["report_id"] = reportId
                }
                if (reportType != null) {
                    dic["report_type"] = reportType
                }
                if (userId != null) {
                    dic["userId"] = userId
                }
                dic["violation"] = violation
                val response = appService.reportNetworking(dic).awaitResp()
                if (response.code == 200) {
                    _pushSuccess.value = true
                    val noMoreData = BaseApplication.context.resources.getString(R.string.push_success)
                    _errorMsg.value = noMoreData
                }else{
                    val noMoreData = BaseApplication.context.resources.getString(R.string.push_fail)
                    _errorMsg.value = noMoreData
                }
            }catch (e: Exception) {
                val noMoreData = BaseApplication.context.resources.getString(R.string.push_fail)
                _errorMsg.value = noMoreData
            }
        }
    }
}