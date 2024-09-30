package com.rescue.flutter_720yun.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.activity.LoginActivity
import com.rescue.flutter_720yun.databinding.FragmentHomeBinding
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), OnItemClickListener {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!
    private lateinit var homeAdapter: HomeListAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView: RecyclerView = binding.recyclerview
        recyclerView.layoutManager = LinearLayoutManager(context)

        context?.let {
            homeAdapter = HomeListAdapter(it, this)
            recyclerView.adapter = homeAdapter.withLoadStateFooter(
                footer = HomeLoadStateAdapter()
            )


        }


        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            homeAdapter.refresh()
        }

        lifecycleScope.launch {
            homeViewModel.items.collectLatest {
                homeAdapter.submitData(it)
                swipeRefreshLayout.isRefreshing = false
            }
        }

        homeAdapter.addLoadStateListener { loadState ->
            when(loadState.refresh) {
                is LoadState.Loading -> {
                    swipeRefreshLayout.isRefreshing = true
                }

                is LoadState.NotLoading ->{
                    swipeRefreshLayout.isRefreshing = false
                }

                is LoadState.Error -> {
                    val errorState = loadState.refresh as LoadState.Error
                    Log.e("Paging error", "Error: ${errorState.error.message}")
                }
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun userClick(model: HomeListModel?) {

    }

    override fun onItemClick(model: HomeListModel?) {

    }

    override fun onImgClick(model: HomeListModel?, position: Int) {

    }

    override fun likeActionClick(model: HomeListModel?) {
        if (UserManager.isLogin) {

        }else{
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun collectionClick(model: HomeListModel?) {
        if (UserManager.isLogin) {

        }else{
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun commentClick(model: HomeListModel?) {
        if (UserManager.isLogin) {

        }else{
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

}