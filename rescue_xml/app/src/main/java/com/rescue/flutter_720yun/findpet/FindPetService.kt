package com.rescue.flutter_720yun.findpet

import com.google.gson.JsonObject
import com.rescue.flutter_720yun.adoption.models.V2Response
import retrofit2.Response
import retrofit2.http.*

interface FindPetService {
    @GET("api/v3/find-pet/me/recommendations/")
    suspend fun recommendations(@Header("Authorization") auth: String, @Query("page") page: Int, @Query("size") size: Int = 20): Response<V2Response<PetRecommendations>>
    @POST("api/v3/find-pet/recommendations/{id}/read/")
    suspend fun read(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: JsonObject): Response<V2Response<RecommendationRead>>
    @FormUrlEncoded @POST("api/v2/searchpet/info")
    suspend fun info(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v2/searchpet/create")
    suspend fun save(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v2/searchpet/effective")
    suspend fun effective(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v1/systemnotification/")
    suspend fun notifications(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v2/findpet/list")
    suspend fun list(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v2/find/action/like")
    suspend fun like(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v2/find/action/collect")
    suspend fun collect(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v2/find/collect/list")
    suspend fun collections(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v1/getcontact/")
    suspend fun contact(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
    @FormUrlEncoded @POST("api/v1/authunreadnum/")
    suspend fun systemUnread(@FieldMap fields: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>
}
