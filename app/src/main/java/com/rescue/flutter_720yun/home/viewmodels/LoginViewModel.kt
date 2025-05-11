package com.rescue.flutter_720yun.home.viewmodels

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.home.models.BaseResponse
import com.rescue.flutter_720yun.home.models.UserInfo
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.Tool
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.isValidEmail
import com.rescue.flutter_720yun.util.paramDic
import com.rescue.flutter_720yun.util.toMD5
import kotlinx.coroutines.CoroutineScope

class LoginViewModel: ViewModel() {

    private var appService = ServiceCreator.create<AppService>()
    private val _loginStatus = MutableLiveData<BaseResponse<UserInfo>?>()
    private val _agreementStatus = MutableLiveData(false)
    private val _showPasswordStatus = MutableLiveData(false)
    private val _countDownNum = MutableLiveData<Long>(60)
    private val _isCountDowning = MutableLiveData(false)
    private val _checkCodeSucc = MutableLiveData<Boolean?>()
    private val _findPasswordStatus = MutableLiveData<Boolean?>()

    val loginStatus: LiveData<BaseResponse<UserInfo>?> get() = _loginStatus
    val agreement: LiveData<Boolean> get() = _agreementStatus
    val showPassword: LiveData<Boolean> get() = _showPasswordStatus
    val checkCodeStatus: LiveData<Boolean?> get() = _checkCodeSucc
    val findPasswordStatus: LiveData<Boolean?> get() = _findPasswordStatus
    val countDownStatus: LiveData<Long> get() = _countDownNum
    val isCountDowning: LiveData<Boolean> get() = _isCountDowning

    private var countDownTimer: CountDownTimer? = null

    suspend fun loginNetworking(phone: String, password: String) {
        val passwordMD5String = password.toMD5()
        val dic = paramDic
        dic["password"] = passwordMD5String
        if (phone.isValidEmail()) {
            dic["email"] = phone
        }else{
            dic["phoneNum"] = phone
        }
        dic["phone_type"] = "android"
        val response = appService.login(dic).awaitResp()
        if (response.code == 200) {
            val userInfo = response.data
            UserManager.setUserInfo(userInfo)
            _loginStatus.value = response
        }else{
            _loginStatus.value = response
        }
    }

    // 获取验证码
    suspend fun getCodeNetworking(phone: String): BaseResponse<Any> {
        val code = Tool.encryptionString(Tool.code) ?: ""
        val dic = paramDic
        dic["code"] = code
        if (phone.isValidEmail()) {
            dic["email"] = phone
        }else{
            dic["phone"] = phone
        }
        return appService.getVerificationCode(dic).awaitResp()
    }

    // 验证验证码
    suspend fun checkCodeNetworking(phone: String, code: String) {
        val dic = paramDic
        dic["code"] = code
        if (phone.isValidEmail()) {
            dic["email"] = phone
        }else{
            dic["phone"] = phone
        }
        val response = appService.checkCode(dic).awaitResp()
        _checkCodeSucc.value = response.code == 200
    }

    // 登录
    suspend fun register(phone: String, password: String) {
        val dic = paramDic
        dic["password"] = password.toMD5()
        dic["confirm_password"] = password.toMD5()
        if (phone.isValidEmail()) {
            dic["email"] = phone
        }else{
            dic["phoneNum"] = phone
        }
        dic["phone_type"] = "android"
        val response = appService.register(dic).awaitResp()
        if (response.code == 200) {
            val userInfo = response.data
            UserManager.setUserInfo(userInfo)
            _loginStatus.value = response
        }else{
            _loginStatus.value = response
        }
    }

    // 找回密码
    suspend fun findPassword(phone: String, password: String, confirm_phone: String) {
        val dic = paramDic
        dic["password"] = password.toMD5()
        dic["confirm_password"] = password.toMD5()
        if (phone.isValidEmail()) {
            dic["email"] = phone
        }else{
            dic["phoneNum"] = phone
        }
        val response = appService.uploadPassword(dic).awaitResp()
        _findPasswordStatus.value = response.code == 200
    }

    // 倒计时
    fun startCountDown() {
        _isCountDowning.value = true
        val value: Long = 61000
        countDownTimer = object : CountDownTimer(value,1000){//1000ms运行一次onTick里面的方法
            override fun onFinish() {
                Log.d("TAG","==倒计时结束")
                _isCountDowning.value = false
                _countDownNum.value = 0
            }
            override fun onTick(millisUntilFinished: Long) {
                _countDownNum.value = millisUntilFinished/1000
            }
        }
        countDownTimer?.start()
    }

    fun stopCountDown() {
        countDownTimer?.onFinish()
    }


    // 清空状态
    fun cleanLoginStatus() {
        _loginStatus.value = null
    }

    fun cleanFindPasswordStatus() {
        _findPasswordStatus.value = null
    }

    fun cleanCheckStatus() {
        _checkCodeSucc.value = null
    }

    fun changeAgreementStatus(finish: Boolean) {
        _agreementStatus.value = finish
    }

    fun changeShowPasswordStatus(show: Boolean) {
        _showPasswordStatus.value = show
    }

//    fun countDown(
//        timeMillis: Long = 1000,//默认时间间隔 1 秒
//        time: Int = 3,//默认时间为 3 秒
//        start: (scop: CoroutineScope) -> Unit,
//        end: () -> Unit,
//        next: (time: Int) -> Unit,
//        error: (msg: String?) -> Unit
//    ) {
//        CoroutineScope.launch {
//            flow {
//                (time downTo 1).forEach {
//                    delay(timeMillis)
//                    emit(it)
//                }
//            }.onStart {
//                start(this@launch)
//            }.onCompletion {
//                end()
//            }.catch {
//                error(it.message ?: "countDown 出现未知错误")
//            }.collect {
//                next(it)
//            }
//        }
//    }
}