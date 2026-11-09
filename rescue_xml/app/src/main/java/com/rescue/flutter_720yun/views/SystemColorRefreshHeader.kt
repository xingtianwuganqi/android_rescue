package com.rescue.flutter_720yun.views

import android.content.Context
import android.util.AttributeSet
import com.rescue.flutter_720yun.R
import com.scwang.smart.refresh.header.MaterialHeader

/** Use the same loading color for headers inflated from XML or created in code. */
class SystemColorRefreshHeader @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : MaterialHeader(context,attrs) {
    init { setColorSchemeResources(R.color.color_system) }
}
