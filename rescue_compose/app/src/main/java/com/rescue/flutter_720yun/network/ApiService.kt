package com.rescue.flutter_720yun.network

import com.rescue.flutter_720yun.models.HomeItem
import retrofit2.http.GET

interface ApiService {

    @GET("/v1/posts?pageNum=1&pageSize=20")
    suspend fun getHomeList(): BaseResponse<List<HomeItem>>
}
