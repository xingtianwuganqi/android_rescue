package com.rescue.flutter_720yun.user.viewmodels

import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.user.models.BlackDetailItemModel
import com.rescue.flutter_720yun.user.models.BlackDetailModel
import com.rescue.flutter_720yun.user.models.ReleaseReportInfoModel
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class BlackDetailViewModel: ViewModel() {
    private val appService = ServiceCreator.create<UserService>()
    private var _dataModels = MutableLiveData<MutableList<BlackDetailModel>>()
    private val _releaseCompletion = MutableLiveData<Boolean>()
    private val _message = MutableLiveData<String>()

    val dataModels get() = _dataModels
    val releaseCompletion get() = _releaseCompletion
    val message get() = _message
    var blackId: Int = 0

    private var data: MutableList<BlackDetailModel> = mutableListOf(
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_phone),
            ContextCompat.getString(BaseApplication.context, R.string.black_phone_placeholder),
            null,
            null,
            true,
            0,
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_wx),
            ContextCompat.getString(BaseApplication.context, R.string.black_wx_placeholder),
            null,
            null,
            false,
            1,
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_wx_nickname),
            ContextCompat.getString(BaseApplication.context, R.string.black_nickname_placeholder),
            null,
            null,
            false,
            2,
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_id),
            ContextCompat.getString(BaseApplication.context, R.string.black_id_placeholder),
            "2",
            null,
            true,
            3,
        ),
        BlackDetailModel(
            ContextCompat.getString(BaseApplication.context, R.string.black_reason),
            ContextCompat.getString(BaseApplication.context, R.string.black_reason_placeholder),
            null,
            null,
            true,
            4,
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
            true,
            5,
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
        Log.d("TAG","fucking ${_dataModels.value?.first()?.desc}")
        val datas = _dataModels.value
        datas?.get(5)?.photos = photos

        datas?.let {
            _dataModels.value = it
            Log.d("TAG","fucking ${it.first().desc}")

        }
    }

    fun releaseReportNetworking(info: ReleaseReportInfoModel) {
        viewModelScope.launch {
            try {
                val dic = paramDic
                if (info.name != null) {
                    dic["name"] = info.name
                }
                if (info.phone != null) {
                    dic["contact"] = info.phone
                }
                if (info.wx_num != null) {
                    dic["wx_num"] = info.wx_num
                }
                if (info.desc != null) {
                    dic["desc"] = info.desc
                }
                dic["black_type"] = if (info.black_type == "1") 1 else 2
                dic["from_userId"] = UserManager.userId
                dic["imgs"] = info.photos

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

    fun getBlackDetail(blackId: Int) {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["blackId"] = blackId
                val response = appService.getBlackDetail(dic).awaitResp()
                if (response.code == 200) {
                    Log.d("TAG", "response data is ${response.data}")
                    val item = when (response.data) {
                        is Map<*, *> -> {
                            // 初始化Gson实例
                            val gson = Gson()
                            // 尝试将Any转换成Json字符串
                            val jsonString = gson.toJson(response.data)
                            // 创建一个具体类型的TypeToken
                            // 将jsonString解析成List<T>
                            gson.fromJson(jsonString, BlackDetailItemModel::class.java)
                        }// data 为 {}，返回空列表
                        else -> {
                            null
                        }
                    }
                    Log.d("TAG", "uploadList $item")
                    if (item != null) {
                        uploadList(item)
                    }
                }
            }catch (e: Exception) {
                Log.d("TAG", "error $e")
            }
        }
    }

    private fun uploadList(item: BlackDetailItemModel) {
        val data: MutableList<BlackDetailModel> = mutableListOf(
            BlackDetailModel(
                ContextCompat.getString(BaseApplication.context, R.string.black_phone),
                ContextCompat.getString(BaseApplication.context, R.string.black_phone_placeholder),
                item.contact,
                null,
                true,
                0,
            ),
            BlackDetailModel(
                ContextCompat.getString(BaseApplication.context, R.string.black_wx),
                ContextCompat.getString(BaseApplication.context, R.string.black_wx_placeholder),
                item.wx_num,
                null,
                false,
                1,
            ),
            BlackDetailModel(
                ContextCompat.getString(BaseApplication.context, R.string.black_wx_nickname),
                ContextCompat.getString(BaseApplication.context, R.string.black_nickname_placeholder),
                item.name,
                null,
                false,
                2,
            ),
            BlackDetailModel(
                ContextCompat.getString(BaseApplication.context, R.string.black_id),
                ContextCompat.getString(BaseApplication.context, R.string.black_id_placeholder),
                if (item.black_type == 1) "1" else "2",
                null,
                true,
                3,
            ),
            BlackDetailModel(
                ContextCompat.getString(BaseApplication.context, R.string.black_reason),
                ContextCompat.getString(BaseApplication.context, R.string.black_reason_placeholder),
                item.desc,
                null,
                true,
                4,
            ),
            BlackDetailModel(
                ContextCompat.getString(BaseApplication.context, R.string.black_evidence),
                ContextCompat.getString(BaseApplication.context, R.string.black_evidence_placeholder),
                null,
                item.images?.map {
                    CoachReleasePhoto(
                        false,
                        it,
                        null,
                        null,
                        true
                    )
                }?.toMutableList(),
                true,
                5,
            ),
        )
        _dataModels.value = data
    }
}