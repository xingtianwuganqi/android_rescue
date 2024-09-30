package com.rescue.flutter_720yun.ui.home
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.reflect.TypeToken
import com.rescue.flutter_720yun.network.AppService
import com.rescue.flutter_720yun.network.awaitResponse
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.util.convertAnyToList

class HomePagingSource(
    private val apiService: AppService // 你的网络服务接口
) : PagingSource<Int, HomeListModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HomeListModel> {
        return try {
            // 当前页码，第一页时默认为 1
            val currentPage = params.key ?: 1
            // 从 API 获取数据
            val response = apiService.getTopicList(currentPage, 10, 0).awaitResponse()
            Log.d("TAG", response.data.toString())
            // 获取响应数据列表
            // 判断 data 的类型
            val items = when (response.data) {
                is List<*> -> {
                    val homeList = convertAnyToList(response.data, HomeListModel::class.java)
                    (homeList ?: emptyList())
                }

                is Map<*, *> -> {
                    emptyList()
                }// data 为 {}，返回空列表
                else -> {
                    emptyList()
                }
            }

            LoadResult.Page(
                data = items,
                prevKey = if (currentPage == 1) null else currentPage - 1,
                nextKey = if (items.isEmpty()) null else currentPage + 1
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }



    override fun getRefreshKey(state: PagingState<Int, HomeListModel>): Int? {
        // 返回刷新时的页码
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

