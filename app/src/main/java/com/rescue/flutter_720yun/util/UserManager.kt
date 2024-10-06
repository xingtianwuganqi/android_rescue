package com.rescue.flutter_720yun.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.models.UserInfo

object UserManager {
    private var _userInfo: UserInfo? = null
    val isLogin: Boolean get() = _userInfo != null
    val token: String? get() = _userInfo?.token
    val userId: Int?  get() = _userInfo?.id
    fun setUserInfo(info: UserInfo) {
        val sharedPreferences = BaseApplication.context.getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userinfo", Gson().toJson(info))
        editor.apply()
        _userInfo = info
    }

    fun getUserInfo() {
        val sharedPreferences =
            BaseApplication.context.getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        val userInfoStr = sharedPreferences.getString("userinfo", "")
        _userInfo = if ((userInfoStr?.length ?: 0) > 0) {
            val info = Gson().fromJson(userInfoStr, UserInfo::class.java)
            info
        }else{
            null
        }
    }
}
