package com.rescue.flutter_720yun.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

interface CommonViewModelInterface {
    val isLoading: LiveData<Boolean>
    val isFirstLoading: LiveData<Boolean>
    val isLastPage: LiveData<Boolean>
    val refreshState: LiveData<RefreshState>
}
