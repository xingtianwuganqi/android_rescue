package com.rescue.flutter_720yun.show.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentImageBinding

class ImageFragment : Fragment(R.layout.fragment_image) {

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String): ImageFragment {
            val fragment = ImageFragment()
            val bundle = Bundle().apply {
                putString(ARG_IMAGE_URL, imageUrl)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    private lateinit var imageUrl: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentImageBinding.inflate(inflater, container, false)
        imageUrl = arguments?.getString(ARG_IMAGE_URL) ?: ""

        // 使用图片加载库（例如 Glide 或 Picasso）加载图片
        Glide.with(this)
            .load(imageUrl)
            .into(binding.imageV)

        return binding.root
    }
}
