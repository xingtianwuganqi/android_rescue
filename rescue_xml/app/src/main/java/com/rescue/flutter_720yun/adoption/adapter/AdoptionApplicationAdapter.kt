package com.rescue.flutter_720yun.adoption.adapter
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.ui.*
import com.rescue.flutter_720yun.util.toImgUrl
class AdoptionApplicationAdapter(private val detail:(AdoptionApplication)->Unit,private val topic:(AdoptionApplication)->Unit, private val copyContact:((AdoptionApplication)->Unit)?=null, private val action:(AdoptionApplication,String)->Unit={_,_->}, private val busy:()->Boolean={false}, private val previews: () -> Map<Int, String> = { emptyMap() }, private val missingPreview:(Int)->Unit={}) : RecyclerView.Adapter<AdoptionApplicationAdapter.Holder>() {
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
                val image=previews()[t.topic_id] ?: t.preview_img
                if(image.isNullOrBlank()) missingPreview(t.topic_id)
                Glide.with(photo).load(image?.toImgUrl()).listener(object: com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?,model: Any?,target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,first: Boolean): Boolean {
                        photo.post { missingPreview(t.topic_id) };return false
                    }
                    override fun onResourceReady(resource: android.graphics.drawable.Drawable,model: Any,target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,source: com.bumptech.glide.load.DataSource,first: Boolean)=false
                }).placeholder(com.rescue.flutter_720yun.R.drawable.icon_eee).error(com.rescue.flutter_720yun.R.drawable.icon_eee).into(photo)
                root.label("帖子 ${t.topic_id} · ${t.content_excerpt.orEmpty()}\n${t.address_info.orEmpty()}")
            } else root.label("帖子 ${t.topic_id} · 帖子已删除")
        }
        app.applicant?.let {
            val avatar=ImageView(root.context); root.addView(avatar,LinearLayout.LayoutParams(80,80))
            Glide.with(avatar).load(it.avatar?.toImgUrl()).into(avatar)
        }
        root.label(app.applicant?.nickname ?: "账号或资料已不可用")
        root.label(AdoptionLabels.summary(app.profile_snapshot.takeUnless { app.topic?.unavailable == true || app.topic?.is_delete == 0 }))
        root.label("申请时资料 · 用户填写\n${AdoptionTime.display(app.created_at)}\n${AdoptionOperations.status(app)}")
        val mine=copyContact!=null
        val contact=if(mine) AdoptionContact.visible(app.topic) else null
        if(contact!=null) root.label("送养人联系方式：$contact").apply {
            setTextColor(androidx.core.content.ContextCompat.getColor(context,com.rescue.flutter_720yun.R.color.color_system))
            setBackgroundColor(0xfff4f8fb.toInt())
        }
        val row=LinearLayout(root.context).apply { orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.END }
        root.addView(row)
        if(contact!=null && app.status=="communicating") row.button("复制联系方式") { copyContact?.invoke(app) }.apply {
            layoutParams=LinearLayout.LayoutParams(-2,-2);isEnabled=!busy()
        }
        AdoptionOperations.actions(app,mine).forEach { code -> row.button(AdoptionOperations.title(code)) { action(app,code) }.apply {
            layoutParams=LinearLayout.LayoutParams(-2,-2);isEnabled=!busy()
        } }
        root.button("查看申请时资料") { detail(app) }.isEnabled=!busy()
        if(app.topic?.let { !it.unavailable && it.is_delete!=0 }==true) root.button("查看原帖") { topic(app) }
    }
}
