package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.adoption.models.*
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface AdoptionService {
    @GET("api/v2/adoption-profile/me") suspend fun profile(@Header("Authorization") auth: String): Response<V2Response<ProfileData>>
    @PUT("api/v2/adoption-profile/me") suspend fun saveProfile(@Header("Authorization") auth: String, @Body body: ProfileWrite): Response<V2Response<ProfileData>>
    @GET("api/v2/adoptions/{id}/application-state") suspend fun state(@Header("Authorization") auth: String, @Path("id") id: Int): Response<V2Response<ApplicationState>>
    @POST("api/v2/adoptions/{id}/applications") suspend fun apply(@Header("Authorization") auth: String, @Path("id") id: Int,
        @Header("Idempotency-Key") key: String, @Body body: ApplicationWrite): Response<V2Response<AdoptionApplication>>
    @GET("api/v2/adoptions/mine/applications") suspend fun received(@Header("Authorization") auth: String,
        @Query("topic_id") topic: Int?, @Query("status") status: String, @Query("page") page: Int, @Query("size") size: Int = 10): Response<V2Response<V2Page<AdoptionApplication>>>
    @GET("api/v2/adoption-applications/mine") suspend fun mine(@Header("Authorization") auth: String,
        @Query("status") status: String?, @Query("page") page: Int, @Query("size") size: Int = 10): Response<V2Response<V2Page<AdoptionApplication>>>
    @GET("api/v2/adoption-applications/{id}") suspend fun application(@Header("Authorization") auth: String, @Path("id") id: Int): Response<V2Response<AdoptionApplication>>
    @PATCH("api/v2/adoption-applications/{id}") suspend fun action(@Header("Authorization") auth: String, @Path("id") id: Int,
        @Body body: ApplicationAction): Response<V2Response<AdoptionApplication>>
    @POST("api/v2/adoptions/{id}/status") suspend fun topicAction(@Header("Authorization") auth: String, @Path("id") id: Int,
        @Body body: TopicAction): Response<V2Response<ApplicationState>>
    @GET("api/v2/adoption-notifications") suspend fun notifications(@Header("Authorization") auth: String, @Query("page") page: Int,
        @Query("size") size: Int = 10, @Query("role") role: String? = null, @Query("topic_id") topic: Int? = null,
        @Query("unread") unread: Boolean? = null): Response<V2Response<V2Page<AdoptionNotification>>>
    @GET("api/v2/adoption-notifications/unread-count") suspend fun unread(@Header("Authorization") auth: String, @Query("role") role: String? = null, @Query("topic_id") topic: Int? = null): Response<V2Response<UnreadCount>>
    @POST("api/v2/adoption-notifications/{id}/read") suspend fun read(@Header("Authorization") auth: String, @Path("id") id: Int,
        @Body body: Map<String, String> = emptyMap()): Response<V2Response<JsonObject>>
}
