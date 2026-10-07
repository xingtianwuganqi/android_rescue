package com.rescue.flutter_720yun.adoption.activity

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.ui.*
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionProfileViewModel
import com.rescue.flutter_720yun.home.models.AddressItem

class AdoptionProfileActivity : AdoptionActivity() {
    private val vm by lazy { ViewModelProvider(this)[AdoptionProfileViewModel::class.java] }
    private lateinit var age: EditText
    private lateinit var location: Button
    private lateinit var housing: Spinner
    private lateinit var employment: Spinner
    private lateinit var experience: EditText
    private lateinit var save: Button
    private var regionDialog: AlertDialog? = null
    private val errors = mutableMapOf<String, TextView>()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); setupToolbar("领养资料")
        render(); observe(vm)
        vm.data.observe(this) { if(it != null) fill() }
        vm.busy.observe(this) { save.isEnabled = it != true && vm.loaded }
        vm.saved.observe(this) { if(it == true) { setResult(Activity.RESULT_OK); finish() } }
        vm.error.observe(this) { e -> if(e != null) {
            e.fields.forEach { (key, value) -> errors[key]?.text = value }
            if(e.errorCode == "PROFILE_CHANGED") { notice("资料已变化，重新加载后请确认再保存"); vm.load() }
        } }
        requireLogin()
    }
    override fun onAuthenticated() { if(!::age.isInitialized || age.parent != content) render(); if(!vm.loaded) vm.load() }
    private fun render() {
        content.removeAllViews(); errors.clear()
        content.label("一份资料，多次复用。修改将用于之后的新申请，不会改变已提交申请的资料。")
        age = content.field("年龄（周岁，1–120）", 3).apply { inputType = InputType.TYPE_CLASS_NUMBER }
        errors["age"] = content.label("")
        location = content.button("选择城市和区县") { chooseRegion() }
        errors["residence_city"] = content.label("")
        errors["residence_district"] = content.label("")
        housing = content.choice("住房情况", listOf("请选择") + AdoptionLabels.housing.values)
        errors["housing_type"] = content.label("")
        employment = content.choice("工作状态", listOf("请选择") + AdoptionLabels.employment.values)
        errors["employment_status"] = content.label("")
        experience = content.field("养宠经验（选填，最多200字）", 200)
        errors["pet_experience"] = content.label("")
        save = content.button("保存资料") {
            vm.age = age.text.toString(); vm.experience = experience.text.toString()
            vm.housing = AdoptionLabels.housing.keys.elementAtOrNull(housing.selectedItemPosition-1).orEmpty()
            vm.employment = AdoptionLabels.employment.keys.elementAtOrNull(employment.selectedItemPosition-1).orEmpty()
            errors.values.forEach { it.text = "" }
            val failures = mutableMapOf<String, String>()
            if(vm.age.toIntOrNull() !in 1..120) failures["age"] = "请输入1–120的整数年龄"
            if(vm.city.isBlank()) failures["residence_city"] = "请选择城市"
            if(vm.district.isBlank()) failures["residence_district"] = "请选择区县"
            if(vm.housing.isBlank()) failures["housing_type"] = "请选择住房情况"
            if(vm.employment.isBlank()) failures["employment_status"] = "请选择工作状态"
            if(failures.isEmpty()) vm.save() else failures.forEach { (key,value) -> errors[key]?.text=value }
        }
        age.doAfterTextChanged { vm.age = it.toString() }
        experience.doAfterTextChanged { vm.experience = it.toString() }
        housing.onItemSelectedListener = selection { vm.housing = AdoptionLabels.housing.keys.elementAtOrNull(it-1).orEmpty() }
        employment.onItemSelectedListener = selection { vm.employment = AdoptionLabels.employment.keys.elementAtOrNull(it-1).orEmpty() }
        content.button("重新加载资料") { vm.load() }
        fill()
    }
    override fun onPause() { regionDialog?.dismiss(); regionDialog=null; super.onPause() }
    private fun selection(changed: (Int)->Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) = changed(position)
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
    private fun fill() {
        age.setText(vm.age); experience.setText(vm.experience)
        housing.setSelection(AdoptionLabels.housing.keys.indexOf(vm.housing)+1)
        employment.setSelection(AdoptionLabels.employment.keys.indexOf(vm.employment)+1)
        location.text = if(vm.city.isBlank()) "选择城市和区县" else "${vm.city} · ${vm.district}"
        save.isEnabled = vm.loaded
    }
    private fun chooseRegion() {
        val regions = resources.openRawResource(R.raw.location).reader().use { Gson().fromJson(it, Array<AddressItem>::class.java) }
        fun pick(title: String, items: List<AddressItem>, selected: (AddressItem)->Unit) {
            regionDialog = AlertDialog.Builder(this).setTitle(title).setItems(items.map { it.name }.toTypedArray()) { _,index -> selected(items[index]) }.show()
        }
        pick("选择省份", regions.toList()) { province ->
            pick("选择城市", province.children.orEmpty()) { city ->
                pick("选择区县", AdoptionRegions.districts(city)) { district ->
                    vm.city = city.name; vm.district = district.name; fill()
                }
            }
        }
    }
}
