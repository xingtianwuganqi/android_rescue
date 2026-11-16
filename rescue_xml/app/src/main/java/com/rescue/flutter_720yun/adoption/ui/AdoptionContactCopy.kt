package com.rescue.flutter_720yun.adoption.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.models.AdoptionContact
import com.rescue.flutter_720yun.adoption.models.AdoptionApplication
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.localized

object AdoptionContactCopy {
    val SAFETY_TIP get() = localized(R.string.adoption_verify_warning,"请核实宠物与送养人信息，谨防收费领养、押金和诈骗；不要提前转账。")
    fun copy(host: FragmentActivity, vm: AdoptionViewModel, applicationId: Int, refreshed: (AdoptionApplication) -> Unit = {}) {
        vm.run {
            val app=vm.repository.application(applicationId)
            val contact=AdoptionContact.visible(app.topic)
            if(!host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@run
            refreshed(app)
            if(app.applicant?.user_id!=UserManager.userId || contact==null) {
                host.notice(host.getString(R.string.adoption_no_contact_refresh));return@run
            }
            val clip = ClipData.newPlainText(host.getString(R.string.adoption_owner_contact_title),contact).apply {
                description.extras = android.os.PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE",true) }
            }
            (host.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
            host.notice(host.getString(R.string.adoption_contact_copied))
        }
    }
}
