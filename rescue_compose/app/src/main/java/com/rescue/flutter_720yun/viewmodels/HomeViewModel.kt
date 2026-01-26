package com.rescue.flutter_720yun.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.common.UiState
import com.rescue.flutter_720yun.models.HomeItem
import com.rescue.flutter_720yun.network.ResultWrapper
import com.rescue.flutter_720yun.network.RetrofitClient
import com.rescue.flutter_720yun.network.safeApiCall
import kotlinx.coroutines.launch


class HomeRepository {

    suspend fun fetchHomeList(
        pageNum: Int,
        pageSize: Int
    ): ResultWrapper<List<HomeItem>> {
        return safeApiCall {
            RetrofitClient.api.getHomeList(
                pageNum = pageNum,
                pageSize = pageSize
            )
        }
    }
}

class HomeViewModel(
    private val homeRepository: HomeRepository = HomeRepository()
): ViewModel() {

    private var currentPage = 1
    private val pageSize = 20
    private var hasMore = true
    private var isLoading = false

    private val list = mutableListOf<HomeItem>()

    var uiState by mutableStateOf<UiState<List<HomeItem>>>(UiState.Loading)
        private set

    /** 首次进入自动调用 */
    fun loadIfNeeded() {
        if (list.isNotEmpty()) return
        refresh()
    }


    /** 下拉刷新调用 */
    fun refresh() {
        currentPage = 1
        hasMore = true
        list.clear()
        loadMore()
    }


    fun loadData() {
        viewModelScope.launch {
            uiState = UiState.Loading
            val result = homeRepository.fetchHomeList(
                pageNum = 1,
                pageSize = 20
            )
            uiState = when(result) {
                is ResultWrapper.Success -> UiState.Success(result.value)
                is ResultWrapper.Error -> UiState.Error(result.message)
            }
        }
    }

    fun loadMore() {
        if (isLoading || !hasMore) return

        viewModelScope.launch {
            isLoading = true
            if (currentPage == 1) {
                uiState = UiState.Loading
            }

            val result = homeRepository.fetchHomeList(
                pageNum = currentPage,
                pageSize = pageSize
            )

            uiState = when (result) {
                is ResultWrapper.Success -> {
                    val data = result.value
                    if (data.size < pageSize) {
                        hasMore = false
                    }
                    list.addAll(data)
                    currentPage++
                    UiState.Success(list.toList())
                }

                is ResultWrapper.Error -> {
                    UiState.Error(result.message)
                }
            }

            isLoading = false
        }
    }
}
