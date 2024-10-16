package com.rescue.flutter_720yun.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.models.SearchHistoryItemModel
import com.rescue.flutter_720yun.models.SearchKeywordModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import kotlinx.coroutines.launch

class SearchViewModel: ViewModel() {
    private val appService = ServiceCreator.create<AppService>()

    private var _keywordModels = MutableLiveData<List<SearchHistoryItemModel>?>()

    val keywordModels: LiveData<List<SearchHistoryItemModel>?> get() = _keywordModels


    fun searchKeywordNetworking() {
        viewModelScope.launch {
            try {
                var response = appService.getSearchKeyword().awaitResp()
                if (response.code == 200) {
                    val historyItemModel = SearchHistoryItemModel("热门搜索", response.data)
                    _keywordModels.value = listOf(historyItemModel)
                }else{
                    _keywordModels.value = null
                }
            }catch (e: Exception) {
                Log.d("TAG", "error msg is $e")
            }finally {

            }
        }
    }
}