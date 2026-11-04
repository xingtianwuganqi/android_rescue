package com.rescue.flutter_720yun.adoption

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.adapter.AdoptionApplicationAdapter
import com.rescue.flutter_720yun.adoption.models.AdoptionApplication
import com.rescue.flutter_720yun.adoption.ui.AdoptionFilterBar
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Artificial cards only; no authenticated requests, clipboard, or application writes. */
@RunWith(AndroidJUnit4::class)
class AdoptionApplicationCardTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun context(scale: Float = 1f): android.content.Context {
        val base = instrumentation.targetContext
        return ContextThemeWrapper(base.createConfigurationContext(Configuration(base.resources.configuration).apply { fontScale = scale }), R.style.Theme_Android_rescue)
    }
    private fun application(status: String = "ended", result: String = "not_completed", acquired: Boolean = false, deleted: Boolean = false) = Gson().fromJson("""
        {"application_id":501,"status":"$status","result":"$result","version":1,
         "end_reason":"owner_ended","end_note":"拒绝沟通申请","created_at":"2026-10-11T20:34:55+08:00",
         "applicant":{"user_id":1,"nickname":"喵喵酱"},
         "profile_snapshot":{"age":28,"residence_city":"北京市","residence_district":"东城区","housing_type":"with_family","employment_status":"student"},
         "topic":{"topic_id":123,"content_excerpt":"小白很乖很好养","is_complete":false,"is_delete":${if (deleted) 0 else 1},"getedcontact":$acquired,"contact_info":"local-test-contact"}}
    """.trimIndent(), AdoptionApplication::class.java)
    private fun holder(adapter: AdoptionApplicationAdapter, scale: Float = 1f): AdoptionApplicationAdapter.Holder =
        adapter.onCreateViewHolder(RecyclerView(context(scale)), 0)

    @Test fun mineShowsRejectedBadgeAndEmptyContactWithoutApplicantProfile() = instrumentation.runOnMainSync {
        var opened = 0
        val adapter = AdoptionApplicationAdapter({}, { opened++ }, copyContact = {})
        val holder = holder(adapter)
        adapter.submit(listOf(application())); adapter.onBindViewHolder(holder, 0)
        val b = holder.binding
        assertEquals(View.VISIBLE, b.applicationMineHeader.visibility)
        assertEquals(View.GONE, b.applicationReceivedContent.visibility)
        assertEquals("被拒绝", b.applicationMineStatus.text.toString())
        assertEquals("暂无已获取的联系方式", b.applicationContactValue.text.toString())
        assertEquals(View.VISIBLE, b.applicationContact.visibility)
        assertEquals(View.GONE, b.applicationActions.visibility)
        b.applicationTopic.performClick(); assertEquals(1, opened)
    }
    @Test fun receivedUsesSnapshotAndNeverExposesContact() = instrumentation.runOnMainSync {
        var details = 0
        val adapter = AdoptionApplicationAdapter({ details++ }, {})
        val holder = holder(adapter)
        adapter.submit(listOf(application(result = "completed", acquired = true)))
        adapter.onBindViewHolder(holder, 0)
        val b = holder.binding
        assertEquals(View.GONE, b.applicationMineHeader.visibility)
        assertEquals(View.VISIBLE, b.applicationReceivedContent.visibility)
        assertEquals("喵喵酱", b.applicationApplicant.text.toString())
        assertTrue(b.applicationProfileSummary.text.contains("28"))
        assertTrue(b.applicationProfileSummary.text.contains("北京市"))
        assertEquals("已结束 · 完成送养（送养人报告）", b.applicationResult.text.toString())
        assertEquals(View.GONE, b.applicationContact.visibility)
        assertFalse(b.applicationContactValue.text.contains("local-test-contact"))
        b.applicationProfile.performClick(); assertEquals(1, details)
    }
    @Test fun recycledCardDropsActionsAndNavigationForDeletedTopic() = instrumentation.runOnMainSync {
        val adapter = AdoptionApplicationAdapter({}, {})
        val holder = holder(adapter)
        adapter.submit(listOf(application(status = "applying"))); adapter.onBindViewHolder(holder, 0)
        assertEquals(2, holder.binding.applicationActions.childCount)
        adapter.submit(listOf(application(deleted = true))); adapter.onBindViewHolder(holder, 0)
        val b = holder.binding
        assertEquals(View.GONE, b.applicationActions.visibility)
        assertEquals(0, b.applicationActions.childCount)
        assertFalse(b.applicationTopic.isClickable)
        assertEquals(View.GONE, b.applicationTopicArrow.visibility)
        assertEquals("资料已不可用", b.applicationProfileSummary.text.toString())
    }
    @Test fun authorizedContactRemainsVisibleAfterApplicationEndsAndBusyLocksActions() = instrumentation.runOnMainSync {
        var copied = 0
        var busy = false
        val adapter = AdoptionApplicationAdapter({}, {}, copyContact = { copied++ }, busy = { busy })
        val holder = holder(adapter)
        adapter.submit(listOf(application(status = "communicating", acquired = true))); adapter.onBindViewHolder(holder, 0)
        val actions = holder.binding.applicationActions
        assertEquals(2, actions.childCount)
        actions.getChildAt(0).performClick(); assertEquals(1, copied)
        busy = true; adapter.onBindViewHolder(holder, 0)
        assertFalse(actions.getChildAt(0).isEnabled); assertFalse(actions.getChildAt(1).isEnabled)
        adapter.submit(listOf(application(acquired = true))); adapter.onBindViewHolder(holder, 0)
        assertEquals("local-test-contact", holder.binding.applicationContactValue.text.toString())
        assertEquals(View.GONE, actions.visibility)
    }
    @Test fun filterSelectionDoesNotResubmitWhenTappingSelectedSegment() = instrumentation.runOnMainSync {
        val bar = AdoptionFilterBar(context())
        val changes = mutableListOf<String>()
        bar.onSelectionChanged = { changes.add(it) }
        val tabs = (0 until bar.childCount).map { bar.getChildAt(it) }.filterIsInstance<TextView>()
        assertEquals(listOf("全部", "申请中", "沟通中", "已结束"), tabs.map { it.text.toString() })
        assertTrue(tabs.first().isSelected)
        tabs[2].performClick(); tabs[2].performClick()
        assertEquals(listOf("communicating"), changes)
        assertTrue(tabs[2].isSelected); assertFalse(tabs.first().isSelected)
        bar.select("ended"); assertTrue(tabs.last().isSelected)
        assertEquals(listOf("communicating"), changes)
    }
    @Test fun largeFontActionsWrapInsideANarrowCard() = instrumentation.runOnMainSync {
        val adapter = AdoptionApplicationAdapter({}, {}, copyContact = {})
        val holder = holder(adapter, 1.8f)
        adapter.submit(listOf(application(status = "communicating", acquired = true))); adapter.onBindViewHolder(holder, 0)
        val root = holder.binding.root
        val width = (288 * root.resources.displayMetrics.density).toInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        root.layout(0, 0, width, root.measuredHeight)
        fun bounded(group: ViewGroup) {
            for (index in 0 until group.childCount) {
                val view = group.getChildAt(index)
                if (view.visibility == View.GONE) continue
                assertTrue("Child overflows ${view.javaClass.simpleName}", view.left >= 0 && view.right <= group.width)
                if (view is ViewGroup) bounded(view)
            }
        }
        bounded(root)
    }
}
