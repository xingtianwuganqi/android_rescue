package com.rescue.flutter_720yun.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityHomeDetailBinding

class HomeDetailActivity : AppCompatActivity() {

    private var _binding: ActivityHomeDetailBinding? = null
    val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHomeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}