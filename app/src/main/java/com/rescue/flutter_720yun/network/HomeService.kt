package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.home.models.BaseListResp
import com.rescue.flutter_720yun.home.models.BaseResponse
import com.rescue.flutter_720yun.home.models.CollectionActionModel
import com.rescue.flutter_720yun.home.models.ContactInfoModel
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.LikeActionModel
import com.rescue.flutter_720yun.home.models.SearchKeywordModel
import com.rescue.flutter_720yun.home.models.UploadTokenModel
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.GET

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


    @FormUrlEncoded
    @POST("api/v1/likeaction/")
    fun topicLikeAction(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<LikeActionModel?>>

    @FormUrlEncoded
    @POST("api/v1/collection/")
    fun topicCollectionAction(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<CollectionActionModel?>>

    // 获取联系方式
    @FormUrlEncoded
    @POST("api/v1/getcontact/")
    fun getTopicContact(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<ContactInfoModel>>

    @FormUrlEncoded
    @POST("api/v1/topiclist/")
    fun getTopicList(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v1/topicdetail/")
    fun topicDetail(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<HomeListModel>>

    @GET("api/v1/searchkeywords/")
    fun getSearchKeyword(): Call<BaseListResp<SearchKeywordModel>>

    @FormUrlEncoded
    @POST("api/v1/search/")
    fun searchList(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v2/findpet/list")
    fun findPetList(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v1/updateuserinfo/")
    fun uploadUserInfo(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>

    // 用户收藏列表
    @FormUrlEncoded
    @POST("/api/v1/authcollection/")
    fun userCollectionList(@FieldMap dic: Map<String, @JvmSuppressWildcards Any?>): Call<BaseResponse<Any>>
}