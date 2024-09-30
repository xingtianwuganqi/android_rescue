package com.rescue.flutter_720yun.util

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ParameterizedTypeImpl(
    private val raw: Class<*>,
    private val args: Array<Type>
) : ParameterizedType {
    override fun getActualTypeArguments(): Array<Type> = args
    override fun getRawType(): Type = raw
    override fun getOwnerType(): Type? = null
}
