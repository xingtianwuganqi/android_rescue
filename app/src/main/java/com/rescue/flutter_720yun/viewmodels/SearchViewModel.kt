package com.rescue.flutter_720yun.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.models.BaseListResp
import com.rescue.flutter_720yun.models.SearchHistoryItemModel
import com.rescue.flutter_720yun.models.SearchKeywordModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SearchViewModel: ViewModel() {
    private val appService = ServiceCreator.create<AppService>()

    private var _keywordModels = MutableLiveData<List<SearchHistoryItemModel>?>()
    private var _keyword = MutableLiveData<String>()
    private var _searchWord = MutableLiveData<String>()

    val keywordModels: LiveData<List<SearchHistoryItemModel>?> get() = _keywordModels
    val searchWord: LiveData<String> get() = _searchWord
    val keyword: LiveData<String> get() = _keyword

    fun searchKeywordNetworking() {
        viewModelScope.launch {
            try {
                val deferred1 = async {
                    appService.getSearchKeyword().awaitResp()
                }

                val deferred2 = async {
                    fetchLocalData()
                }

                val result = awaitAll(deferred1, deferred2)
                val response = result[0]
                val localData = result[1]
                val historyItems = mutableListOf<SearchHistoryItemModel>()
                if ((response as BaseListResp<SearchKeywordModel>) != null) {
                    val resp = response.data as List<SearchKeywordModel>
                    val historyItemModel = SearchHistoryItemModel("热门搜索",
                        resp)
                    historyItems.add(historyItemModel)
                }
                Log.d("TAG", "local data is $localData")
                if (localData is List<*>) {
                    val list = (localData as List<String>?)?.map {
                        SearchKeywordModel(0,it)
                    }
                    val localHistory = SearchHistoryItemModel("历史搜索", list)
                    historyItems.add(localHistory)
                }
                _keywordModels.value = if (historyItems.size > 0) historyItems else null

            }catch (e: Exception) {
                Log.d("TAG", "error msg is $e")
            }finally {

            }
        }
    }

    //读取本地存储的数据
    private suspend fun fetchLocalData(): List<String> {
        return suspendCancellableCoroutine { continuation ->
            val sharedPreferences = BaseApplication.context.getSharedPreferences("default", Context.MODE_PRIVATE)
            val wordJson = sharedPreferences.getString("keywords", "")

            if (wordJson != null) {
                val words = wordJson.split(",")
                continuation.resume(words)
            } else {
                continuation.resumeWithException(Exception("null"))
            }
        }
    }

    fun addSearchKeyToLocalJson(keyword: String) {
        if (keyword.trim().isEmpty()) {
            return
        }
        _keyword.value = keyword
        val sharedPreferences = BaseApplication.context.getSharedPreferences("default", Context.MODE_PRIVATE)
        val wordJson = sharedPreferences.getString("keywords","")
        if (wordJson != null) {
            val words = wordJson.split(",").filter {
                it != ""
            }.toMutableList()
            if (!words.contains(keyword)) {
                words.add(keyword)
                val totalStr = words.joinToString(",")
                val editor = sharedPreferences.edit()
                editor.putString("keywords", totalStr)
                editor.apply()

                // 更新
                val list = words.map {
                    SearchKeywordModel(0,it)
                }
                val localHistory = SearchHistoryItemModel("历史搜索", list)

                val historyItems = mutableListOf<SearchHistoryItemModel>()
                _keywordModels.value?.first()?.let {
                    historyItems.add(it)
                }
                historyItems.add(localHistory)
                _keywordModels.value = historyItems
            }

        }else{
            val editor = sharedPreferences.edit()
            editor.putString("keywords",keyword)
            editor.apply()

            // 更新
            val item = SearchKeywordModel(0,keyword)
            val localHistory = SearchHistoryItemModel("历史搜索", listOf(item))

            val historyItems = mutableListOf<SearchHistoryItemModel>()
            _keywordModels.value?.first()?.let {
                historyItems.add(it)
            }
            historyItems.add(localHistory)
            _keywordModels.value = historyItems
        }
    }

    fun deleteLocalKeyword() {
        val sharedPreferences = BaseApplication.context.getSharedPreferences("default", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("keywords")
        editor.apply()
        val historyItems = mutableListOf<SearchHistoryItemModel>()
        _keywordModels.value?.first()?.let {
            historyItems.add(it)
        }
        _keywordModels.value = historyItems
    }

    fun beginSearch(keyword: String) {
        _searchWord
    }

}