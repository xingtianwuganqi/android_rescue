package com.rescue.flutter_720yun.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.text.TextUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.models.AdoptionTime
import com.rescue.flutter_720yun.databinding.FindPetListItemBinding
import com.rescue.flutter_720yun.home.models.FindPetModel
import com.rescue.flutter_720yun.util.toImgUrl

interface FindPetItemClickListener {
    fun userClick(model: FindPetModel?)
    fun likeActionClick(model: FindPetModel?)
    fun collectionClick(model: FindPetModel?)
    fun commentClick(model: FindPetModel?)
    fun getContactClick(model: FindPetModel?)
    fun moreButtonClick(model: FindPetModel?) {}
}
class FindPetListAdapter(var list: MutableList<FindPetModel>, val clickListener: FindPetItemClickListener) : RecyclerView.Adapter<FindPetListAdapter.Holder>() {
    private val expanded=mutableSetOf<Int>()
    private var busy=emptySet<String>()
    class Holder(val b: FindPetListItemBinding): RecyclerView.ViewHolder(b.root) { var boundId: Int?=null }
    override fun getItemCount()=list.size
    fun refreshItem(items: List<FindPetModel>) { list.clear();list.addAll(items);expanded.retainAll(items.mapNotNull { it.findId }.toSet());notifyDataSetChanged() }
    fun setBusy(keys: Set<String>) { busy=keys;notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int)=Holder(FindPetListItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item=list[position];val b=holder.b;val id=item.findId ?: 0
        holder.boundId=id
        b.nickName.text=item.userInfo?.username?.takeIf { it.isNotBlank() } ?: "用户"
        Glide.with(b.headImg).load(item.userInfo?.avator?.toImgUrl()).placeholder(R.drawable.icon_eee).error(R.drawable.icon_eee).into(b.headImg)
        b.timeText.text=listOf(AdoptionTime.display(item.update_time ?: item.create_time).let { if(it.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2} .*"))) it.substring(5,16).replace('-','.') else it },item.address?.replace('.', ' ')?.takeIf { it.isNotBlank() } ?: "地区未填写").joinToString(" · ")
        b.tagTitle.text=when(item.pet_type) { 1->"猫咪";2->"狗狗";else->"宠物" }
        b.content.text=item.desc.orEmpty()
        val open=id in expanded
        b.content.maxLines=if(open) Int.MAX_VALUE else 7
        b.content.ellipsize=if(open) null else TextUtils.TruncateAt.END
        b.expandContent.isVisible=item.desc.orEmpty().isNotEmpty()
        b.expandContent.text=if(open) "收起" else "展开"
        b.content.post {
            val layout=b.content.layout
            if(holder.boundId==id && layout!=null && layout.lineCount>0)
                b.expandContent.isVisible=id in expanded || layout.getEllipsisCount(layout.lineCount-1)>0
        }
        b.expandContent.setOnClickListener { if(id in expanded) expanded.remove(id) else expanded.add(id);val current=list.indexOfFirst { it.findId==id };if(current>=0) notifyItemChanged(current) }
        val available=item.effective==1 && item.is_delete==1
        val contact=item.contact_info?.takeIf { available && item.getedcontact==true && it.isNotBlank() }
        b.findContact.text=if(!available) "找宠已关闭或不可用" else contact?.let { "$it（点击复制）" } ?: "获取联系方式"
        b.findContact.isEnabled=available && "$id:contact" !in busy
        b.likeButton.text="点赞 ${item.likeNum ?: 0}";b.likeButton.setIconResource(if(item.liked==true) R.drawable.icon_zan_se else R.drawable.icon_zan_un)
        b.collectButton.text="收藏 ${item.collectionNum ?: 0}";b.collectButton.setIconResource(if(item.collection==true) R.drawable.icon_collection_se else R.drawable.icon_collection_un)
        b.commentButton.text="评论 ${item.commNum ?: 0}"
        b.likeButton.isEnabled=available && "$id:like" !in busy;b.collectButton.isEnabled=available && "$id:collect" !in busy
        b.commentButton.isEnabled=available
        b.headImg.setOnClickListener { clickListener.userClick(item) };b.nickName.setOnClickListener { clickListener.userClick(item) }
        b.findContact.setOnClickListener { clickListener.getContactClick(item) };b.likeButton.setOnClickListener { clickListener.likeActionClick(item) }
        b.collectButton.setOnClickListener { clickListener.collectionClick(item) };b.commentButton.setOnClickListener { clickListener.commentClick(item) }
        b.moreBtn.setOnClickListener { clickListener.moreButtonClick(item) }
    }
    override fun onViewRecycled(holder: Holder) { holder.boundId=null;Glide.with(holder.b.headImg).clear(holder.b.headImg);super.onViewRecycled(holder) }
}
