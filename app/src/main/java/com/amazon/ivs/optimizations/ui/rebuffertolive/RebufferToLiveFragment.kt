package com.amazon.ivs.optimizations.ui.rebuffertolive

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.amazon.ivs.optimizations.App
import com.amazon.ivs.optimizations.cache.PREFERENCES_NAME
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import com.amazon.ivs.optimizations.common.*
import com.amazon.ivs.optimizations.databinding.FragmentRebufferToLiveBinding
import com.amazon.ivs.optimizations.ui.models.MEASURE_REPEAT_COUNT
import com.amazon.ivs.optimizations.ui.models.MEASURE_REPEAT_DELAY
import kotlinx.coroutines.delay
import java.util.*

class RebufferToLiveFragment : Fragment() {

    private lateinit var binding: FragmentRebufferToLiveBinding
    private val viewModel by lazyViewModel(
        { requireActivity().application as App },
        { RebufferToLiveViewModel(preferences) }
    )
    private val preferences by lazy {
        PreferenceProvider(requireContext(), PREFERENCES_NAME)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRebufferToLiveBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBackButtonAvailable()
        preferences.capturedClickTime = Date().time

        launchUI {
            viewModel.onInfoUpdate.collect { infoUpdate ->
                binding.infoUpdate = infoUpdate
            }
        }

        launchUI {
            viewModel.onBuffering.collect { bufferingState ->
                binding.surfaceBuffering = bufferingState
            }
        }

        launchUI {
            viewModel.onError.collect { error ->
                binding.root.showSnackBar(error.errorMessage)
            }
        }

        launchUI {
            viewModel.onSizeChanged.collect { videoSizeState ->
                binding.surfaceView.onReady {
                    binding.surfaceView.scaleToFit(videoSizeState)
                }
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

        launchMain {
            repeat(MEASURE_REPEAT_COUNT) {
                binding.surfaceView.doOnLayout {
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
            viewModel.initPlayers(requireContext(), surface, preferences.playbackUrl)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.release()
    }
}
