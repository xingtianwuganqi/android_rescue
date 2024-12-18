package com.rescue.flutter_720yun.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.activity.LoginActivity
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.show.models.ShowPageModel
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Locale
import java.time.format.DateTimeFormatter
import kotlin.random.Random

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

fun String.toastString() {
    Toast.makeText(BaseApplication.context, this, Toast.LENGTH_SHORT).show()
}

fun String.imageResourcesId(): Int {
    val packageName = "com.rescue.flutter_720yun"
    return BaseApplication.context.resources.getIdentifier(this, "drawable", packageName)
}

fun HomeListModel.getImages(): List<String>? {
    return if (!this.preview_img.isNullOrEmpty()) {
        this.preview_img
    }else{
        this.imgs
    }
}

fun ShowPageModel.getImages(): List<String>? {
    return this.imgs
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

/**
 * 加载图片，支持高度伸缩
 * @param context 上下文
 * @param url 图片地址
 * @param imageView 图片组件
 */
fun loadScaleImage(context: Context, url: String, imageView: ImageView) {
    Glide.with(context)
        .asBitmap()
        .load(url)
        .error(R.drawable.icon_eee)
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                val imageWidth = resource.width
                val imageHeight = resource.height

                // 计算 ImageView 的高度
                val imageViewWidth = DisplayUtil.getScreenWidth(BaseApplication.context) - 30.dpToPx()
                val imageViewHeight = (imageViewWidth.toFloat() / imageWidth * imageHeight).toInt()

                // 设置 ImageView 的高度
                val params = imageView.layoutParams
                params.height = imageViewHeight
                imageView.layoutParams = params

                // 将图片显示在 ImageView 中
                imageView.setImageBitmap(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // 可选的清除操作
            }
        })
}

fun lazyLogin(activity: Context, callback: () -> Unit) {
    if (UserManager.isLogin) {
        callback()
    }else{
        val intent = Intent(activity, LoginActivity::class.java)
        activity.startActivity(intent)
    }
}


var params = mapOf<String, Any?>(
    "appType" to "android",
    "appVersion" to "1.0.2",
    "androidVersion" to "8"
)

val paramDic get() = if (UserManager.isLogin) {
    // 深拷贝
    val deepCopiedParams = params.mapValues {
        when (it.value) {
            is MutableList<*> -> (it.value as MutableList<*>).toMutableList() // 深拷贝 List
            else -> it.value
        }
    }.toMutableMap()
    deepCopiedParams["token"] = UserManager.token
    deepCopiedParams
}else{
    // 深拷贝
    val deepCopiedParams = params.mapValues {
        when (it.value) {
            is MutableList<*> -> (it.value as MutableList<*>).toMutableList() // 深拷贝 List
            else -> it.value
        }
    }.toMutableMap()
    deepCopiedParams
}

// 时间格式
fun dateFormatter(format: String): String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern(format)
    return current.format(formatter)
}


fun randomString(length: Int): String {
    if (length <= 0) return ""

    val base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val randomString = StringBuilder()

    repeat(length) {
        val randomIndex = (base.indices).random()
        randomString.append(base[randomIndex])
    }

    return randomString.toString()
}
