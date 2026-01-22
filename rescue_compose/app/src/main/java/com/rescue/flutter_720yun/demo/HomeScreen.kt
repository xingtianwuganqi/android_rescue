package com.rescue.flutter_720yun.demo

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Button(onClick = {
        navController.navigate("home/detail/123")
    }){
        Text("home detail")
    }
}


@Composable
fun HomeDetail(navController: NavController, id: Int) {
    Text("home detail $id")
}

@Composable
fun Page() {
    Text("page")
}

@Composable
fun Page2() {
    Text("page2")
}