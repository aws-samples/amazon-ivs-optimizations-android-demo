package com.amazon.ivs.optimizations.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.amazon.ivs.optimizations.R
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import com.amazon.ivs.optimizations.common.getCurrentFragment
import com.amazon.ivs.optimizations.common.openFragment
import com.amazon.ivs.optimizations.common.setVisible
import com.amazon.ivs.optimizations.databinding.ActivityMainBinding
import com.amazon.ivs.optimizations.ui.home.HomeFragment
import com.amazon.ivs.optimizations.ui.settings.IVS_PLAYBACK_URL_BASE
import com.amazon.ivs.optimizations.ui.settings.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var preferences: PreferenceProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        validateUrl()

        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_pre_caching,
                R.id.navigation_rebuff_to_live,
                R.id.navigation_catch_up_to_live
            )
        )

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home -> binding.toolbarSettings.setVisible()
                else -> binding.toolbarSettings.setVisible(false)
            }
        }

        binding.toolbarSettings.setOnClickListener {
            openFragment(R.id.navigation_settings)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                getCurrentFragment()?.let { currentFragment ->
                    when (currentFragment) {
                        is HomeFragment -> finish()
                        is SettingsFragment -> validateUrl()
                    }
                    if (currentFragment is HomeFragment) finish()
                    findNavController(R.id.nav_host_fragment).popBackStack(R.id.navigation_home, false)
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun validateUrl() {
        if (preferences.customLiveStreamUrl == null || preferences.customLiveStreamUrl?.contains(IVS_PLAYBACK_URL_BASE) == false){
            preferences.useCustomUrl = false
        }
    }
}
