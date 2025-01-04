package com.rescue.flutter_720yun.message.viewmodels

import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.message.models.MessageSingleListModel
import com.rescue.flutter_720yun.network.MessageService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.show.models.CommentItemModel
import com.rescue.flutter_720yun.show.models.CommentListModel
import com.rescue.flutter_720yun.show.models.ReplyListModel
import com.rescue.flutter_720yun.util.CommonViewModelInterface
import com.rescue.flutter_720yun.util.RefreshState
import com.rescue.flutter_720yun.util.UiState
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.convertAnyToList
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class CommentListViewModel: ViewModel(), CommonViewModelInterface {

    private val appService = ServiceCreator.create<MessageService>()
    private val _isLoading = MutableLiveData(false)
    private val _isFirstLoading = MutableLiveData(true)
    private val _isLastPage = MutableLiveData(false)
    private val _refreshState = MutableLiveData<RefreshState>()
    private val _uiState = MutableLiveData<UiState<List<CommentItemModel>>>()
    private val _errorMsg = MutableLiveData<String>()

    override val isLoading: LiveData<Boolean>
        get() = _isLoading

    override val isFirstLoading: LiveData<Boolean>
        get() = _isFirstLoading

    override val isLastPage: LiveData<Boolean>
        get() = _isLastPage

    override val refreshState: LiveData<RefreshState>
        get() = _refreshState


    val uiState get() = _uiState
    val errorMsg get() = _errorMsg

    private var page: Int = 1

    var topicType: Int? = null
    var topicId: Int? = null
    // 这个帖子的userId
    var toUid: Int? = null

    // 当前要回复的回复
    var currentCommentModel: CommentListModel? = null

    // 当前要回复的评论
    var currentReplyModel: ReplyListModel? = null

    // 存储所有的数据源
    private var dataSource: MutableList<CommentListModel> = mutableListOf()

    fun commentListNetworking(refresh: RefreshState) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                if (refresh == RefreshState.REFRESH) {
                    page = 1
                    _isLastPage.value = false
                }
                if (refresh == RefreshState.MORE && _isLastPage.value == true) {
                    return@launch
                }
                if (_isFirstLoading.value == true) {
                    _uiState.value = UiState.FirstLoading
                }
                _isLoading.value = true
                _refreshState.value = refresh
                val dic = paramDic
                dic["page"] = page
                dic["size"] = 10
                dic["topic_type"] = topicType
                dic["topic_id"] = topicId
                Log.d("TAG","Message dic is $dic")
                val response = appService.commentList(dic).awaitResp()
                _isFirstLoading.value = false
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data, CommentListModel::class.java)
                            (homeList ?: emptyList())
                        }
                        is Map<*, *> -> {
                            emptyList()
                        }// data 为 {}，返回空列表
                        else -> {
                            emptyList()
                        }
                    }

                    if (page == 1) {
                        if (items.isNotEmpty()) {
                            dataSource = items.toMutableList()
                            val newValue = setCommentList(dataSource)
                            _uiState.value = UiState.Success(newValue)
                            page += 1
                        }else {
                            val noMoreData =
                                BaseApplication.context.resources.getString(R.string.no_data)
                            _uiState.value = UiState.Error(noMoreData)
                        }
                    }else {
                        if (items.isNotEmpty()) {
                            dataSource.addAll(items)
                            val newValue = setCommentList(dataSource)
                            _uiState.value = UiState.Success(newValue)
                            page += 1
                        }else {
                            _isLastPage.value = true
                        }

                    }

                }else{
                    if (page == 1) {
                        val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                        _uiState.value = UiState.Error(noMoreData)
                    }
                }

            }catch (_: Exception) {
                if (page == 1) {
                    val noMoreData = BaseApplication.context.resources.getString(R.string.no_data)
                    _uiState.value = UiState.Error(noMoreData)
                }
            }finally {
                _isLoading.value = false
            }
        }
    }

    private fun setCommentList(commentList: List<CommentListModel>): List<CommentItemModel> {
        val items = mutableListOf<CommentItemModel>()
        for (i in commentList) {
            val commentModel = CommentItemModel(type = 1, commentItem = i, replyItem = null)
            items.add(commentModel)
            val replySize = i.replys?.size ?: 0
            if (replySize > 0) {
                i.replys?.let {
                    for (j in it) {
                        val replyModel = CommentItemModel(type = 2,commentItem = null, replyItem = j)
                        items.add(replyModel)
                    }
                }
                if (replySize < (i.reply_count ?: 0)) {
                    val replyBottomModel = CommentItemModel(type = 3,i,null)
                    items.add(replyBottomModel)
                }
            }else{
                continue
            }
        }
        return items
    }


    fun commentActionNetworking(topicId: Int, topicType: Int, content: String, toUid: Int) {
        viewModelScope.launch {
            try {
                if (isLoading.value == true) {
                    return@launch
                }
                _isLoading.value = true
                val dic = paramDic
                dic["topic_id"] = topicId
                dic["topic_type"] = topicType
                dic["content"] = content
                dic["from_uid"] = UserManager.userId
                dic["to_uid"] = toUid
                val response = appService.commentAction(dic).awaitResp()
                Log.d("TAG","${response.data}")
                if (response.code == 200) {
                    if (_uiState.value is UiState.Success) {
                        dataSource.add(0, response.data)
                        val data = setCommentList(dataSource)
                        _uiState.value = UiState.Success(data)
                    }
                }else{
                    _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.comment_fail)
                }

            }catch (e: Exception) {
                _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.network_request_error)
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun replyActionNetworking(topicId: Int, topicType: Int, content: String, toUid: Int, commentId: Int, replyType: Int, replyId: Int) {
        viewModelScope.launch {
            try {
                if (isLoading.value == true) {
                    return@launch
                }
                _isLoading.value = true
                val dic = paramDic
                dic["topic_id"] = topicId
                dic["topic_type"] = topicType
                dic["content"] = content
                dic["from_uid"] = UserManager.userId
                dic["to_uid"] = toUid
                dic["comment_id"] = commentId
                dic["reply_type"] = replyType
                dic["reply_id"] = replyId
                Log.d("TAG", "$dic")
                val response = appService.replyComment(dic).awaitResp()
                Log.d("TAG","${response.data}")
                if (response.code == 200) {
                    if (_uiState.value is UiState.Success) {

                        dataSource.forEach { item ->
                            if (item.comment_id == response.data.comment_id) {
                                val newReplys: MutableList<ReplyListModel> = mutableListOf()
                                newReplys.add(response.data)
                                newReplys.addAll(item.replys ?: mutableListOf())
                                item.replys = newReplys
                                item.reply_count = item.reply_count?.plus(1)
                            }
                        }
                        val data = setCommentList(dataSource)
                        _uiState.value = UiState.Success(data)
                    }
                }else{
                    _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.comment_fail)
                }

            }catch (e: Exception) {
                _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.network_request_error)
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreReplyNetworking(commentId: Int, page: Int) {
        viewModelScope.launch {
            try {
                if (_isLoading.value == true) {
                    return@launch
                }
                _isLoading.value = true
                val dic = paramDic
                dic["comment_id"] = commentId
                dic["page"] = page
                Log.d("TAG","load more dis is $dic")
                val response = appService.loadMoreReply(dic).awaitResp()
                if (response.code == 200) {
                    val items = when (response.data) {
                        is List<*> -> {
                            val homeList = convertAnyToList(response.data, ReplyListModel::class.java)
                            (homeList ?: emptyList())
                        }
                        is Map<*, *> -> {
                            emptyList()
                        }// data 为 {}，返回空列表
                        else -> {
                            emptyList()
                        }
                    }
                    if (items.isNotEmpty()) {
                        dataSource.forEach { item ->
                            if (item.comment_id == commentId) {
                                val newReplys: MutableList<ReplyListModel> = mutableListOf()
                                newReplys.addAll(item.replys ?: mutableListOf())
                                newReplys.addAll(items)
                                item.replys = newReplys
                                item.next_page += 1
                            }
                        }
                        val data = setCommentList(dataSource)
                        _uiState.value = UiState.Success(data)
                    }
                }else{
                    _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.network_request_error)
                }
            }catch (e: Exception) {
                _errorMsg.value = ContextCompat.getString(BaseApplication.context, R.string.network_request_error)
            }finally {
                _isLoading.value = false
            }
        }
    }
}