package com.amazon.ivs.optimizations.common

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.amazon.ivs.optimizations.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.*
import kotlin.math.roundToInt

fun TextInputEditText.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun TextInputEditText.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.setVisible(isVisible: Boolean = true, hideOption: Int = View.GONE) {
    this.visibility = if (isVisible) View.VISIBLE else hideOption
}

fun View.scaleToFit(videoSize: Size, parentView: View? = null) {
    (parentView ?: parent as View).doOnLayout { useToScale ->
        calculateSurfaceSize(videoSize.width, videoSize.height)
        val size = useToScale.calculateSurfaceSize(videoSize.width, videoSize.height)
        val params = layoutParams
        params.width = size.width
        params.height = size.height
        layoutParams = params
    }
}

private fun View.calculateSurfaceSize(videoWidth: Int, videoHeight: Int): Size {
    val ratio = videoHeight / videoWidth.toFloat()
    val scaledWidth: Int
    val scaledHeight: Int
    if (measuredHeight > measuredWidth * ratio) {
        scaledWidth = measuredWidth
        scaledHeight = (measuredWidth * ratio).roundToInt()
    } else {
        scaledWidth = (measuredHeight / ratio).roundToInt()
        scaledHeight = measuredHeight
    }
    Timber.d("CALCULATED: ($scaledWidth, $scaledHeight) FOR PARENT: ($measuredWidth, $measuredHeight), VIDEO: ($videoWidth, $videoHeight)")
    return Size(scaledWidth, scaledHeight)
}

fun View.showSnackBar(message: String) {
    val snackBar = Snackbar.make(this, message, Snackbar.LENGTH_INDEFINITE)
    snackBar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_bg_color))
    snackBar.setActionTextColor(ContextCompat.getColor(context, R.color.snackbar_action_color))
    snackBar.setTextColor(ContextCompat.getColor(context, R.color.snackbar_action_color))
    snackBar.duration = Snackbar.LENGTH_LONG
    snackBar.setAction(context.getString(R.string.close)) {
        snackBar.dismiss()
    }
    snackBar.show()
}

fun TextureView.onReady(onReady: (surface: Surface) -> Unit) = launchMain {
    waitForSurface().collectLatest { playerView ->
        onReady(playerView)
    }
}

private fun TextureView.waitForSurface() = channelFlow {
    if (surfaceTexture != null) {
        offer(Surface(surfaceTexture))
        return@channelFlow
    }
    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            surfaceTextureListener = null
            offer(Surface(surfaceTexture))
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            /* Ignored */
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            /* Ignored */
        }
    }
    awaitClose()
}

fun Fragment.setBackButtonAvailable() {
    (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
}
