package com.rescue.flutter_720yun.home.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.CoachReleaseInfo
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.home.models.TagInfoModel
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InputStream
import com.qiniu.android.common.FixedZone
import com.qiniu.android.common.Zone
import com.qiniu.android.http.ResponseInfo
import com.qiniu.android.storage.Configuration
import com.qiniu.android.storage.KeyGenerator
import com.qiniu.android.storage.UpCancellationSignal
import com.qiniu.android.storage.UpCompletionHandler
import com.qiniu.android.storage.UpProgressHandler
import com.qiniu.android.storage.UploadManager
import com.qiniu.android.storage.UploadOptions
import com.qiniu.android.storage.Recorder
import org.json.JSONObject

class ReleaseTopicViewModel: ViewModel() {
    private val appService = ServiceCreator.create<HomeService>()
    private var _selectTags = MutableLiveData<List<TagInfoModel>>()
    private val _checkCode = MutableLiveData<Int?>()
    private val _uploadToken = MutableLiveData<String>()
    private val _imageUploadCompletion = MutableLiveData<Int?>() // 1：成功，2：失败
    private val _releaseSuccess = MutableLiveData<Int?>()

    val checkCode: LiveData<Int?> get() = _checkCode
    val uploadToken: LiveData<String> get() = _uploadToken
    val selectTags: LiveData<List<TagInfoModel>> get() = _selectTags
    val imageUploadCompletion: LiveData<Int?> get() = _imageUploadCompletion
    val releaseSuccess: LiveData<Int?> get() = _releaseSuccess

    var releaseInfo: CoachReleaseInfo = CoachReleaseInfo(
        mutableListOf(),
        null,
        mutableListOf(
            CoachReleasePhoto(true,
                null,
                null,
                null,
                null
            ),
        ),
        null,
        null
    )

    private val uploadManager by lazy {
        UploadManager()
    }


    // 更新所选的标签
    fun uploadSelectTags(items: List<TagInfoModel>){
        _selectTags.value = items
        releaseInfo.tags = items
    }

    // 获取上传token
    fun getUploadTokenNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["check"] = "check"
                val response = appService.getUploadToken(dic).awaitResp()
                _checkCode.value = response.code
                if (response.code == 200) {
                    _uploadToken.value = response.data.token
                }
            }catch (e: Exception) {
                Log.d("TAG", "upload token error $e")
            }
        }
    }

    // 图片上传
    fun imageUpload(token: String) {
        viewModelScope.launch {
            try {
                val uploadPhotos = releaseInfo.photos.filter {
                    !it.isAdd
                }
                val options = UploadOptions(null, null, true,
                    { key, percent ->

                    }
                ) { false }
                for (i in uploadPhotos) {
                    Log.d("TAG", "media path is ${i.media?.path}, photoKey is ${i.photoKey}")
                    if (i.media?.compressPath != null) {
                        uploadManager.put(
                            i.media!!.compressPath,
                            i.photoKey,
                            token,
                            { key, info, response ->
                                Log.d("TAG", "key is $key, info is $info, response is $response",)
                                if (info != null && info.isOK) {
                                    releaseInfo.photos.map { photo ->
                                        if (photo.photoKey == key) {
                                            photo.uploadComplete = true
                                        }
                                        photo
                                    }
                                    judgeUploadCompletion()
                                } else {
                                    releaseInfo.photos.map { photo ->
                                        if (photo.photoKey == key) {
                                            photo.uploadComplete = false
                                        }
                                        photo
                                    }
                                    judgeUploadCompletion()
                                }
                            }, options
                        )
                    }
                }
            }catch (e: Exception) {
                Log.d("TAG", "upload fail $e")
            }finally {

            }
         }
    }

    private fun judgeUploadCompletion() {
        val totalSize = releaseInfo.photos.filter { photo ->
            !photo.isAdd
        }.size

        val uploadComp = releaseInfo.photos.filter { photo ->
            !photo.isAdd
        }.filter {
            it.uploadComplete != null
        }

        val uploadSucc = releaseInfo.photos.filter { photo ->
            !photo.isAdd
        }.filter {
            it.uploadComplete == true
        }

        val uploadFail = releaseInfo.photos.filter { photo ->
            !photo.isAdd
        }.filter {
            it.uploadComplete == false
        }.isNotEmpty()

        if (totalSize == uploadSucc.size) {
            _imageUploadCompletion.value = 1
        }else if (uploadFail && totalSize == uploadComp.size) {
            _imageUploadCompletion.value = 2
        }

    }

    fun releaseTopicNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["content"] = releaseInfo.content
                dic["imgs"] = releaseInfo.photos.filter {
                    !it.isAdd
                }.map { photo ->
                    photo.photoKey
                }.joinToString(",")
                dic["address_info"] = releaseInfo.address
                dic["contact"] = releaseInfo.contact
                dic["tags"] = releaseInfo.tags.map { photo ->
                    photo.id
                }.joinToString(",")
                val response = appService.releaseTopic(dic).awaitResp()
                _releaseSuccess.value = response.code
                if (response.code == 200) {
                    cleanImageUploadCompletion()
                }
            }catch (e: Exception) {
                Log.d("TAG", "release topic error: $e")
            }finally {

            }
        }
    }

    private fun cleanImageUploadCompletion() {
        _imageUploadCompletion.value = null
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
                    val items = when (response.data) {
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
            currentList = currentList.map { it ->
                if (it.id == item.id) {
                    if (item.tag_type == 0) {
                        it.isSelected = !it.isSelected
                    }else{
                        if (item.isSelected) {
                            it.isSelected = !it.isSelected
                        }else {
                            if (currentList.filter { it1 ->
                                    it1.isSelected && it1.tag_type != 0 && it1.tag_type != item.tag_type
                                }.isEmpty()) {
                                it.isSelected = !it.isSelected
                            }else{
                                it.isSelected = false
                            }
                        }
                    }
                }
                it
            }
            val selectType = currentList.filter {
                it.isSelected && it.tag_type != 0
            }.map {
                it.tag_type
            }
            Log.d("TAG", "select types $selectType")
            currentList = currentList.map { it2 ->
                if (it2.tag_type != 0 && selectType.isNotEmpty() && !selectType.contains(it2.tag_type)) {
                    it2.isEnable = false
                }else{
                    it2.isEnable = true
                }
                it2
            }
            _selectTags.value = currentList.filter {
                it.isSelected
            }
        }
    }
}