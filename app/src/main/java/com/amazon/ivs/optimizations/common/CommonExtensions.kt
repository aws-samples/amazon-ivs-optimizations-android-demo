package com.amazon.ivs.optimizations.common

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.amazon.ivs.optimizations.R
import com.amazon.ivs.optimizations.ui.MainActivity
import com.amazon.ivs.optimizations.ui.models.DECIMAL_POINT_COUNT
import java.math.BigDecimal
import java.math.RoundingMode

fun Long.toDecimalSeconds(decimalPointCount: Int = DECIMAL_POINT_COUNT): BigDecimal = if (this > 0) {
    (this / 1000.0)
} else {
    0.0
}.toBigDecimal().setScale(decimalPointCount, RoundingMode.UP)

fun Float.toDecimalSeconds(decimalPointCount: Int = DECIMAL_POINT_COUNT): BigDecimal = toBigDecimal().setScale(decimalPointCount, RoundingMode.UP)

fun MainActivity.openFragment(id: Int) {
    findNavController(R.id.nav_host_fragment).run {
        navigate(id)
    }
}

fun Fragment.openFragment(id: Int) {
    (this.activity as? MainActivity)?.openFragment(id)
}

fun AppCompatActivity.getCurrentFragment() =
    supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.firstOrNull()
