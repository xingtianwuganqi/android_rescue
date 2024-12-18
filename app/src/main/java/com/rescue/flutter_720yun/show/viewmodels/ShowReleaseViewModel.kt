package com.rescue.flutter_720yun.show.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiniu.android.storage.UploadManager
import com.qiniu.android.storage.UploadOptions
import com.rescue.flutter_720yun.home.models.CoachReleaseInfo
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.home.models.TagInfoModel
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.ShowService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.show.models.GambitListModel
import com.rescue.flutter_720yun.show.models.ShowReleaseModel
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class ShowReleaseViewModel: ViewModel() {

    private val homeService = ServiceCreator.create<HomeService>()
    private val showService = ServiceCreator.create<ShowService>()

    private val _checkCode = MutableLiveData<Int?>()
    private val _uploadToken = MutableLiveData<String>()
    private val _imageUploadCompletion = MutableLiveData<Int?>() // 1：成功，2：失败
    private val _releaseSuccess = MutableLiveData<Int?>()

    val checkCode: LiveData<Int?> get() = _checkCode
    val uploadToken: LiveData<String> get() = _uploadToken
    val imageUploadCompletion: LiveData<Int?> get() = _imageUploadCompletion
    val releaseSuccess: LiveData<Int?> get() = _releaseSuccess

    var gambitModel: GambitListModel? = null
    private val uploadManager by lazy {
        UploadManager()
    }
    var releaseInfo: CoachReleaseInfo = CoachReleaseInfo(
        mutableListOf(),
        null,
        mutableListOf(
            CoachReleasePhoto(true,
                null,
                null,
                null,
            ),
        ),
        null,
        null
    )

    var showReleaseModel = ShowReleaseModel(
        instruction = null,
        images = null,
        gambit_id = null
    )

    // 获取上传token
    fun getUploadTokenNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["check"] = "check"
                val response = homeService.getUploadToken(dic).awaitResp()
                _checkCode.value = response.code
                if (response.code == 200) {
                    _uploadToken.value = response.data.token
                }
            }catch (e: Exception) {
                Log.d("TAG", "upload token error $e")
                _checkCode.value = 300
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

    fun showReleaseNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["instruction"] = showReleaseModel.instruction
                dic["imgs"] = releaseInfo.photos.filter {
                    !it.isAdd
                }.map { photo ->
                    photo.photoKey
                }.joinToString(",")
                if (showReleaseModel.gambit_id != null) {
                    dic["gambit_id"] = showReleaseModel.gambit_id
                }
                val response = showService.releaseShowInfo(dic).awaitResp()
                _releaseSuccess.value = response.code
            }catch (e: Exception) {
                Log.d("TAG", "release topic error: $e")
                _releaseSuccess.value = 201
            }finally {

            }
        }
    }
}