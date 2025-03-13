package com.rescue.flutter_720yun.home.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityReleaseTopicBinding
import com.rescue.flutter_720yun.databinding.ActivityTopicReportBinding
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader

class TopicReportActivity : BaseActivity() {

    private var _binding: ActivityTopicReportBinding? = null
    private val binding get() = _binding!!



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_topic_report)
        setupToolbar(resources.getString(R.string.more_report))
        _binding = ActivityTopicReportBinding.bind(baseBinding.contentFrame.getChildAt(2))

        addViewAction()
        addViewModelObserver()
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.refreshLayout.setRefreshHeader(MaterialHeader(this))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(this))
        binding.refreshLayout.setOnRefreshListener {

        }
        binding.refreshLayout.setOnLoadMoreListener {

        }

    }



    override fun addViewModelObserver() {
        super.addViewModelObserver()

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}