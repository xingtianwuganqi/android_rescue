package com.rescue.flutter_720yun

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rescue.flutter_720yun.databinding.ActivityBaseBinding

open class BaseActivity : AppCompatActivity() {
    private var _binding: ActivityBaseBinding? = null
    protected val baseBinding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityController.addActivity(this)
        _binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityController.removeActivity(this)
        _binding = null
    }

    // 方法用于加载子 Activity 的布局
    protected fun setContentLayout(layoutResID: Int) {
        layoutInflater.inflate(layoutResID, baseBinding.contentFrame, true)
    }

    fun setupToolbar(title: String, showBackButton: Boolean = true) {
        val toolbar = baseBinding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        if (showBackButton) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener {
                finishAction()
                this.finish()
            }
        }
    }

    open fun finishAction() {

    }

}