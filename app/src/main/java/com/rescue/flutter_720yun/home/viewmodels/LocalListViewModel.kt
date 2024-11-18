package com.rescue.flutter_720yun.home.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.home.models.AddressItem
import com.rescue.flutter_720yun.util.SharedPreferencesUtil
import com.rescue.flutter_720yun.util.UiState

class LocalListViewModel: ViewModel() {

    private var _cityName = MutableLiveData<String>()
    val cityName: LiveData<String> get() = _cityName

    init {
        _cityName.value = getSharePreInfo()
    }

    private fun getSharePreInfo(): String {
        return SharedPreferencesUtil.getString("local_city", BaseApplication.context) ?: "北京市"
    }

    fun uploadCitySharePreInfo(city: String) {
        _cityName.value = city
        SharedPreferencesUtil.putString("local_city", city, BaseApplication.context)
    }
}