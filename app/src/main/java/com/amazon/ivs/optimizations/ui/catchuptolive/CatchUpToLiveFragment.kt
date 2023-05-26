package com.amazon.ivs.optimizations.ui.catchuptolive

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.optimizations.common.*
import com.amazon.ivs.optimizations.databinding.FragmentCatchUpToLiveBinding
import com.amazon.ivs.optimizations.ui.models.MEASURE_REPEAT_COUNT
import com.amazon.ivs.optimizations.ui.models.MEASURE_REPEAT_DELAY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class CatchUpToLiveFragment : Fragment() {
    private lateinit var binding: FragmentCatchUpToLiveBinding
    private val viewModel by activityViewModels<CatchUpToLiveViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCatchUpToLiveBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBackButtonAvailable()
        viewModel.captureClickTime()

        collect(viewModel.onInfoUpdate) { infoUpdate ->
            binding.infoUpdate = infoUpdate
        }

        collect(viewModel.onBuffering) { bufferingState ->
            binding.surfaceBuffering = bufferingState
        }

        collect(viewModel.onError) { error ->
            binding.root.showSnackBar(error.errorMessage)
        }

        collect(viewModel.onSizeChanged) { videoSizeState ->
            binding.surfaceView.onReady {
                binding.surfaceView.scaleToFit(videoSizeState)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            binding.playerGuideline.setGuidelinePercent(0.5f)
        } else {
            binding.playerGuideline.setGuidelinePercent(0.3f)
        }

        launchUI {
            repeat(MEASURE_REPEAT_COUNT) {
                binding.root.doOnLayout {
                    binding.surfaceView.onReady {
                        viewModel.currentSize?.let { videoSizeState ->
                            binding.surfaceView.scaleToFit(videoSizeState)
                        }
                    }
                }
                delay(MEASURE_REPEAT_DELAY)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.surfaceView.onReady { surface ->
            viewModel.initPlayers(requireContext(), surface)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.release()
    }
}
