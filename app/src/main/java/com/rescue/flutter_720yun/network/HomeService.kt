package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.home.models.BaseResponse
import com.rescue.flutter_720yun.home.models.UploadTokenModel
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.Call

interface HomeService {
    @FormUrlEncoded
    @POST("api/v1/gettaglist/")
    fun getTagsNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v1/qiniu/")
    fun getUploadToken(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<UploadTokenModel>>

    @FormUrlEncoded
    @POST("api/v1/releasetopic/")
    fun releaseTopic(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("/api/v2/addresstopiclist/")
    fun localTopicList(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>
}