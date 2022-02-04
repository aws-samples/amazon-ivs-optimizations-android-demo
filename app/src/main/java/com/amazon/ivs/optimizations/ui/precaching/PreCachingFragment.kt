package com.amazon.ivs.optimizations.ui.precaching

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.amazon.ivs.optimizations.App
import com.amazon.ivs.optimizations.cache.PREFERENCES_NAME
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import com.amazon.ivs.optimizations.common.*
import com.amazon.ivs.optimizations.databinding.FragmentPreCachingBinding
import com.amazon.ivs.optimizations.ui.models.MEASURE_REPEAT_COUNT
import com.amazon.ivs.optimizations.ui.models.MEASURE_REPEAT_DELAY
import kotlinx.coroutines.delay
import java.util.*

class PreCachingFragment : Fragment() {

    private lateinit var binding: FragmentPreCachingBinding
    private val viewModel by lazyViewModel(
        { requireActivity().application as App },
        { PreCachingViewModel(preferences) }
    )
    private val preferences by lazy {
        PreferenceProvider(requireContext(), PREFERENCES_NAME)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPreCachingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBackButtonAvailable()
        preferences.capturedClickTime = Date().time

        binding.streamContainer.addView(viewModel.playerView, 0)
        val params = viewModel.playerView?.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToBottom = binding.playerGuideline.id
        params.endToEnd = binding.streamContainer.id
        params.startToStart = binding.streamContainer.id
        params.topToTop = binding.playerGuideline.id
        viewModel.playerView?.layoutParams = params

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
                viewModel.playerView?.surfaceView?.scaleToFit(videoSizeState, binding.streamContainer)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            binding.playerGuideline.setGuidelinePercent(0.5f)
        } else {
            binding.playerGuideline.setGuidelinePercent(0.3f)
        }

        launchMain {
            repeat(MEASURE_REPEAT_COUNT) {
                binding.streamContainer.doOnLayout {
                    viewModel.currentSize?.let { videoSizeState ->
                        viewModel.playerView?.surfaceView?.scaleToFit(videoSizeState, binding.streamContainer)
                    }
                }
                delay(MEASURE_REPEAT_DELAY)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.release()
        binding.streamContainer.removeAllViews()
    }
}
