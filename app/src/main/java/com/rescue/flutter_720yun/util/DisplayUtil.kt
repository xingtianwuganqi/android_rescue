package com.rescue.flutter_720yun.util

import com.rescue.flutter_720yun.BaseApplication
import android.content.Context

class DisplayUtil {
    companion object {

        /**
         * 获取屏幕宽度
         */
        fun getScreenWidth(context: Context): Int {
            return context.resources.displayMetrics.widthPixels
        }

        /**
         * 获取屏幕高度
         */
        fun getScreenHeight(context: Context): Int {
            return context.resources.displayMetrics.heightPixels
        }

        /**
         * 获取屏幕分辨率
         */
        fun getScreenRatio(context: Context): String {
            return getScreenWidth(context).toString() + "X" + getScreenHeight(context).toString()
        }

        /**
         * dp转px
         */
        fun dip2px(dipValue: Float): Int {
            val scale = BaseApplication.context.resources.displayMetrics.density
            return (dipValue * scale + 0.5f).toInt()
        }

        /**
         * px转dp
         */
        fun px2dip(pxValue: Float): Int {
            val scale = BaseApplication.context.resources.displayMetrics.density
            return (pxValue / scale + 0.5f).toInt()
        }
    }
}
