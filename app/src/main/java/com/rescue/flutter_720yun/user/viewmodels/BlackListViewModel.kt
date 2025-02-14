package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.util.RefreshState
import kotlinx.coroutines.launch

class BlackListViewModel: ViewModel() {

    fun blackListNetworking(refreshState: RefreshState) {
        viewModelScope.launch {

        }
    }
}