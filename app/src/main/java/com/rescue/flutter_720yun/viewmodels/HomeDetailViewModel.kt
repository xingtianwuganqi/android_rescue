package com.rescue.flutter_720yun.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResponse
import com.rescue.flutter_720yun.util.UserManager

class HomeDetailViewModel: ViewModel() {
    private val appService = ServiceCreator.create<AppService>()
    private var _homeData = MutableLiveData<HomeListModel?>()
    private var _loadFail = MutableLiveData(false)
    val homeData: LiveData<HomeListModel?> get() = _homeData
    suspend fun loadDetailNetworking(topicId: Int) {
        var dic = mutableMapOf<String, Any?>(
            "topic_id" to topicId
        )
        if (UserManager.isLogin) {
            dic["token"] = UserManager.token
        }
        val response = appService.topicDetail(dic).awaitResponse()
        if (response.code == 200) {
            _homeData.value = response.data
        }else{
            _loadFail.value = true
        }
    }
}