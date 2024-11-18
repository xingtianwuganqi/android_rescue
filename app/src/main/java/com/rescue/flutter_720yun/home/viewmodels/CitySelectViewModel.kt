package com.rescue.flutter_720yun.home.viewmodels

import android.content.Context
import android.location.Address
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.AddressItem
import com.rescue.flutter_720yun.home.models.CityModel
import com.rescue.flutter_720yun.util.ParameterizedTypeImpl
import com.rescue.flutter_720yun.util.SharedPreferencesUtil
import com.rescue.flutter_720yun.util.UiState
import java.io.IOException
import java.io.InputStream

class CitySelectViewModel: ViewModel() {

    private var _cityModels = MutableLiveData<UiState<List<AddressItem>>>()
    private var _cityName = MutableLiveData<String>()
    val cityModels: LiveData<UiState<List<AddressItem>>> get() = _cityModels
    val cityName: LiveData<String> get() = _cityName

    init {
        _cityName.value = getSharePreInfo()
        loadCityData()
    }

    private fun loadCityData() {
        loadProvinceData()?.let {
            _cityModels.value = UiState.Success(it)
        }
    }

    private fun loadProvinceData(): List<AddressItem>? {
        val data = readJsonFromRaw(BaseApplication.context, R.raw.location)
        return data?.let {
            convertStringToList(it)
        }
    }

    private fun convertStringToList(jsonString: String): List<AddressItem>? {
        // 初始化Gson实例
        val gson = Gson()

        // 创建一个具体类型的TypeToken
        val listType = ParameterizedTypeImpl(List::class.java, arrayOf(AddressItem::class.java))
        // 将jsonString解析成List<T>
        return try {
            gson.fromJson(jsonString, listType)
        } catch (e: Exception) {
            e.printStackTrace()
            null // 转换失败返回null
        }
    }

    private fun readJsonFromRaw(context: Context, resId: Int): String? {
        return try {
            val inputStream: InputStream = context.resources.openRawResource(resId)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getSharePreInfo(): String {
        return SharedPreferencesUtil.getString("local_city", BaseApplication.context) ?: "北京市"
    }
}