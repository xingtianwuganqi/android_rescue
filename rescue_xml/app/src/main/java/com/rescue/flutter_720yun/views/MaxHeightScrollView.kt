package com.rescue.flutter_720yun.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * ScrollView that wraps its content when it is short, but never grows beyond [maxHeight].
 * Used by AlertDialogs with long bodies so the bottom button panel stays on screen without
 * leaving a large blank area for short content.
 */
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ScrollView(context, attrs) {

    var maxHeight: Int = 0
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (maxHeight > 0 && measuredHeight > maxHeight) {
            setMeasuredDimension(measuredWidth, maxHeight)
        }
    }
}
