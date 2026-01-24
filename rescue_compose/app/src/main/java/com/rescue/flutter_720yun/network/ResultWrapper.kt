package com.rescue.flutter_720yun.network

sealed class ResultWrapper<out T> {

    data class Success<T>(val value: T) : ResultWrapper<T>()

    data class Error(
        val code: Int? = null,
        val message: String
    ) : ResultWrapper<Nothing>()
}
