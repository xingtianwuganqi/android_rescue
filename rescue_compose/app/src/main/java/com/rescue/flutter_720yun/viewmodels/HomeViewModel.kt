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

    suspend fun fetchHomeList(): ResultWrapper<List<HomeItem>> {
        return safeApiCall {
            RetrofitClient.api.getHomeList()
        }
    }
}


class HomeViewModel(
    private val homeRepository: HomeRepository = HomeRepository()
): ViewModel () {
    var uiState by mutableStateOf<UiState<List<HomeItem>>>(UiState.Loading)
    fun loadData() {
        viewModelScope.launch {
            uiState = UiState.Loading
            val result = homeRepository.fetchHomeList()
            uiState = when(result) {
                is ResultWrapper.Success -> UiState.Success(result.value)
                is ResultWrapper.Error -> UiState.Error(result.message)
            }
        }
    }
}