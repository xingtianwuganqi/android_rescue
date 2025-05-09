package com.rescue.flutter_720yun.home.viewmodels

import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeDetailViewModel: ViewModel() {
    private val appService = ServiceCreator.create<HomeService>()

    private var _homeData = MutableLiveData<HomeListModel?>()
    private var _loadFail = MutableLiveData(false)
    private var _isLoading = MutableLiveData(false)
    private var _errorMsg = MutableLiveData<String>()
    private val _homeDataChanged = MutableLiveData<Boolean>()
    private val _statusCode = MutableLiveData<Int?>()
    private val _deleted = MutableLiveData<Boolean>()
    val homeData: LiveData<HomeListModel?> get() = _homeData
    val isLoading: LiveData<Boolean> get() = _isLoading
    val errorMsg: LiveData<String> get() = _errorMsg
    val homeDataChanged: LiveData<Boolean> get() = _homeDataChanged
    val statusCode: LiveData<Int?> get() = _statusCode
    val deleted: LiveData<Boolean> get() = _deleted

    var topicId: Int? = null
    var topicFrom: Int = 0

    fun loadDetailNetworking(topicId: Int) {
        viewModelScope.launch {
            if (_isLoading.value == true) {
                return@launch
            }
            try {
                val dic = paramDic
                dic["topic_id"] = topicId
                val response = appService.topicDetail(dic).awaitResp()
                if (response.code == 200) {
                    _homeData.value = response.data
                }else{
                    _errorMsg.value = response.message
                }
            }catch (e: Exception) {
                _errorMsg.value = BaseApplication.context.getString(R.string.network_request_error)
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
                    _homeData.value = model
                    _homeDataChanged.value = true
                }else{
                    val msg = BaseApplication.context.resources.getString(R.string.like_error)
                    _errorMsg.value = msg
                }
            }catch (e: Exception) {
                _errorMsg.value = BaseApplication.context.resources.getString(R.string.like_error)
            }finally {
                _isLoading.value = false
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
                    _homeData.value = model
                    _homeDataChanged.value = true
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

    fun clickGetContactInfoNetworking(model: HomeListModel?) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                    val dic = paramDic
                    dic["topic_id"] = model?.topic_id
                    val response = appService.getTopicContact(dic).awaitResp()
                    _statusCode.value = response.code
                    if (response.code == 200) {
                        model?.contact_info = response.data.contact
                        model?.getedcontact = true
                        _homeData.value = model
                        _homeDataChanged.value = true
                    } else if (response.code == 202) { // 无法获取联系方式
                        _errorMsg.value = BaseApplication.context.getString(R.string.unable_get_contact)
                    }else{
                        _errorMsg.value = BaseApplication.context.getString(R.string.get_contact_error)
                    }

            }catch (e: Exception) {
                _errorMsg.value = BaseApplication.context.getString(R.string.network_request_error)
            }finally {
                _isLoading.value = false
            }
        }
    }


    /*
    // status 要改成的状态 1：已完成 0：未完成
     */
    fun changeCompleteStatus(model: HomeListModel?) {
        viewModelScope.launch {
            try {
                val status = if (model?.is_complete == false) 1 else 0
                val dic = paramDic
                if (model?.topic_id != null) {
                    dic["topic_id"] = model.topic_id
                }
                dic["isComplete"] = status
                Log.d("TAG", "$dic")
                val response = appService.changeCompleteStatus(dic).awaitResp()
                Log.d("TAG", "$response")
                if (response.code == 200) {
                    model?.is_complete = status == 1
                    _homeData.value = model
                    _homeDataChanged.value = true
                }else{
                    _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.network_request_error)
                }
            }catch (e: Exception) {
                Log.d("TAG", "$e")
                _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.network_request_error)
            }
        }
    }

    fun deleteTopic(model: HomeListModel?) {
        viewModelScope.launch {
            try {
                val dic = paramDic
                if (model?.topic_id != null) {
                    dic["topic_id"] = model.topic_id
                }
                val response = appService.deleteTopic(dic).awaitResp()
                if (response.code == 200) {
                    _deleted.value = true
                }else{
                    _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.network_request_error)
                }
            }catch (e: Exception) {
                _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.network_request_error)
            }
        }
    }
}