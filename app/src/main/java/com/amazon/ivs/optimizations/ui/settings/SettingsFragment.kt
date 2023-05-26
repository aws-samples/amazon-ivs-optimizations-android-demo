package com.amazon.ivs.optimizations.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.amazon.ivs.optimizations.R
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import com.amazon.ivs.optimizations.common.hideKeyboard
import com.amazon.ivs.optimizations.common.setVisible
import com.amazon.ivs.optimizations.common.showKeyboard
import com.amazon.ivs.optimizations.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val IVS_PLAYBACK_URL_BASE = "live-video.net"

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding

    @Inject
    lateinit var preferences: PreferenceProvider

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.customStreamSwitch.isChecked = preferences.useCustomUrl
        binding.playbackUrlContainer.setVisible(preferences.useCustomUrl)
        binding.playbackUrlInput.text?.clear()
        binding.playbackUrlInput.text?.append(preferences.customLiveStreamUrl ?: "")

        binding.customStreamSwitch.setOnCheckedChangeListener { _, checked ->
            binding.playbackUrlContainer.setVisible(checked)
            preferences.useCustomUrl = checked
            if (checked) {
                binding.playbackUrlInput.requestFocus()
                binding.playbackUrlInput.showKeyboard()
            } else {
                binding.playbackUrlInput.hideKeyboard()
            }
        }

        binding.customStreamHolder.setOnClickListener {
            binding.customStreamSwitch.isChecked = !binding.customStreamSwitch.isChecked
        }

        binding.playbackUrlInput.addTextChangedListener {
            val urlInput = binding.playbackUrlInput.text.toString()
            if (urlInput.isNotBlank() && !urlInput.contains(IVS_PLAYBACK_URL_BASE)) {
                binding.playbackUrlContainer.isErrorEnabled = true
                binding.playbackUrlContainer.error = getString(R.string.playback_url)
                preferences.customLiveStreamUrl = null
            } else {
                binding.playbackUrlContainer.isErrorEnabled = false
                preferences.customLiveStreamUrl = urlInput
            }
        }
    }
}
