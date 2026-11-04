package com.rescue.flutter_720yun.home.activity

import android.content.res.ColorStateList
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.activity.AdoptionActivity
import com.rescue.flutter_720yun.adoption.ui.*
import com.rescue.flutter_720yun.findpet.*
import com.rescue.flutter_720yun.home.fragment.LocationSheetFragment
import com.rescue.flutter_720yun.home.models.AddressItem

class FindPetDetailActivity : AdoptionActivity() {
    private val vm by lazy { ViewModelProvider(this)[PetRequestViewModel::class.java] }
    private lateinit var location: Button
    private lateinit var types: RadioGroup
    private lateinit var cat: RadioButton
    private lateinit var dog: RadioButton
    private lateinit var description: EditText
    private lateinit var contact: EditText
    private lateinit var save: Button
    private lateinit var effective: Button
    private lateinit var retry: Button
    private lateinit var stateText: TextView
    private lateinit var closedImage: ImageView
    private var region: LocationSheetFragment?=null
    private var dialog: AlertDialog?=null
    override fun onCreate(state: Bundle?) {
        super.onCreate(state);setupToolbar("找宠小助手")
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        render();observe(vm,false)
        vm.info.observe(this) { if(vm.loaded && ::save.isInitialized && save.parent==content) fill() }
        vm.busy.observe(this) { update() }
        vm.refreshAfterWriteError.observe(this) { update() }
        vm.writeCommitted.observe(this) { if(it==true) { notice("操作成功，正在更新找宠信息");setResult(RESULT_OK) };update() }
        vm.saved.observe(this) { if(it==true) { setResult(RESULT_OK);finish() } }
        requireLogin()
    }
    override fun onAuthenticated() { if(!::save.isInitialized || save.parent!=content) render();if(!vm.loaded) vm.load() }
    private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
    private fun render() {
        content.removeAllViews()
        content.setPadding(0,0,0,dp(20))
        fun row(): LinearLayout = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(20),dp(12),dp(20),dp(12));content.addView(this,LinearLayout.LayoutParams(-1,-2)) }
        fun title(parent: LinearLayout,text: String) { parent.addView(TextView(this).apply { this.text=text;textSize=16f;setTypeface(null,Typeface.BOLD);setTextColor(ContextCompat.getColor(context,R.color.color_content));setPadding(0,0,dp(12),0) }) }
        fun separator() { content.addView(View(this).apply { setBackgroundColor(0xfff4f4f4.toInt()) },LinearLayout.LayoutParams(-1,dp(1)).apply { marginStart=dp(15);marginEnd=dp(15) }) }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL;setPadding(dp(15),dp(20),dp(15),dp(20));setBackgroundColor(0xfff0ecdd.toInt()) }
        content.addView(header,LinearLayout.LayoutParams(-1,-2))
        val avatar=ImageView(this).apply { importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        header.addView(avatar,LinearLayout.LayoutParams(dp(66),dp(66)))
        Glide.with(avatar).load(R.drawable.icon_cat_header).override(dp(66),dp(66)).circleCrop().into(avatar)
        val bubble=FrameLayout(this).apply { setPadding(dp(12),dp(12),dp(12),dp(12));setBackgroundColor(0xffcdd1ba.toInt()) }
        header.addView(bubble,LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=dp(10) })
        bubble.addView(TextView(this).apply {
            text="没有找到想要的宠物？\n可以提交相关信息给小助手，有合适的宠物后会通知您~"
            textSize=14f;setTextColor(ContextCompat.getColor(context,R.color.color_node));setPadding(dp(10),dp(8),dp(10),dp(8));setBackgroundColor(android.graphics.Color.WHITE)
        },FrameLayout.LayoutParams(-1,-2))
        closedImage=ImageView(this).apply { setImageResource(R.drawable.icon_find_hadclose);importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        bubble.addView(closedImage,FrameLayout.LayoutParams(dp(72),dp(72),Gravity.END or Gravity.TOP))
        stateText=content.label("").apply { setPadding(dp(20),dp(8),dp(20),dp(8));textSize=12f;setTextColor(ContextCompat.getColor(context,R.color.color_desc)) }
        val typeRow=row();title(typeRow,"类别：")
        types=RadioGroup(this).apply { orientation=RadioGroup.HORIZONTAL }
        typeRow.addView(types,LinearLayout.LayoutParams(0,-2,1f))
        fun choice(text: String)=RadioButton(this).apply {
            id=View.generateViewId();this.text=text;textSize=16f;minHeight=dp(48);setPadding(dp(8),0,dp(8),0)
            buttonDrawable=StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_checked),ContextCompat.getDrawable(this@FindPetDetailActivity,R.drawable.icon_pet_select))
                addState(intArrayOf(),ContextCompat.getDrawable(this@FindPetDetailActivity,R.drawable.icon_pet_unselect))
            }
            types.addView(this,RadioGroup.LayoutParams(0,-2,1f))
        }
        cat=choice("猫咪");dog=choice("狗狗")
        types.setOnCheckedChangeListener { _,id -> vm.type=if(id==dog.id) 2 else 1 }
        separator()
        content.label("描述：").apply { setPadding(dp(20),dp(16),dp(20),dp(8));setTypeface(null,Typeface.BOLD) }
        description=content.field("请输入简短的描述",Int.MAX_VALUE).apply { inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE;minLines=5;setPadding(dp(8),dp(8),dp(8),dp(8));background=GradientDrawable().apply { setColor(android.graphics.Color.WHITE);setStroke(dp(1),0xffeeeeee.toInt()) }
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { marginStart=dp(20);marginEnd=dp(20);bottomMargin=dp(12) }
        }
        separator()
        val addressRow=row();title(addressRow,"地址：")
        location=content.button("选择省 / 市 / 区县") {
            region=LocationSheetFragment().also { sheet ->
                sheet.selectCompletion={ items ->
                    vm.address=items.joinToString(".") { it.name };vm.cityId=resolveCity(vm.address);location.text=vm.address
                }
                sheet.show(supportFragmentManager,"find_pet_city")
            }
        }
        content.removeView(location)
        addressRow.addView(location,LinearLayout.LayoutParams(0,-2,1f))
        location.gravity=Gravity.START or Gravity.CENTER_VERTICAL;location.setPadding(0,0,0,0)
        location.backgroundTintList=ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        location.setTextColor(ContextCompat.getColor(this,R.color.color_content))
        separator()
        val contactRow=row();title(contactRow,"联系方式：")
        contact=content.field("手机号或微信等联系方式",Int.MAX_VALUE)
        content.removeView(contact);contactRow.addView(contact,LinearLayout.LayoutParams(0,-2,1f));contact.background=null
        separator()
        content.label("发布后，其他用户可通过平台获取你留下的联系方式。不希望继续被联系，可以关闭找宠信息。").apply { textSize=12f;setPadding(dp(20),dp(10),dp(20),dp(10));setTextColor(ContextCompat.getColor(context,R.color.color_desc)) }
        save=content.button("发布") { saveRequest() }
        effective=content.button("关闭找宠") {
            val open=vm.info.value?.effective!=1
            dialog=AlertDialog.Builder(this).setMessage(if(open) "确认重新开启找宠信息？" else "关闭后将停止新增推荐和后续获取联系，已复制的联系方式无法追回。确认关闭？")
                .setNegativeButton("取消",null).setPositiveButton("确认") { d,_ -> d.dismiss();vm.setEffective(open) }.show()
        }
        (effective as MaterialButton).setIconResource(R.drawable.icon_more_close)
        retry=content.button("点击重试加载资料") { vm.load() }
        for(index in 0 until content.childCount) (content.getChildAt(index) as? MaterialButton)?.apply {
            val primary=this==save
            backgroundTintList=ColorStateList.valueOf(ContextCompat.getColor(context,if(primary) R.color.color_system else R.color.adoption_panel_background))
            setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(0xff999999.toInt(),ContextCompat.getColor(context,if(primary) R.color.white else R.color.color_content))))
            cornerRadius=if(primary) dp(28) else dp(8)
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { marginStart=dp(20);marginEnd=dp(20);topMargin=dp(8) }
            if(primary) { textSize=20f;minHeight=dp(50) }
        }
        description.doAfterTextChanged { vm.description=it.toString() };contact.doAfterTextChanged { vm.contact=it.toString() }
        fill()
    }
    private fun resolveCity(address: String): String? {
        val gson=Gson()
        val provinces=resources.openRawResource(R.raw.location).reader().use { gson.fromJson(it,Array<AddressItem>::class.java).toList() }
        val cities=resources.openRawResource(R.raw.city).reader().use { gson.fromJson(it,Array<AddressItem>::class.java).toList() }
        return FindPetCities.resolve(address,provinces,cities)
    }
    private fun saveRequest() {
        vm.description=description.text.toString();vm.contact=contact.text.toString()
        vm.validate()?.let { notice(it);return }
        vm.cityId=resolveCity(vm.address)
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken ?: content.windowToken,0)
        currentFocus?.clearFocus()
        vm.save()
    }
    private fun fill() {
        types.check(if(vm.type==2) dog.id else cat.id)
        location.text=vm.address.ifBlank { "选择省 / 市 / 区县" };description.setText(vm.description);contact.setText(vm.contact);update()
    }
    private fun update() {
        if(!::save.isInitialized) return
        val busy=vm.busy.value==true;val info=vm.info.value;val committed=vm.writeCommitted.value==true
        stateText.text=vm.refreshAfterWriteError.value ?: when {
            busy && !vm.loaded -> "正在加载找宠资料…";!vm.loaded -> "找宠资料未加载成功，请点击重试"
            info==null -> "发布领养意愿";info.effective==1 -> "找宠中";else -> "找宠已关闭"
        }
        closedImage.isVisible=info?.effective==2
        save.text=when { info==null -> "发布";info.effective!=1 -> "修改并重新开启找宠";else -> "修改并提交" }
        save.isEnabled=!busy && vm.loaded && !committed
        effective.isVisible=false;effective.isEnabled=!busy && vm.loaded && !committed
        effective.text=if(info?.effective==1) "关闭找宠" else "重新开启找宠"
        retry.isVisible=!busy && (!vm.loaded || vm.refreshAfterWriteError.value!=null)
        location.isEnabled=!busy && !committed;cat.isEnabled=!busy && !committed;dog.isEnabled=!busy && !committed
        description.isEnabled=!busy && !committed;contact.isEnabled=!busy && !committed
        invalidateOptionsMenu()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0,1001,0,"更多").setIcon(R.drawable.icon_detail_more).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(1001)?.apply { isVisible=vm.info.value!=null;isEnabled=vm.loaded && vm.busy.value!=true && vm.writeCommitted.value!=true }
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==1001) {
            dialog=AlertDialog.Builder(this).setItems(arrayOf(if(vm.info.value?.effective==1) "关闭找宠" else "重新开启找宠","取消")) { _,index -> if(index==0) effective.callOnClick() }.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onPause() { dialog?.dismiss();region?.dismissAllowingStateLoss();region=null;super.onPause() }
}
