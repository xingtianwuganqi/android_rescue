package com.rescue.flutter_720yun.home.adapter


import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.TagInfoModel
import com.rescue.flutter_720yun.util.formatTime
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.timeToStr
import com.rescue.flutter_720yun.util.toImgUrl

// 定义点击监听接口
interface OnItemClickListener {

    fun userClick(model: HomeListModel?)
    fun onItemClick(model: HomeListModel?)
    fun onImgClick(model: HomeListModel?, position: Int)
    fun likeActionClick(model: HomeListModel?)
    fun collectionClick(model: HomeListModel?)
    fun commentClick(model: HomeListModel?)
    fun moreButtonClick(model: HomeListModel?)
}

//class HomeListAdapter(private val context: Context, private val listener: OnItemClickListener): PagingDataAdapter<HomeListModel, HomeListViewHolder>(
//    DiffCallback
//) {
//
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeListViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_item, parent, false)
//        return HomeListViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: HomeListViewHolder, position: Int) {
//        val item = getItem(position)
//        holder.bind(context, item, listener)
//    }
//
//    companion object DiffCallback : DiffUtil.ItemCallback<HomeListModel>() {
//        override fun areItemsTheSame(oldItem: HomeListModel, newItem: HomeListModel): Boolean {
//            return oldItem.id == newItem.id
//        }
//
//        override fun areContentsTheSame(oldItem: HomeListModel, newItem: HomeListModel): Boolean {
//            return oldItem == newItem
//        }
//    }
//
//}


class HomeListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val name: TextView = view.findViewById<TextView>(R.id.nick_name)
    val imgView: ImageView = view.findViewById<ImageView>(R.id.head_img)
    val content: TextView = view.findViewById(R.id.content)
    val timeText: TextView = view.findViewById(R.id.time_text)
    val tagInfo: RecyclerView = view.findViewById(R.id.tag_info)
    val imgRecyclerView: RecyclerView = view.findViewById(R.id.img_recyclerview)
    val likeBtn: MaterialButton = view.findViewById(R.id.like_button)
    val collection: MaterialButton = view.findViewById(R.id.collect_button)
    val commentBtn: MaterialButton = view.findViewById(R.id.comment_button)
    var moreButton: ImageButton = view.findViewById(R.id.more_btn)
    val completion: ImageView = view.findViewById(R.id.completion)
    var homeBackLayout: ConstraintLayout = view.findViewById(R.id.home_back)

    fun bind(context: Context, item: HomeListModel?, listener: OnItemClickListener) {
        name.text = item?.userInfo?.username
        name.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        item?.userInfo?.avator?.let {
            val imgStr = it.toImgUrl()
            Glide.with(context)
                .load(imgStr)
                .placeholder(R.drawable.icon_eee)
                .into(imgView)
        }
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        timeText.text = "${item?.create_time?.formatTime()}•${item?.address_info}"

        content.text = item?.content

        if (item?.tagInfos?.isNotEmpty() == true) {
            tagInfo.visibility = View.VISIBLE
            tagInfo.adapter = TagInfoAdapter(item.tagInfos)
            tagInfo.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            val paddingTop = 26 * context.resources.displayMetrics.density
            content.setPadding(0, paddingTop.toInt(), 0, 0)
        }else{
            tagInfo.visibility = View.GONE
            content.setPadding(0, 0, 0, 0)
        }

        // 设置
        if ((item?.getImages()?.size ?: 0) > 1) {
            imgRecyclerView.layoutManager = GridLayoutManager(context, 2)
            val images = item?.getImages()?.slice(0..1) ?: listOf("", "")
            imgRecyclerView.adapter = TopicImgAdapter(item?.getImages() ?: emptyList(), images, object : TopicImgAdapter.OnChildItemClickListener {
                override fun onChildItemClick(position: Int) {
                    listener.onImgClick(item, position)
                }
            })
        }else{
            imgRecyclerView.layoutManager = GridLayoutManager(context, 1)
            imgRecyclerView.adapter = TopicImgAdapter(item?.getImages() ?: emptyList(), item?.getImages() ?: emptyList(), object : TopicImgAdapter.OnChildItemClickListener {
                override fun onChildItemClick(position: Int) {
                    listener.onImgClick(item, position)
                }
            })
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

        if (item?.commNum != 0) {
            commentBtn.text = "${item?.commNum}"
        }else{
            val commonText = ContextCompat.getString(BaseApplication.context, R.string.home_common)
            commentBtn.text = commonText
        }

        if (item?.collectioned == true) {
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

        if (item?.is_complete == true) {
            completion.visibility = View.VISIBLE
        }else{
            completion.visibility = View.GONE
        }

        name.setOnClickListener{
            listener.userClick(item)
        }

        imgView.setOnClickListener{
            listener.userClick(item)
        }

        homeBackLayout.setOnClickListener{
            listener.onItemClick(item)
        }

        tagInfo.setOnClickListener{
            listener.onItemClick(item)
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

        moreButton.setOnClickListener{
            listener.moreButtonClick(item)
        }
    }
}

class HomeLoadStateAdapter: LoadStateAdapter<HomeLoadStateViewHolder>() {

    override fun onBindViewHolder(holder: HomeLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): HomeLoadStateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.load_state_view, parent, false)
        return HomeLoadStateViewHolder(view)
    }
}

class HomeLoadStateViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
    val errorMessage = view.findViewById<TextView>(R.id.errorMessage)

    fun bind(loadState: LoadState) {
        // 控制 ProgressBar 的可见性
        when (loadState) {
            is LoadState.Loading -> {
                // 显示加载中的 ProgressBar
                progressBar.visibility = View.VISIBLE
                errorMessage.visibility = View.VISIBLE
                errorMessage.text = "加载中..."
            }
            is LoadState.Error -> {
                progressBar.visibility = View.GONE
                errorMessage.visibility = View.VISIBLE
                // 显示具体的错误信息
                errorMessage.text = "加载出错"
            }
            is LoadState.NotLoading -> {
                // 隐藏所有状态视图
                progressBar.visibility = View.GONE
                errorMessage.visibility = View.VISIBLE
                errorMessage.text = "没有更多了"
            }
        }
    }
}


class TagInfoAdapter(private val tagList: List<TagInfoModel>): RecyclerView.Adapter<TagInfoAdapter.ViewHandler>() {

    inner class ViewHandler(view: View): RecyclerView.ViewHolder(view) {
        val tagText: TextView = view.findViewById<TextView>(R.id.tag_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHandler {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tag_item, parent, false)
        return ViewHandler(view)
    }

    override fun getItemCount(): Int {
        return tagList.size
    }

    override fun onBindViewHolder(holder: ViewHandler, position: Int) {
        val value = tagList[position]
        holder.tagText.text = value.tag_name
        val background = GradientDrawable()
// 设置背景颜色
        val colorValue = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
        background.setColor(colorValue)
        val cornerRadius: Float = 5F
        background.cornerRadius = cornerRadius
// 将这个背景应用到TextView
        holder.tagText.background = background
    }
}

