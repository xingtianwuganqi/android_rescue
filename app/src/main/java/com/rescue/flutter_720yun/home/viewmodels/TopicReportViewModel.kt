package com.rescue.flutter_720yun.home.viewmodels

import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.network.HomeService
import com.rescue.flutter_720yun.network.ServiceCreator

class TopicReportViewModel: ViewModel() {

    private val appService = ServiceCreator.create<HomeService>()


}