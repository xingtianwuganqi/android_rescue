package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.home.models.BaseResponse
import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface MessageService {
    @FormUrlEncoded
    @POST("api/v1/authmessage/")
    fun authMessageSingleList(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v1/systemnotification/")
    fun systemMessageList(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>

    // 评论列表
    @FormUrlEncoded
    @POST("api/v1/commentlist/")
    fun commentList(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>
}