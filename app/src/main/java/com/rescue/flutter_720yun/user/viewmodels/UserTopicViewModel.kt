package com.rescue.flutter_720yun.user.viewmodels

import android.app.Service
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.util.CommonViewModelInterface
import com.rescue.flutter_720yun.util.RefreshState

class UserTopicViewModel: ViewModel(), CommonViewModelInterface {

    val appService = ServiceCreator.create<UserService>()

    private val _isLoading = MutableLiveData(false)
    private val _isFirstLoading = MutableLiveData(false)
    private val _isLastPage = MutableLiveData(false)
    private val _refreshState = MutableLiveData<RefreshState>()

    override val isLoading: LiveData<Boolean>
        get() = _isLoading

    override val isFirstLoading: LiveData<Boolean>
        get() = _isFirstLoading

    override val isLastPage: LiveData<Boolean>
        get() = _isLastPage

    override val refreshState: LiveData<RefreshState>
        get() = _refreshState

    var userId: Int? = null

    var from: Int? = null // 0: 帖子，1:秀宠

    fun loadUserTopicListNetworking(refreshState: RefreshState, userId: Int) {

    }
}