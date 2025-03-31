package com.rescue.flutter_720yun.util

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesUtil {

    private var sps: SharedPreferences?=null

    private fun getSps(context: Context):SharedPreferences{
        if(sps==null){
            sps=context.getSharedPreferences("default",Context.MODE_PRIVATE)
        }
        return sps!!
    }

    fun putString(key:String,value:String?,context:Context){
        if(!value.isNullOrBlank()){
            val editor:SharedPreferences.Editor=getSps(context).edit()
            editor.putString(key,value)
            editor.apply()
        }
    }

    fun getString(key:String,context:Context):String?{
        if(key.isNotBlank()){
            val sps:SharedPreferences=getSps(context)
            return sps.getString(key,null)
        }
        return null
    }

    fun putStringSet(key: String, value: Set<String>, context: Context) {
        if (value.isNotEmpty()) {
            val edit: SharedPreferences.Editor = getSps(context).edit()
            edit.putStringSet(key, value)
            edit.apply()
        }
    }

    fun getStringSet(key: String, context: Context): Set<String>? {
        if (key.isNotBlank()) {
            val sps: SharedPreferences= getSps(context)
            return sps.getStringSet(key, null)
        }
        return null
    }
}