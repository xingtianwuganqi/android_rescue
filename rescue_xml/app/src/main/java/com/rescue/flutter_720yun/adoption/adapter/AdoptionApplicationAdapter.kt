package com.rescue.flutter_720yun.adoption.adapter
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.ui.*
import com.rescue.flutter_720yun.util.toImgUrl
class AdoptionApplicationAdapter(private val detail:(AdoptionApplication)->Unit,private val topic:(AdoptionApplication)->Unit, private val copyContact:((AdoptionApplication)->Unit)?=null) : RecyclerView.Adapter<AdoptionApplicationAdapter.Holder>() {
    private var items=emptyList<AdoptionApplication>()
    class Holder(val content:LinearLayout):RecyclerView.ViewHolder(content)
    override fun getItemCount()=items.size
    fun submit(value:List<AdoptionApplication>) { items=value; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent:ViewGroup,type:Int)=Holder(LinearLayout(parent.context).apply {
        orientation=LinearLayout.VERTICAL; setPadding(16,16,16,24); layoutParams=RecyclerView.LayoutParams(-1,-2); isSaveEnabled=false
    })
    override fun onBindViewHolder(holder:Holder,position:Int) {
        val app=items[position]; val root=holder.content; root.removeAllViews()
        app.topic?.let { t ->
            if(!t.unavailable && t.is_delete!=0) {
                val photo=ImageView(root.context); root.addView(photo,LinearLayout.LayoutParams(140,140))
                Glide.with(photo).load(t.preview_img?.toImgUrl()).into(photo)
                root.label("帖子 ${t.topic_id} · ${t.content_excerpt.orEmpty()}\n${t.address_info.orEmpty()}")
            } else root.label("帖子 ${t.topic_id} · 帖子已删除")
        }
        app.applicant?.let {
            val avatar=ImageView(root.context); root.addView(avatar,LinearLayout.LayoutParams(80,80))
            Glide.with(avatar).load(it.avatar?.toImgUrl()).into(avatar)
        }
        root.label(app.applicant?.nickname ?: "账号或资料已不可用")
        root.label(AdoptionLabels.summary(app.profile_snapshot.takeUnless { app.topic?.unavailable == true || app.topic?.is_delete == 0 }))
        root.label("申请时资料 · 用户填写\n${app.created_at.orEmpty()}\n${AdoptionLabels.status(app.status,app.result)}")
        if(copyContact != null) {
            val contact=AdoptionContact.visible(app.topic)
            root.label(if(contact != null) "送养人联系方式：$contact" else "暂无可显示的联系方式")
            if(contact != null) root.button("复制联系方式") { copyContact.invoke(app) }
        }
        root.button("查看申请") { detail(app) }
        if(app.topic?.let { !it.unavailable && it.is_delete!=0 }==true) root.button("查看原帖") { topic(app) }
    }
}
