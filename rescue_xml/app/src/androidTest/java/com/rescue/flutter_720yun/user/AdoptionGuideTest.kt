package com.rescue.flutter_720yun.user

import android.content.Intent
import android.text.Spanned
import android.text.style.ClickableSpan
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.activity.ReleaseTopicActivity
import com.rescue.flutter_720yun.home.viewmodels.ReleaseTopicViewModel
import com.rescue.flutter_720yun.user.activity.AdoptAboutActivity
import com.rescue.flutter_720yun.user.activity.WebPageActivity
import com.rescue.flutter_720yun.user.viewmodels.UserSettingViewModel
import com.rescue.flutter_720yun.util.UserManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Read-only UI checks: never submits a post, saves a profile, or changes the account. */
@RunWith(AndroidJUnit4::class)
class AdoptionGuideTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private fun evaluate(activity: WebPageActivity, script: String): String {
        var result = ""
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            activity.findViewById<WebView>(R.id.webView).evaluateJavascript(script) {
                result = it; latch.countDown()
            }
        }
        assertTrue("WebView evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result
    }
    private fun waitForGuide(activity: WebPageActivity) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (evaluate(activity, "document.querySelectorAll('.flow li').length") == "6") return
            Thread.sleep(50)
        }
        fail("Local guide did not load its six steps")
    }

    @Test fun localGuideLoadsSixStepsWithoutNetworkOrAccountBridge() {
        ActivityScenario.launch<WebPageActivity>(WebPageActivity.adoptionFlowIntent(context)).use { scenario ->
            lateinit var activity: WebPageActivity
            scenario.onActivity { activity = it }
            waitForGuide(activity)
            scenario.onActivity {
                val web = it.findViewById<WebView>(R.id.webView)
                assertEquals(WebPageActivity.ADOPTION_FLOW_URL, web.url)
                assertEquals("领养流程说明", it.supportActionBar?.title)
                assertFalse(web.settings.javaScriptEnabled)
                assertTrue(web.settings.blockNetworkLoads)
            }
            assertEquals("true", evaluate(activity, "typeof AccountDeleteClickInterface === 'undefined' && typeof AccountSafeClickInterface === 'undefined'"))
            assertEquals("true", evaluate(activity, "['送养人怎么操作','领养人怎么操作','怎样区分几种状态','如果暂时不再送养','交接前的小提醒','首次保存资料返回后需再次点击申请'].every(t => document.body.innerText.includes(t))"))
            scenario.onActivity { it.findViewById<WebView>(R.id.webView).settings.textZoom = 200 }
            instrumentation.waitForIdleSync()
            assertEquals("true", evaluate(activity, "document.documentElement.scrollWidth <= document.documentElement.clientWidth"))
        }
    }

    @Test fun aboutEntryOpensTheSameReadOnlyGuide() {
        val monitor = instrumentation.addMonitor(WebPageActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch<AdoptAboutActivity>(Intent(context, AdoptAboutActivity::class.java)).use { scenario ->
                scenario.onActivity { it.itemClick(2) }
                val guide = instrumentation.waitForMonitorWithTimeout(monitor, 5000) as? WebPageActivity
                assertNotNull("Missing guide navigation", guide)
                try { waitForGuide(guide!!) }
                finally { instrumentation.runOnMainSync { guide?.finish() } }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun readingFromPublishPreservesContactContentRegionAndPhotos() {
        val monitor = instrumentation.addMonitor(WebPageActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch<ReleaseTopicActivity>(Intent(context, ReleaseTopicActivity::class.java)).use { scenario ->
                var photoKeys: List<String?> = emptyList()
                scenario.onActivity { activity ->
                    activity.findViewById<EditText>(R.id.content_edit).setText("仅用于本地检查的发布草稿")
                    activity.findViewById<EditText>(R.id.contact_edit).setText("本地测试联系")
                    activity.findViewById<EditText>(R.id.address_edit).setText("上海市.浦东新区")
                    val vm = ViewModelProvider(activity)[ReleaseTopicViewModel::class.java]
                    vm.releaseInfo.photos.add(com.rescue.flutter_720yun.home.models.CoachReleasePhoto(false, "qa-local-photo", null, null))
                    photoKeys = vm.releaseInfo.photos.map { it.photoKey }
                    activity.findViewById<EditText>(R.id.contact_edit).requestFocus()
                    val note = activity.findViewById<TextView>(R.id.adoption_flow_note)
                    val text = note.text as Spanned
                    text.getSpans(0, text.length, ClickableSpan::class.java).single().onClick(note)
                    assertFalse(activity.findViewById<EditText>(R.id.contact_edit).hasFocus())
                }
                val guide = instrumentation.waitForMonitorWithTimeout(monitor, 5000) as? WebPageActivity
                assertNotNull(guide)
                try { waitForGuide(guide!!) }
                finally { instrumentation.runOnMainSync { guide?.finish() } }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    assertEquals("仅用于本地检查的发布草稿", activity.findViewById<EditText>(R.id.content_edit).text.toString())
                    assertEquals("本地测试联系", activity.findViewById<EditText>(R.id.contact_edit).text.toString())
                    assertEquals("上海市.浦东新区", activity.findViewById<EditText>(R.id.address_edit).text.toString())
                    assertEquals(photoKeys, ViewModelProvider(activity)[ReleaseTopicViewModel::class.java].releaseInfo.photos.map { it.photoKey })
                }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun settingsKeepStableRoutesWithProfileFirst() {
        instrumentation.runOnMainSync {
            val vm = UserSettingViewModel()
            vm.refreshEntries()
            val routes = vm.messageList.value!!.map { it.category }
            assertEquals(listOf("adoption_profile", "find_pet_profile", "password", "feedback", "account_security") +
                if (UserManager.isLogin) listOf("logout") else emptyList(), routes)
            assertEquals("领养资料", vm.messageList.value!!.first().title)
            assertNotEquals(0, context.resources.getIdentifier(vm.messageList.value!!.first().icon, "drawable", context.packageName))
        }
    }
}
