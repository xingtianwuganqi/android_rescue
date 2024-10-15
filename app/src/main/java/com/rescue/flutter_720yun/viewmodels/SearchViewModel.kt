package com.rescue.flutter_720yun.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.models.SearchKeywordModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import kotlinx.coroutines.launch

class SearchViewModel: ViewModel() {
    private val appService = ServiceCreator.create<AppService>()

    private var _keywordModels = MutableLiveData<List<SearchKeywordModel>?>()

    val keywordModels: LiveData<List<SearchKeywordModel>?> get() = _keywordModels


    fun searchKeywordNetworking() {
        viewModelScope.launch {
            var response = appService.getSearchKeyword().awaitResp()
            if (response.code == 200) {
                _keywordModels.value = response.data
            }else{
                _keywordModels.value = null
            }
        }
    }
}