package com.rescue.flutter_720yun.user.viewmodels

import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.user.models.BlackDetailModel
import com.rescue.flutter_720yun.user.models.ReleaseReportInfoModel
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class BlackDetailViewModel: ViewModel() {
    private val appService = ServiceCreator.create<UserService>()
    private var _dataModels = MutableLiveData<List<BlackDetailModel>>()
    private val _releaseCompletion = MutableLiveData<Boolean>()
    private val _message = MutableLiveData<String>()

    val dataModels get() = _dataModels
    val releaseCompletion get() = _releaseCompletion
    val message get() = _message

    private var data: MutableList<BlackDetailModel> = mutableListOf(
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_phone),
            ContextCompat.getString(BaseApplication.context, R.string.black_phone_placeholder),
            null,
            null,
            true
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_wx),
            ContextCompat.getString(BaseApplication.context, R.string.black_wx_placeholder),
            null,
            null,
            false
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_wx_nickname),
            ContextCompat.getString(BaseApplication.context, R.string.black_nickname_placeholder),
            null,
            null,
            false
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_id),
            ContextCompat.getString(BaseApplication.context, R.string.black_id_placeholder),
            null,
            null,
            true
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_reason),
            ContextCompat.getString(BaseApplication.context, R.string.black_reason_placeholder),
            null,
            null,
            true
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_evidence),
            ContextCompat.getString(BaseApplication.context, R.string.black_evidence_placeholder),
            null,
            mutableListOf(
                CoachReleasePhoto(
                true,
                null,
                null,
                null
            )),
            true
        ),
    )
    var photos = mutableListOf(
        CoachReleasePhoto(
            true,
            null,
            null,
            null
        )
    )


    init {
        _dataModels.value = data
    }


    fun uploadImageData() {
        val item = data.last()
        item.photos = photos

        data[5] = item
        _dataModels.value = data
    }

    fun releaseReportNetworking(info: ReleaseReportInfoModel) {
        viewModelScope.launch {
            try {
                /*
                parameter["name"] = info.name
            parameter["contact"] = info.phone
            parameter["wx_num"] = info.wx_num
            parameter["desc"] = info.desc
            parameter["imgs"] = info.photos.map({ model in
                return model.photoKey
            }).joined(separator: ",")
            parameter["black_type"] = info.black_type
            parameter["from_userId"] = UserManager.shared.userId
            parameter["token"] = UserManager.shared.token
                 */
                val dic = paramDic
                dic["name"] = info.name ?: ""
                dic["contact"] = info.phone ?: ""
                dic["wx_num"] = info.wx_num ?: ""
                dic["desc"] = info.desc ?: ""
                dic["black_type"] = info.black_type ?: 1
                dic["from_userId"] = UserManager.userId
                Log.d("TAG","$dic")
                val response = appService.blackRelease(dic).awaitResp()
                if (response.code == 200) {
                    _releaseCompletion.value = true
                    _message.value = BaseApplication.context.getString(R.string.push_success)
                }else{
                    _releaseCompletion.value = false
                    _message.value = BaseApplication.context.getString(R.string.push_fail)

                }
            }catch (e: Exception) {
                _releaseCompletion.value = false
                _message.value = BaseApplication.context.getString(R.string.push_fail)
            }
        }
    }
}