package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.home.models.BaseListResp
import com.rescue.flutter_720yun.home.models.BaseResponse
import com.rescue.flutter_720yun.home.models.CollectionActionModel
import com.rescue.flutter_720yun.home.models.ContactInfoModel
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.LikeActionModel
import com.rescue.flutter_720yun.home.models.SearchKeywordModel
import com.rescue.flutter_720yun.home.models.UserInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface AppService {


    @FormUrlEncoded
    @POST("api/v1/login/")
//    fun login(@Field("phoneNum") phone: String,
//              @Field("password") password: String,
//              @Field("phone_type") type: String = "android"
//    ): Call<BaseResponse<UserInfo>>
    fun login(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<UserInfo>>


    /*
    @Field("phone") phone: String,
    @Field("code") code: String
     */
    @FormUrlEncoded
    @POST("api/v2/tecent/code/")
    fun getVerificationCode(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>


    // 验证验证码
//    @Field("phone") phone: String,
//    @Field("code") code: String,
    @FormUrlEncoded
    @POST("api/v2/tecent/check/")
    fun checkCode(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>

    // 注册
    /*
    @Field("phoneNum") phone: String,
     @Field("password") password: String,
     @Field("confirm_password") confirm: String,
     @Field("phone_type") phontType: String = "android"
     */
    @FormUrlEncoded
    @POST("api/v1/register/")
    fun register(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<UserInfo>>

    /*
    @Field("phoneNum") phone: String,
     @Field("password") password: String,
     @Field("confirm_password") confirm: String,
     */
    // 找回密码
    @FormUrlEncoded
    @POST("api/v1/updatepswd/")
    fun uploadPassword(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<UserInfo>>

    // 点赞
//    @FormUrlEncoded
//    @POST("api/v1/")
    // 详情



}
