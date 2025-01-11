package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.home.models.BaseResponse
import com.rescue.flutter_720yun.home.models.CollectionActionModel
import com.rescue.flutter_720yun.home.models.LikeActionModel
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.Call

interface ShowService {
    @FormUrlEncoded
    @POST("api/v1/showinfolist/")
    fun showPageList(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>


    @FormUrlEncoded
    @POST("api/v1/gambitlist/")
    fun gambitList(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v1/releaseshowinfo/")
    fun releaseShowInfo(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v1/showinfolikeaction/")
    fun showInfoLikeNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<LikeActionModel>>

    @FormUrlEncoded
    @POST("api/v1/showcollectionaction/")
    fun showInfoCollectionNetworking(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<CollectionActionModel>>
}