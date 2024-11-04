package com.rescue.flutter_720yun.home.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.home.models.BaseListResp
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.SearchHistoryItemModel
import com.rescue.flutter_720yun.home.models.SearchKeywordModel
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
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

    private var _isLastPage = MutableLiveData(false)
    private var _refreshState = MutableLiveData<RefreshState>()

    private val _uiState = MutableLiveData<UiState<List<HomeListModel>>>()
    val uiState: LiveData<UiState<List<HomeListModel>>> = _uiState

    val keywordModels: LiveData<List<SearchHistoryItemModel>?> get() = _keywordModels
    val searchWord: LiveData<String> get() = _searchWord
    val keyword: LiveData<String> get() = _keyword
    val isLastPage: LiveData<Boolean> get() = _isLastPage
    val refreshState: LiveData<RefreshState> get() = _refreshState

    private var page: Int = 1
    private var isLoading = false

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
                if (localData is List<*>) {
                    val list = (localData as List<String>?)?.map {
                        SearchKeywordModel(0,it)
                    }
                    if (list?.isNotEmpty() == true) {
                        val localHistory = SearchHistoryItemModel("历史搜索", list)
                        historyItems.add(localHistory)
                    }
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
                val words = wordJson.split(",").filter {
                    it != ""
                }.toMutableList()
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
            Log.d("TAG", "All words $words")
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
        _searchWord.value = keyword
    }

    fun searchListNetworking(keyword: String, refreshState: RefreshState) {
        viewModelScope.launch {
            if (isLoading) {
                return@launch
            }
            try {
                if (refreshState == RefreshState.REFRESH) {
                    _uiState.value = UiState.FirstLoading
                    page = 1
                }
                if (page == 1) {
                    _isLastPage.value = false
                }
                isLoading = true
                val dic = paramDic
                dic["keyword"] = keyword
                dic["page"] = page
                dic["size"] = 10
                val response = appService.searchList(dic).awaitResp()
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data, HomeListModel::class.java)
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
                        page += 1
                        _uiState.value = UiState.Success<List<HomeListModel>>(items)
                    }else{
                        if (page == 1) {
                            _uiState.value = UiState.Error("暂无数据")
                        }else{
                            _isLastPage.value = true
                        }
                    }
                }else{
                    if (page == 1) {
                        _uiState.value = UiState.Error("暂无数据")
                    }
                }
            }catch (e: Exception) {
                if (page == 1) {
                    _uiState.value = UiState.Error("暂无数据")
                }
            }finally {
                isLoading = false
                _refreshState.value = refreshState
            }
        }
    }

    fun cleanSearchList() {
        _refreshState.value = RefreshState.REFRESH
        _uiState.value = UiState.Success(emptyList())
    }
}