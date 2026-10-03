package com.rescue.flutter_720yun.message.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.rescue.flutter_720yun.adoption.activity.*
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.adoption.ui.notice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Compatibility for older internal intents. Role is verified from application-state, never target. */
class AdoptionNotificationsActivity : AdoptionActivity() {
    private var forwarding=false
    override fun onCreate(state: Bundle?) { super.onCreate(state); setupToolbar("申请"); requireLogin() }
    override fun onAuthenticated() {
        if(forwarding) return
        forwarding=true
        lifecycleScope.launch {
            try {
                val topic=intent.getIntExtra("topic_id",0)
                val id=intent.getIntExtra("application_id",0)
                if((intent.hasExtra("topic_id") && topic<=0) || (intent.hasExtra("application_id") && id<=0)) {
                    notice("申请参数无效"); finish(); return@launch
                }
                val repo=AdoptionRepository()
                val verifiedTopic=if(id>0) repo.application(id).topic?.topic_id else topic.takeIf { it>0 }
                val received=if(verifiedTopic!=null) repo.state(verifiedTopic).can_manage else intent.getStringExtra("role")=="received"
                val next=Intent(this@AdoptionNotificationsActivity,if(received) AdoptionApplicationsActivity::class.java else AdoptionMyApplicationsActivity::class.java)
                if(received && verifiedTopic!=null) next.putExtra("topic_id",verifiedTopic)
                if(id>0) next.putExtra("application_id",id)
                startActivity(next); finish()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { notice((e as? AdoptionError)?.message ?: "暂时无法打开申请，请返回消息页重试"); finish() }
        }
    }
}
