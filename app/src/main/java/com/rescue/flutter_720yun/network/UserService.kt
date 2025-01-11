package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.home.models.BaseResponse
import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface UserService {

    @FormUrlEncoded
    @POST("api/v2/getuserpublish/")
    fun userPublishNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>
}