package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R

class UserAccountSafeActivity : BaseActivity() {

    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_account_safe)
        setupToolbar(resources.getString(R.string.user_account))

    }
}