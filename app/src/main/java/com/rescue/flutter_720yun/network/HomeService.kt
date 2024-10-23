package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.models.BaseResponse
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.Call

interface HomeService {
    @FormUrlEncoded
    @POST("api/v1/gettaglist/")
    fun getTagsNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>
}