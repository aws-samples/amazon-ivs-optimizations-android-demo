package com.amazon.ivs.optimizations.ui.models

import com.amazon.ivs.optimizations.R
import java.math.BigDecimal

data class InfoUpdate(
    val playerVersion: String,
    val bufferSize: BigDecimal,
    val latency: BigDecimal,
    val playerSpeed: BigDecimal? = null,
    val rebufferToLive: Boolean = false,
    val timeToVideo: Int = 0,
    val preCached: Boolean = false
) {
    val pillBackground get() = when {
        latency.toDouble() <= 5 -> {
            R.drawable.bg_pill_dark_green
        }
        latency.toDouble() > 5 && latency.toDouble() <= 10 -> {
            R.drawable.bg_pill_light_green
        }
        latency.toDouble() > 10 && latency.toDouble() < 15 -> {
            R.drawable.bg_pill_orange
        }
        else -> {
            R.drawable.bg_pill_red
        }
    }
}
