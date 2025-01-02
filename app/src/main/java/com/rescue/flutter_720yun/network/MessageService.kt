package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.home.models.BaseResponse
import com.rescue.flutter_720yun.show.models.CommentListModel
import com.rescue.flutter_720yun.show.models.ReplyListModel
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

    // 发表评论
    @FormUrlEncoded
    @POST("api/v1/commentaction/")
    fun commentAction(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<CommentListModel>>

    // 发表回复
    @FormUrlEncoded
    @POST("api/v1/replycomment/")
    fun replyComment(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<ReplyListModel>>
}