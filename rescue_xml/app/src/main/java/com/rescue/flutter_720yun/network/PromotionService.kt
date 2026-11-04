package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.adoption.models.V2Response
import com.rescue.flutter_720yun.promotion.models.*
import retrofit2.Response
import retrofit2.http.*

interface PromotionService {
    @GET("api/v2/config/")
    suspend fun configuration(@Query("key") key: String = "BOOST_DURATION_SECONDS"): Response<V2Response<com.google.gson.JsonObject>>
    @GET("api/v2/topics/{id}/promotion-state")
    suspend fun state(@Header("Authorization") auth: String, @Path("id") topic: Int): Response<V2Response<PromotionState>>
    @POST("api/v2/topics/{id}/promotion")
    suspend fun promote(@Header("Authorization") auth: String, @Path("id") topic: Int,
        @Header("Idempotency-Key") key: String, @Body body: PromotionWrite): Response<V2Response<PromotionResult>>
}
