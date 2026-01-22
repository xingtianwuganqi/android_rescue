package com.rescue.flutter_720yun

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rescue.flutter_720yun.demo.BottomTab
import com.rescue.flutter_720yun.demo.DemoActivity
import com.rescue.flutter_720yun.demo.HomeScreen
import com.rescue.flutter_720yun.ui.theme.RescuecomposeTheme
import androidx.navigation.compose.composable
import com.rescue.flutter_720yun.demo.Page
import com.rescue.flutter_720yun.demo.Page2
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.rescue.flutter_720yun.demo.HomeDetail


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RescuecomposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
    Spacer(modifier = Modifier.padding(16.dp))
    Button(
        onClick = {

        }
    ) {
        Text(text = "点击")
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val tabs = listOf(
        BottomTab.Home,
        BottomTab.Profile,
        BottomTab.Settings
    )
    Scaffold(
        bottomBar = {
            NavigationBar() {
                val currentRouter = currentRoute(navController)
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRouter == tab.route,
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null
                            )
                        },
                        label = { Text(text = tab.title) },
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId){
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            navigation(
                startDestination = "home/main",
                route = BottomTab.Home.route
            ) {
                composable("home/main") {
                    HomeScreen(navController)
                }

                composable("home/detail/{id}",
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.IntType
                            }
                        )
                    ) {
                    val id = it.arguments?.getInt("id") ?: 0
                    HomeDetail(navController, id)
                }

            }

            navigation(
                startDestination = "page/home",
                route = "page"
            ) {
                composable("page/home") {
                    Page()
                }
                composable("page/detail") {
                    Page2()
                }
            }

            navigation(
                startDestination = "settings/home",
                route = "settings"
            ) {
                composable("settings/home") {
                    Page2()
                }
            }
        }

    }
}

@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RescuecomposeTheme {
        Greeting("Android")
    }
}