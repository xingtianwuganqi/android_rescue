package com.rescue.flutter_720yun.demo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.Navigator
import com.rescue.flutter_720yun.common.ErrorComposable
import com.rescue.flutter_720yun.common.LoadingComposable
import com.rescue.flutter_720yun.common.UiState
import com.rescue.flutter_720yun.models.HomeItem
import com.rescue.flutter_720yun.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel
) {
    val uiState = viewModel.uiState
    val isRefreshing = uiState is UiState.Loading

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    val state = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Home", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                },
            )
        },
        contentColor = Color.Black,
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier.padding(innerPadding), // ⭐关键
            state = state,
            isRefreshing = uiState is UiState.Loading,
            onRefresh = { viewModel.refresh() }
        ) {
            when (uiState) {
                is UiState.Loading -> {
                    LoadingComposable()
                }

                is UiState.Success -> {
                    val list = uiState.data
                    HomeList(list,
                        onLoadMore = {
                            viewModel.loadMore()
                        },
                        clickItem = {
                            navController.navigate("home/detail/${it.id}")
                        })
                }


                is UiState.Error -> {
                    ErrorComposable(uiState.message)
                }
            }
        }
    }

}

@Composable
fun HomeList(
    list: List<HomeItem>,
    onLoadMore: () -> Unit,
    clickItem: (HomeItem) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(list) { item ->
            Text(
                text = item.content ?: "test",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { clickItem(item) })
                    .padding(16.dp),
            )
        }

        item {
            Text(
                text = "加载中...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    // ⭐ 监听是否滑到底部
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { index ->
                if (index == list.lastIndex) {
                    onLoadMore()
                }
            }
    }
}


@Composable
fun HomeItemView(item: HomeItem) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(item.content ?: "test")
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.desc ?: "fuck")
    }
}


@Composable
fun HomeDetail(
    navController: NavController,
    id: Int
) {
    Text("home detail $id")
}

@Composable
fun Page(navController: NavController) {
    val current = LocalContext.current
    val activity = current.findActivity()
    BackHandler{
        AppActivityManager.finishAll()
    }
    Text("page")
}

@Composable
fun Page2() {
    val current = LocalContext.current
    val activity = current.findActivity()
    BackHandler{
        activity?.finish()
    }
    Text("page2")
}


fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
