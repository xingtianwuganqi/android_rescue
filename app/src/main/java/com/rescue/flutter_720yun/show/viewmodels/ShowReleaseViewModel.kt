package com.rescue.flutter_720yun.show.viewmodels

import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.home.models.CoachReleaseInfo
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.show.models.GambitListModel

class ShowReleaseViewModel: ViewModel() {
    var gambitModel: GambitListModel? = null

    var releaseInfo: CoachReleaseInfo = CoachReleaseInfo(
        mutableListOf(),
        null,
        mutableListOf(
            CoachReleasePhoto(true,
                null,
                null,
                null,
                null
            ),
        ),
        null,
        null
    )
}