package com.rescue.flutter_720yun.adoption.activity

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.*
import android.graphics.Typeface
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
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
        super.onCreate(state); window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); setupToolbar(getString(R.string.adoption_profile_title))
        render(); observe(vm)
        vm.data.observe(this) { if(it != null) fill() }
        vm.busy.observe(this) { save.isEnabled = it != true && vm.loaded }
        vm.saved.observe(this) { if(it == true) { setResult(Activity.RESULT_OK); finish() } }
        vm.error.observe(this) { e -> if(e != null) {
            e.fields.forEach { (key, value) -> errors[key]?.text = value }
            if(e.errorCode == "PROFILE_CHANGED") { notice("资料已变化，重新加载后请确认再保存"); vm.load(preserveDraft = true) }
        } }
        requireLogin()
    }
    override fun onAuthenticated() { if(!::age.isInitialized || age.parent != content) render(); if(!vm.loaded) vm.load() }
    private fun render() {
        content.removeAllViews(); errors.clear()
        content.label(getString(R.string.adoption_profile_purpose))
        content.label(ProfileInputPolicy.AGE_TITLE)
        age = content.field(ProfileInputPolicy.AGE_TITLE, 3).apply {
            inputType = InputType.TYPE_CLASS_NUMBER; textSize=18f;setTypeface(typeface,Typeface.BOLD)
        }
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
            if(ProfileInputPolicy.age(vm.age)==null) failures["age"] = ProfileInputPolicy.AGE_ERROR
            if(vm.city.isBlank()) failures["residence_city"] = "请选择城市"
            if(vm.district.isBlank()) failures["residence_district"] = "请选择区县"
            if(vm.housing.isBlank()) failures["housing_type"] = "请选择住房情况"
            if(vm.employment.isBlank()) failures["employment_status"] = "请选择工作状态"
            if(failures.isEmpty()) vm.save() else failures.forEach { (key,value) -> errors[key]?.text=value }
        }
        (save as MaterialButton).apply {
            layoutParams=LinearLayout.LayoutParams(-2,-2).apply { gravity=android.view.Gravity.CENTER_HORIZONTAL }
            cornerRadius=(8*resources.displayMetrics.density).toInt()
            backgroundTintList=ColorStateList.valueOf(ContextCompat.getColor(this@AdoptionProfileActivity,R.color.color_system))
            setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(0x88ffffff.toInt(),0xffffffff.toInt())))
            typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        }
        age.doAfterTextChanged { vm.age = it.toString() }
        experience.doAfterTextChanged { vm.experience = it.toString() }
        housing.onItemSelectedListener = selection(housing) { vm.housing = AdoptionLabels.housing.keys.elementAtOrNull(it-1).orEmpty() }
        employment.onItemSelectedListener = selection(employment) { vm.employment = AdoptionLabels.employment.keys.elementAtOrNull(it-1).orEmpty() }
        content.button("重新加载资料") { vm.load() }
        fill()
    }
    override fun onPause() { regionDialog?.dismiss(); regionDialog=null; super.onPause() }
    private fun selection(spinner: Spinner, changed: (Int)->Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
            (view as? TextView)?.apply { textSize=if(position>0) 18f else 16f;setTypeface(typeface,if(position>0) Typeface.BOLD else Typeface.NORMAL) }
            changed(position)
        }
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
    private fun fill() {
        age.setText(vm.age); experience.setText(vm.experience)
        housing.setSelection(AdoptionLabels.housing.keys.indexOf(vm.housing)+1)
        employment.setSelection(AdoptionLabels.employment.keys.indexOf(vm.employment)+1)
        location.textSize=if(vm.city.isBlank()) 16f else 18f
        location.setTypeface(location.typeface,if(vm.city.isBlank()) Typeface.NORMAL else Typeface.BOLD)
        location.text = if(vm.city.isBlank()) "选择城市和区县" else "${vm.city} · ${vm.district}"
        save.isEnabled = vm.loaded && vm.busy.value!=true
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
