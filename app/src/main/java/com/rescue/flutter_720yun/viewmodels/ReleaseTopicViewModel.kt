package com.rescue.flutter_720yun.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.models.BaseResponse
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.models.TagInfoModel
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class ReleaseTopicViewModel: ViewModel() {
    private var _selectTags = MutableLiveData<List<TagInfoModel>>()
    val selectTags: LiveData<List<TagInfoModel>> get() = _selectTags

    fun uploadSelectTags(items: List<TagInfoModel>){
        _selectTags.value = items
    }
}

class TagListViewModel: ViewModel() {
    private val network = ServiceCreator.create<HomeService>()

    private var _uiState = MutableLiveData<UiState<List<TagInfoModel>>>()
    val uiState: LiveData<UiState<List<TagInfoModel>>> get() = _uiState

    private var _selectTags = MutableLiveData<List<TagInfoModel>>()
    val selectTags: LiveData<List<TagInfoModel>> get() = _selectTags

    fun getTagsNetworking() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.FirstLoading
                var dic = paramDic
                val response = network.getTagsNetworking(dic).awaitResp()
                if (response.code == 200) {
                    var items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data, TagInfoModel::class.java)
                            homeList ?: emptyList()
                        }
                        is Map<*, *> ->{
                            emptyList()
                        }
                        else -> {
                            emptyList()
                        }
                    }
                    _uiState.value = UiState.Success(items)
                }else{
                    _uiState.value = UiState.Error(BaseApplication.context.getString(R.string.no_data))
                }
            }catch (e: Exception) {
                _uiState.value = UiState.Error(BaseApplication.context.getString(R.string.no_data))
            }finally {

            }
        }
    }

    // 更新所选的标签
    fun uploadSelectTag(item: TagInfoModel) {
        // 检查当前状态是否是 Success 状态
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            // 使用当前数据创建一个新的 Success 状态
            var currentList = currentState.data
            currentList = currentList.map {
                if (it.id == item.id) {
                    it.isSelected = !it.isSelected
                }
                it
            }
            _selectTags.value = currentList.filter {
                it.isSelected
            }
        }
    }
}