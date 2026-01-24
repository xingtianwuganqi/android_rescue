package com.rescue.flutter_720yun.demo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    when (uiState) {
        is UiState.Loading -> {
            LoadingComposable()
        }
        is UiState.Success -> {
            val list = uiState.data
            HomeListView(list)
        }
        is UiState.Error -> {
            ErrorComposable(uiState.message)
        }
    }
}

@Composable
fun HomeListView(list: List<HomeItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp)
    ) {
        items( list) { item ->
            HomeItemView(item)
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
