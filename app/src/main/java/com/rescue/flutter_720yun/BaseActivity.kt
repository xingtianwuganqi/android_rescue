package com.rescue.flutter_720yun

import android.os.Bundle
import android.view.View
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

    open fun addViewModelObserver() {

    }

    open fun addViewAction() {

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

    open fun finishAllActivity() {
        ActivityController.finishAll()
    }

    fun showLoading() {
        baseBinding.progressBar.visibility = View.VISIBLE
        baseBinding.errorView.visibility = View.GONE
        baseBinding.contentFrame.getChildAt(2).visibility = View.GONE
    }

    fun showSuccess() {
        baseBinding.progressBar.visibility = View.GONE
        baseBinding.errorView.visibility = View.GONE
        baseBinding.contentFrame.getChildAt(2).visibility = View.VISIBLE
    }

    fun showError(error: String? = null) {
        baseBinding.progressBar.visibility = View.GONE
        baseBinding.errorView.visibility = View.VISIBLE
        baseBinding.contentFrame.getChildAt(2).visibility = View.GONE
        error?.let {
            baseBinding.errorView.text = error
        }
    }
}