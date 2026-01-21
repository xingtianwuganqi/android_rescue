package com.rescue.flutter_720yun.util
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

object Tool {

//    public let IMGURL      = "http://img.rxswift.cn/"
//    public let IMGMARK     = "?imageView2/0/q/70|watermark/2/text/QOecn-WRveWkqeWWtQ==/font/5a6L5L2T/fontsize/240/fill/IzY2NjY2Ng==/dissolve/75/gravity/SouthEast/dx/10/dy/10"
//    public let IMGTHUMBHEAD = "?imageView2/0/q/10"
//    public let IMGTHUMBNAIL = "?imageView2/0/q/20"
//    public let IMGTHUMBFOUR = "?imageView2/0/q/40"
//    public let IMGTHUMBSEVEN = "?imageView2/0/q/70"
//    public let IMGTHUMBTHREE = "?imageView2/0/q/30"
//    public let RescueReleased = "RescueReleased"
//    public let ShowReleased   = "ShowReleased"
//    public let SensitiveWords = ["押金","定金","运费"]
//    public let APPSTOREURL = "https://itunes.apple.com/cn/app/id1556673767"


//    public enum UserProtocal: String {
//        case pravicy = "/api/pravicy/"
//        case userAgreen = "/api/useragreen/"
//        case aboutUs = "/api/aboutus/"
//        case instruction = "/api/instruction/"
//        case prevention = "/api/prevention/"
//    }

    const val code = "伍c七Alz1θVx2ψLHNpfωv九nξ捌τD六053λwGμrMνRuegsη八γ陆jOBX8ρ三E9πFS零bδοmkχ7K6PβϵϕoZ五iυU一Jq柒ydYt四QhW4玖κCIαζTaι二σ"

    /**
     * 加密字符串的方法
     * @param codeStr 要加密的字符串
     * @return 加密后的字符串，或者 null 如果加密过程中出现错误
     */
    fun encryptionString(codeStr: String): String? {
        // 生成一个0到99之间的随机索引
        val index = Random.nextInt(0, 100)

        // 检查索引是否在字符串长度范围内
        if (index >= codeStr.length) {
            // 如果索引超出范围，返回null或者可以选择其他处理方式
            return null
        }

        // 获取随机索引位置的字符
        val currentStr = codeStr[index].toString()

        // 对当前字符进行Base64编码
        val currentOne = base64CodingToString(currentStr) ?: return null

        // 创建 "index_<index>" 字符串并进行Base64编码两次
        val indexStr = "index_$index"
        val indexOne = base64CodingToString(indexStr) ?: return null
        val indexTwo = base64CodingToString(indexOne) ?: return null

        // 获取当前日期，格式为 "yyyy年MM年dd年"
        val dateFormat = SimpleDateFormat("yyyy年MM年dd年", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        // 对日期字符串进行Base64编码
        val enString = base64CodingToString(dateStr) ?: return null

        // 将编码后的字符串转换为可变列表，每个字符作为一个元素
        val enArr = enString.map { it.toString() }.toMutableList()

        // 在索引2处插入 "$currentOne"
        if (enArr.size >= 2) {
            enArr.add(2, "\$$currentOne")
        } else {
            enArr.add("\$$currentOne")
        }

        // 在倒数第3个位置插入 "$currentOne"
        if (enArr.size >= 3) {
            enArr.add(enArr.size - 3, "\$$currentOne")
        } else {
            enArr.add("\$$currentOne")
        }

        // 在末尾添加 "$indexTwo"
        enArr.add("\$$indexTwo")

        // 将列表中的元素连接成一个完整的字符串并返回
        return enArr.joinToString("")
    }

    /**
     * Base64编码处理
     * @param obj 要编码的字符串
     * @return 编码后的Base64字符串，或者null如果编码失败
     */
    private fun base64CodingToString(obj: String): String? {
        return try {
            val bytes = obj.toByteArray(StandardCharsets.UTF_8)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Base64解码处理
     * @param obj 要解码的Base64字符串
     * @return 解码后的字符串，或者null如果解码失败
     */
    fun base64CodingDecoding(obj: String): String? {
        return try {
            val decodedBytes = Base64.decode(obj, Base64.NO_WRAP)
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
