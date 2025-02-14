package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.home.models.BaseResponse
import com.rescue.flutter_720yun.home.models.UserInfoModel
import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface UserService {

    @FormUrlEncoded
    @POST("api/v2/getuserpublish/")
    fun userPublishNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v2/getusershowpublish/")
    fun userShowPublishNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v2/useridgetuserinfo/")
    fun userIdGetUserInfo(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<UserInfoModel>>

    @FormUrlEncoded
    @POST("api/v1/suggestion/")
    fun suggestionNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v1/updatetokenpassword/")
    fun changePasswordNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("/api/v2/login/user/delete")
    fun deleteAccount(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("/api/topic/black/list/")
    fun blackList(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>
}