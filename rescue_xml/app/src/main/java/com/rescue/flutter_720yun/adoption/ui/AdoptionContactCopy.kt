package com.rescue.flutter_720yun.adoption.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.rescue.flutter_720yun.adoption.models.AdoptionContact
import com.rescue.flutter_720yun.adoption.models.AdoptionApplication
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel
import com.rescue.flutter_720yun.util.UserManager

object AdoptionContactCopy {
    const val SAFETY_TIP = "请核实宠物与送养人信息，谨防收费领养、押金和诈骗；不要提前转账。"
    fun copy(host: FragmentActivity, vm: AdoptionViewModel, applicationId: Int, refreshed: (AdoptionApplication) -> Unit = {}) {
        vm.run {
            val app=vm.repository.application(applicationId)
            val contact=AdoptionContact.visible(app.topic)
            if(!host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@run
            refreshed(app)
            if(app.applicant?.user_id!=UserManager.userId || contact==null) {
                host.notice("当前没有可显示的联系方式，请刷新申请列表");return@run
            }
            (host.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("送养人联系方式",contact))
            host.notice("已复制联系方式")
        }
    }
}
