package com.rescue.flutter_720yun.adoption.activity

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.activity.LoginActivity
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel
import com.rescue.flutter_720yun.adoption.ui.*

open class AdoptionActivity : BaseActivity() {
    protected lateinit var content: LinearLayout
    private var loginPending = false
    private var observedSession = UserManager.sessionRevision.value
    private var observedUser = UserManager.userId
    private val login = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loginPending = false
        if(UserManager.isLogin) onAuthenticated() else finish()
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); loginPending = state?.getBoolean("login_pending") ?: false; setContentLayout(R.layout.activity_adoption_container)
        content = findViewById(R.id.adoption_content)
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBoolean("login_pending", loginPending); super.onSaveInstanceState(outState) }
    protected fun requireLogin() {
        if(UserManager.isLogin) onAuthenticated() else if(!loginPending) {
            loginPending = true; login.launch(Intent(this, LoginActivity::class.java))
        }
    }
    protected open fun onAuthenticated() = Unit
    protected fun observe(vm: AdoptionViewModel, replaceContentOnError: Boolean = true) {
        vm.error.observe(this) { error -> if(error != null) {
            notice(error.message)
            if(replaceContentOnError && error.status in listOf(403,404)) {
                content.removeAllViews(); content.label(error.message)
                content.button("重新加载") { onAuthenticated() }
            }
            if(error.status == 401 && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) requireLogin()
        } }
        UserManager.sessionRevision.observe(this) session@{ current ->
            if(current == observedSession) return@session
            val previousUser = observedUser
            observedSession = current; observedUser = UserManager.userId
            content.removeAllViews()
            if(UserManager.isLogin && previousUser != null && previousUser != UserManager.userId) finish()
            else if(UserManager.isLogin) onAuthenticated()
            else { content.label("请登录后查看"); requireLogin() }
        }
    }
}
