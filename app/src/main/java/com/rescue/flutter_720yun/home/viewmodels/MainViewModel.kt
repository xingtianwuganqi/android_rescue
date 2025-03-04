package com.rescue.flutter_720yun.home.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.DrawerListModel

class MainViewModel: ViewModel() {
    private var _drawerList = MutableLiveData<List<DrawerListModel>>()
    val drawerList get() = _drawerList

    init {
        _drawerList.value = listOf(
            DrawerListModel("icon_view_hist",
                BaseApplication.context.resources.getString(R.string.drawer_footer),
                null
            ),
            DrawerListModel("icon_mi_collection",
                BaseApplication.context.resources.getString(R.string.drawer_collect),
                null
            ),
            DrawerListModel("icon_me_black",
                BaseApplication.context.resources.getString(R.string.drawer_black),
                null
            ),
            DrawerListModel("icon_ado_about",
                BaseApplication.context.resources.getString(R.string.drawer_rescue),
                null
            ),
//            DrawerListModel("icon_mi_upload",
//                BaseApplication.context.resources.getString(R.string.drawer_upload),
//                null
//            ),
            DrawerListModel("icon_mi_xy",
                BaseApplication.context.resources.getString(R.string.drawer_agreement),
                null
            ),
            DrawerListModel("icon_pravicy",
                BaseApplication.context.resources.getString(R.string.drawer_privacy),
                null
            ),
            DrawerListModel("icon_mi_about",
                BaseApplication.context.resources.getString(R.string.drawer_about),
                null
            ),
//            DrawerListModel("icon_mi_help",
//                BaseApplication.context.resources.getString(R.string.drawer_help),
//                null
//            ),
//            DrawerListModel("icon_mi_tui",
//                BaseApplication.context.resources.getString(R.string.drawer_recommend),
//                null
//            ),
        )
    }
}