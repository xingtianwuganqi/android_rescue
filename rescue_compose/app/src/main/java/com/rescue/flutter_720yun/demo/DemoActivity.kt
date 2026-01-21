package com.rescue.flutter_720yun.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rescue.flutter_720yun.ui.theme.RescuecomposeTheme

class DemoActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RescuecomposeTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DemoContent(innerPadding)
                }
            }
        }
    }
}

@Composable
fun DemoContent(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) { }
}