package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.models.HomeItem
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("/v1/posts")
    suspend fun getHomeList(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): BaseResponse<List<HomeItem>>
}
