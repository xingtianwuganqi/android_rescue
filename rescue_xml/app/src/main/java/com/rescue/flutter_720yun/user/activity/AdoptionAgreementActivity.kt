package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.adoption.ui.notice
import kotlinx.coroutines.*
import java.util.zip.ZipInputStream

/** Preview the bundled iOS agreement; save the unmodified Word document through SAF. */
class AdoptionAgreementActivity : BaseActivity() {
    private var saving=false
    private val save=registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { uri ->
        if(uri!=null) lifecycleScope.launch {
            saving=true;invalidateOptionsMenu()
            try {
                withContext(Dispatchers.IO) { assets.open("adoption_agreement.docx").use { input -> contentResolver.openOutputStream(uri)?.use { input.copyTo(it) } ?: error("Cannot save") } }
                notice("领养协议已保存")
            } catch(e: CancellationException) { throw e } catch(e: Exception) { notice("保存失败，请稍后重试") }
            finally { saving=false;invalidateOptionsMenu() }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state);setupToolbar("领养协议")
        val text=TextView(this).apply { textSize=16f;setTextColor(android.graphics.Color.DKGRAY);setTextIsSelectable(true);setPadding(24,24,24,24);text="正在加载协议…" }
        baseBinding.contentFrame.addView(ScrollView(this).apply { addView(text) })
        lifecycleScope.launch {
            try {
                text.text=withContext(Dispatchers.IO) {
                    var result=""
                    ZipInputStream(assets.open("adoption_agreement.docx")).use { zip ->
                        while(true) {
                            val entry=zip.nextEntry ?: error("Missing agreement")
                            if(entry.name!="word/document.xml") continue
                            val parser=android.util.Xml.newPullParser();parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);parser.setInput(zip,"UTF-8")
                            val body=StringBuilder();var inText=false
                            while(parser.eventType!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                                when(parser.eventType) {
                                    org.xmlpull.v1.XmlPullParser.START_TAG -> if(parser.name=="t") inText=true else if(parser.name in listOf("tab","br")) body.append(if(parser.name=="tab") "\t" else "\n")
                                    org.xmlpull.v1.XmlPullParser.TEXT -> if(inText) body.append(parser.text)
                                    org.xmlpull.v1.XmlPullParser.END_TAG -> if(parser.name=="t") inText=false else if(parser.name=="p") body.append("\n")
                                };parser.next()
                            };result=body.toString();break
                        }
                    }
                    result
                }
            } catch(e: CancellationException) { throw e } catch(e: Exception) { text.text="协议加载失败，请返回重试" }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean { menu.add("下载").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);return true }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean { menu.getItem(0).isEnabled=!saving;return super.onPrepareOptionsMenu(menu) }
    override fun onOptionsItemSelected(item: MenuItem): Boolean { if(item.title=="下载") { if(!saving) save.launch("领养协议.docx");return true };return super.onOptionsItemSelected(item) }
}
