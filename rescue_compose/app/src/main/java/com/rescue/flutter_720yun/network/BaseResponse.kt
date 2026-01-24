package com.rescue.flutter_720yun.network

data class BaseResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    fun isSuccess(): Boolean = code == 200
}
