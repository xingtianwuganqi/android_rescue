package com.rescue.flutter_720yun.util

import com.google.gson.Gson
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.models.HomeListModel
import java.security.MessageDigest
import java.util.Locale

// Int Extension
fun Int.dpToPx(): Int {
    return (this * BaseApplication.context.resources.displayMetrics.density).toInt()
}


// String Extension
fun String.timeToStr(): String {
    val newText = this.split(".").first()
    return newText.replace("T", " ")
}

fun String.toMD5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }.uppercase(Locale.ROOT) // 转换为大写
}

fun String.toImgUrl(): String {
    return if (this.contains("http")) {
        this
    }else{
        "http://img.rxswift.cn/${this}"
    }
}

fun HomeListModel.getImages(): List<String>? {
    return if (!this.preview_img.isNullOrEmpty()) {
        this.preview_img
    }else{
        this.imgs
    }
}

fun <T> convertAnyToList(anyObject: Any, clazz: Class<T>): List<T>? {
    // 初始化Gson实例
    val gson = Gson()

    // 尝试将Any转换成Json字符串
    val jsonString = gson.toJson(anyObject)

    // 创建一个具体类型的TypeToken
    val listType = ParameterizedTypeImpl(List::class.java, arrayOf(clazz))

    // 将jsonString解析成List<T>
    return try {
        gson.fromJson(jsonString, listType)
    } catch (e: Exception) {
        e.printStackTrace()
        null // 转换失败返回null
    }
}
