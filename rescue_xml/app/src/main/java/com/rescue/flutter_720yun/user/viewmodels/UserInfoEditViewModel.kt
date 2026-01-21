package com.rescue.flutter_720yun.user.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qiniu.android.storage.UploadManager
import com.qiniu.android.storage.UploadOptions
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.home.models.UserInfoModel
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch
import com.luck.picture.lib.entity.LocalMedia

class UserInfoEditViewModel: ViewModel() {

    private val appService = ServiceCreator.create<HomeService>()

    private val _userInfo = MutableLiveData<UserInfoModel>()
    private val _checkCode = MutableLiveData<Int>()
    private val _uploadToken = MutableLiveData<String>()
    private val _releasePhoto = MutableLiveData<CoachReleasePhoto>()
    private val _uploadComplete = MutableLiveData<Boolean>()
    private val _uploadSucc = MutableLiveData<Boolean>()

    val userInfo: LiveData<UserInfoModel> get() = _userInfo
    val checkCode: LiveData<Int> get() = _checkCode
    val uploadToken: LiveData<String> get() = _uploadToken
    val releasePhoto: LiveData<CoachReleasePhoto> get() = _releasePhoto
    val uploadComplete: LiveData<Boolean> get() = _uploadComplete
    val uploadSucc: LiveData<Boolean> get() = _uploadSucc

    private val uploadManager by lazy {
        UploadManager()
    }

    fun uploadUserModel(userInfo: UserInfoModel) {
        _userInfo.value = userInfo
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
                if (_releasePhoto.value?.media?.path == null || _releasePhoto.value?.photoKey == null) {
                    return@launch
                }
                val options = UploadOptions(null, null, true,
                    { key, percent ->

                    }
                ) { false }

                Log.d("TAG", "media path is ${releasePhoto.value?.media?.path}, photoKey is ${releasePhoto.value?.photoKey}")
                if (_releasePhoto.value?.media?.compressPath != null) {
                    uploadManager.put(
                        _releasePhoto.value?.media!!.compressPath,
                        _releasePhoto.value?.photoKey,
                        token,
                        { key, info, response ->
                            Log.d("TAG", "key is $key, info is $info, response is $response",)
                            if (info != null && info.isOK) {
                                if (_releasePhoto.value?.photoKey == key) {
                                    _releasePhoto.value?.uploadComplete = true
                                }
                                _uploadComplete.value = true
                            } else {
                                if (_releasePhoto.value?.photoKey == key) {
                                    _releasePhoto.value?.uploadComplete = false
                                }
                                _uploadComplete.value = false
                            }
                        }, options
                    )
                }
            }catch (e: Exception) {
                Log.d("TAG", "upload fail $e")
            }finally {

            }
        }
    }

    fun uploadNetworking(nickName: String, avatar: String?) {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["username"] = nickName
                if (avatar != null) {
                    dic["avator"] = avatar
                }
                val response = appService.uploadUserInfo(dic).awaitResp()
                if (response.code == 200) {
                    _uploadSucc.value = true
                }else{
                    _uploadSucc.value = false
                }
            }catch (e: Exception) {
                _uploadSucc.value = false
            }
        }
    }

    fun uploadReleasePhoto(photo: CoachReleasePhoto) {
        _releasePhoto.value = photo
    }

    fun uploadUserInfo(avatar: String) {
        _userInfo.value?.avator = avatar
    }

    fun uploadNickname(nickname: String) {
        _userInfo.value?.username = nickname
    }
}