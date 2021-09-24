package com.amazon.ivs.optimizations.common

import android.util.Size
import com.amazon.ivs.optimizations.ui.models.Error
import com.amazonaws.ivs.player.*
import java.nio.ByteBuffer

private const val MAX_PLAYER_SPEED_INCREASE = 0.05f

inline fun Player.setListener(
    crossinline onAnalyticsEvent: (key: String, value: String) -> Unit = { _, _ -> },
    crossinline onRebuffering: () -> Unit = {},
    crossinline onSeekCompleted: (value: Long) -> Unit = { _ -> },
    crossinline onQualityChanged: (quality: Quality) -> Unit = { _ -> },
    crossinline onVideoSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    crossinline onCue: (cue: Cue) -> Unit = { _ -> },
    crossinline onDurationChanged: (duration: Long) -> Unit = { _ -> },
    crossinline onStateChanged: (state: Player.State) -> Unit = { _ -> },
    crossinline onError: (exception: PlayerException) -> Unit = { _ -> },
    crossinline onMetadata: (data: String, buffer: ByteBuffer) -> Unit = { _, _ -> }
): Player.Listener {
    val listener = playerListener(
        onAnalyticsEvent, onRebuffering, onSeekCompleted, onQualityChanged, onVideoSizeChanged,
        onCue, onDurationChanged, onStateChanged, onError, onMetadata
    )

    addListener(listener)
    return listener
}

fun Player.init(
    onVideoSizeChanged: (playerParamsChanged: Size) -> Unit,
    onStateChanged: (state: Player.State) -> Unit,
    onError: (exception: Error) -> Unit
) = setListener(
    onVideoSizeChanged = { width, height ->
        onVideoSizeChanged(Size(width, height))
    },
    onStateChanged = { state ->
        onStateChanged(state)
    },
    onError = { exception ->
        if (exception.code != 0) {
            onError(Error(exception.code.toString(), exception.errorMessage))
        }
    }
)

inline fun playerListener(
    crossinline onAnalyticsEvent: (key: String, value: String) -> Unit = { _, _ -> },
    crossinline onRebuffering: () -> Unit = {},
    crossinline onSeekCompleted: (value: Long) -> Unit = { _ -> },
    crossinline onQualityChanged: (quality: Quality) -> Unit = { _ -> },
    crossinline onVideoSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    crossinline onCue: (cue: Cue) -> Unit = { _ -> },
    crossinline onDurationChanged: (duration: Long) -> Unit = { _ -> },
    crossinline onStateChanged: (state: Player.State) -> Unit = { _ -> },
    crossinline onError: (exception: PlayerException) -> Unit = { _ -> },
    crossinline onMetadata: (data: String, buffer: ByteBuffer) -> Unit = { _, _ -> }
): Player.Listener = object : Player.Listener() {
    override fun onAnalyticsEvent(key: String, value: String) = onAnalyticsEvent(key, value)
    override fun onRebuffering() = onRebuffering()
    override fun onSeekCompleted(value: Long) = onSeekCompleted(value)
    override fun onQualityChanged(quality: Quality) = onQualityChanged(quality)
    override fun onVideoSizeChanged(width: Int, height: Int) = onVideoSizeChanged(width, height)
    override fun onCue(cue: Cue) = onCue(cue)
    override fun onDurationChanged(duration: Long) = onDurationChanged(duration)
    override fun onStateChanged(state: Player.State) = onStateChanged(state)
    override fun onError(exception: PlayerException) = onError(exception)
    override fun onMetadata(data: String, buffer: ByteBuffer) = onMetadata(data, buffer)
}

fun MediaPlayer.shouldAdjustRate(bufferSize: Long): Boolean {
    return when {
        bufferSize > 5000 -> {
            playbackRate = 1 + MAX_PLAYER_SPEED_INCREASE
            true
        }
        bufferSize in 2000..4999 -> {
            val percentage = (bufferSize - 2000) / 2999f * 100
            val calculatedRate = percentage * MAX_PLAYER_SPEED_INCREASE / 100 + 1
            playbackRate = calculatedRate
            true
        }
        else -> {
            playbackRate = 1f
            false
        }
    }
}
