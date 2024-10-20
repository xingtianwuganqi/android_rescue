package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.models.BaseListResp
import com.rescue.flutter_720yun.models.BaseResponse
import com.rescue.flutter_720yun.models.CollectionActionModel
import com.rescue.flutter_720yun.models.ContactInfoModel
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.models.LikeActionModel
import com.rescue.flutter_720yun.models.SearchKeywordModel
import com.rescue.flutter_720yun.models.UserInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface AppService {
    @FormUrlEncoded
    @POST("api/v1/topiclist/")
    fun getTopicList(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>

    @FormUrlEncoded
    @POST("api/v1/login/")
    fun login(@Field("phoneNum") phone: String,
              @Field("password") password: String,
              @Field("phone_type") type: String = "android"
    ): Call<BaseResponse<UserInfo>>

    @FormUrlEncoded
    @POST("api/v2/tecent/code/")
    fun getVerificationCode(@Field("phone") phone: String,
                            @Field("code") code: String
                            ): Call<BaseResponse<Any>>


    // 验证验证码
    @FormUrlEncoded
    @POST("api/v2/tecent/check/")
    fun checkCode(@Field("phone") phone: String,
                  @Field("code") code: String,
                  ): Call<BaseResponse<Any>>

    // 注册
    @FormUrlEncoded
    @POST("api/v1/register/")
    fun register(@Field("phoneNum") phone: String,
                 @Field("password") password: String,
                 @Field("confirm_password") confirm: String,
                 @Field("phone_type") phontType: String = "android"
    ): Call<BaseResponse<UserInfo>>

    // 找回密码
    @FormUrlEncoded
    @POST("api/v1/updatepswd/")
    fun uploadPassword(@Field("phoneNum") phone: String,
                 @Field("password") password: String,
                 @Field("confirm_password") confirm: String,
    ): Call<BaseResponse<UserInfo>>

    // 点赞
//    @FormUrlEncoded
//    @POST("api/v1/")
    // 详情
    @FormUrlEncoded
    @POST("api/v1/topicdetail/")
    fun topicDetail(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<HomeListModel>>


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

    @GET("api/v1/searchkeywords/")
    fun getSearchKeyword(): Call<BaseListResp<SearchKeywordModel>>

    @FormUrlEncoded
    @POST("api/v1/search/")
    fun searchList(@FieldMap(encoded = false) dic: Map<String, @JvmSuppressWildcards Any?>
    ): Call<BaseResponse<Any>>
}
