package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService

class UserViewModel: ViewModel() {
    val appService = ServiceCreator.create<UserService>()

    var userId: Int? = null
}