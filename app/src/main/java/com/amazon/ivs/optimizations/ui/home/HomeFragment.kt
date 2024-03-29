package com.amazon.ivs.optimizations.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.optimizations.R
import com.amazon.ivs.optimizations.common.openFragment
import com.amazon.ivs.optimizations.databinding.FragmentHomeBinding
import com.amazon.ivs.optimizations.ui.precaching.PreCachingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private val viewModel by activityViewModels<PreCachingViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rebufferToLive.setOnClickListener {
            openFragment(R.id.navigation_rebuff_to_live)
        }

        binding.catchUpToLive.setOnClickListener {
            openFragment(R.id.navigation_catch_up_to_live)
        }

        binding.preCaching.setOnClickListener {
            viewModel.play()
            openFragment(R.id.navigation_pre_caching)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.initPlayer(requireContext())
    }
}
