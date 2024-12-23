package com.rescue.flutter_720yun.home.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.FindPetModel
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.TagInfoModel
import com.rescue.flutter_720yun.util.formatTime
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.timeToStr
import com.rescue.flutter_720yun.util.toImgUrl

interface FindPetItemClickListener {
    fun userClick(model: FindPetModel?)
    fun likeActionClick(model: FindPetModel?)
    fun collectionClick(model: FindPetModel?)
    fun commentClick(model: FindPetModel?)
    fun getContactClick(model: FindPetModel?)
}

class FindPetListAdapter(
    var list: MutableList<FindPetModel>,
    val clickListener: FindPetItemClickListener
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_LOADING = 2

    private var showNoMore = false
    override fun getItemCount(): Int {
        return if (showNoMore) list.size + 1 else list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < list.size) VIEW_TYPE_ITEM else VIEW_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LOADING) {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.loading_item, parent, false)
            return LoadingViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.find_pet_list_item, parent, false)
            return FindPetListViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FindPetListViewHolder) {
            val item = list[position]
            holder.bind(item, clickListener)
        }
    }

    fun refreshItem(newList: List<FindPetModel>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged() // 通知适配器刷新所有项
    }

    fun addItems(newList: List<FindPetModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }

    fun uploadItem(item: FindPetModel) {
        val position = list.indexOfFirst { it.findId == item.findId }
        list[position] = item
        notifyItemChanged(position)
    }

    fun clearItems() {
        list.clear()
        notifyDataSetChanged()
    }

    fun showNoMoreData() {
        showNoMore = true
        notifyItemInserted(list.size)
    }

    fun hideNoMoreData() {
        showNoMore = false
        notifyItemRemoved(list.size)
    }

}

class FindPetListViewHolder(view: View): ViewHolder(view) {
    val name: TextView = view.findViewById(R.id.nick_name)
    private val imageView: ImageView = view.findViewById(R.id.head_img)
    val content: TextView = view.findViewById(R.id.content)
    private val timeText: TextView = view.findViewById(R.id.time_text)
    private val tagInfo: RecyclerView = view.findViewById(R.id.tag_info)
    private val likeBtn: MaterialButton = view.findViewById(R.id.like_button)
    private val collection: MaterialButton = view.findViewById(R.id.collect_button)
    private val commentBtn: MaterialButton = view.findViewById(R.id.comment_button)
    private val contactButton: MaterialButton = view.findViewById(R.id.find_contact)

    fun bind(item: FindPetModel?, listener: FindPetItemClickListener) {
        name.text = item?.userInfo?.username
        name.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        item?.userInfo?.avator?.let {
            val imgStr = it.toImgUrl()
            Glide.with(BaseApplication.context)
                .load(imgStr)
                .placeholder(R.drawable.icon_eee)
                .into(imageView)
        }
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        timeText.text = "${item?.create_time?.formatTime()}•${item?.address}"

        content.text = item?.desc

        var petName = ""
        if (item?.pet_type == 1) {
            petName = "猫咪"
        }else if (item?.pet_type == 2){
            petName = "狗狗"
        }
        tagInfo.visibility = View.VISIBLE
        val tagModel = TagInfoModel(null, petName, null, false, null)
        tagInfo.adapter = TagInfoAdapter(listOf(tagModel))
        tagInfo.layoutManager = LinearLayoutManager(BaseApplication.context, LinearLayoutManager.HORIZONTAL, false)
        val paddingTop = 26 * BaseApplication.context.resources.displayMetrics.density
        content.setPadding(0, paddingTop.toInt(), 0, 0)

        if (item?.getedcontact == true && item.contact_info != null) {
            contactButton.text = item.contact_info
        }else{
            contactButton.text = ContextCompat.getString(BaseApplication.context, R.string.find_contact)
        }

        if (item?.liked == true) {
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_zan_se)
            // 设置新图标
            likeBtn.icon = newIcon
        }else{
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_zan_un)
            // 设置新图标
            likeBtn.icon = newIcon
        }


        if (item?.collection == true) {
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_collection_se)
            // 设置新图标
            collection.icon = newIcon
        }else{
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_collection_un)
            // 设置新图标
            collection.icon = newIcon
        }


        name.setOnClickListener{
            listener.userClick(item)
        }

        imageView.setOnClickListener{
            listener.userClick(item)
        }

        contactButton.setOnClickListener{
            listener.getContactClick(item)
        }

        likeBtn.setOnClickListener{
            listener.likeActionClick(item)
        }

        collection.setOnClickListener {
            listener.collectionClick(item)
        }

        commentBtn.setOnClickListener {
            listener.commentClick(item)
        }

    }
}