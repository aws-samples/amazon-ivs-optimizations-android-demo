package com.amazon.ivs.optimizations.ui.catchuptolive

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
import com.amazon.ivs.optimizations.databinding.FragmentCatchUpToLiveBinding
import com.amazon.ivs.optimizations.ui.models.MEASURE_REPEAT_COUNT
import com.amazon.ivs.optimizations.ui.models.MEASURE_REPEAT_DELAY
import kotlinx.coroutines.delay
import java.util.*

class CatchUpToLiveFragment : Fragment() {

    private lateinit var binding: FragmentCatchUpToLiveBinding
    private val viewModel by lazyViewModel(
        { requireActivity().application as App },
        { CatchUpToLiveViewModel(preferences) }
    )
    private val preferences by lazy {
        PreferenceProvider(requireContext(), PREFERENCES_NAME)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCatchUpToLiveBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBackButtonAvailable()
        preferences.capturedClickTime = Date().time

        viewModel.onInfoUpdate.observeConsumable(this) { infoUpdate ->
            binding.infoUpdate = infoUpdate
        }

        viewModel.onBuffering.observeConsumable(viewLifecycleOwner) { bufferingState ->
            binding.surfaceBuffering = bufferingState
        }

        viewModel.onError.observeConsumable(viewLifecycleOwner) { error ->
            binding.root.showSnackBar(error.errorMessage)
        }

        viewModel.onSizeChanged.observeConsumable(viewLifecycleOwner) { videoSizeState ->
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

        launchMain {
            repeat(MEASURE_REPEAT_COUNT) {
                binding.root.doOnLayout {
                    binding.surfaceView.onReady {
                        viewModel.onSizeChanged.consumedValue?.let { videoSizeState ->
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

    override fun onStart() {
        super.onStart()
        viewModel.onError.consume()
    }
}
