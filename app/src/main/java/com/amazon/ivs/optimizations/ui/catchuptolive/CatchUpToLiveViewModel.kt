package com.amazon.ivs.optimizations.ui.catchuptolive

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import com.amazon.ivs.optimizations.common.*
import com.amazon.ivs.optimizations.ui.models.*
import com.amazonaws.ivs.player.MediaPlayer
import com.amazonaws.ivs.player.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import java.util.*
import javax.inject.Inject

const val MAX_BUFFER_SIZE = 5000

@HiltViewModel
class CatchUpToLiveViewModel @Inject constructor(
    private val preferences: PreferenceProvider,
) : ViewModel() {
    private lateinit var listener: Player.Listener
    private var player: MediaPlayer? = null
    private var adjustPlayBackRate = false
    private var timeToVideo: Int? = null
    private val _onSizeChanged = MutableSharedFlow<Size>(replay = 1)
    private val _onInfoUpdate = Channel<InfoUpdate>()
    private val _onBuffering = Channel<Boolean>()
    private val _onError = Channel<Error>()

    val onSizeChanged = _onSizeChanged.asSharedFlow()
    val onInfoUpdate = _onInfoUpdate.receiveAsFlow()
    val onBuffering = _onBuffering.receiveAsFlow()
    val onError = _onError.receiveAsFlow()
    val currentSize get() = _onSizeChanged.replayCache.lastOrNull()

    fun initPlayers(context: Context, surface: Surface) {
        _onBuffering.trySend(true)
        player = MediaPlayer(context)
        listener = player!!.init(
            { videoSizeState ->
                _onSizeChanged.tryEmit(videoSizeState)
            },
            { state ->
                when (state) {
                    Player.State.BUFFERING -> {
                        _onBuffering.trySend(true)
                    }
                    Player.State.READY -> {
                        player?.qualities?.firstOrNull { it.name == MAX_QUALITY }?.let { quality ->
                            player?.setAutoMaxQuality(quality)
                        }
                    }
                    Player.State.PLAYING -> {
                        if (timeToVideo == null) {
                            timeToVideo = (Date().time - preferences.capturedClickTime).toInt()
                        }
                        _onBuffering.trySend(false)
                    }
                    else -> { /* Ignored */ }
                }
            },
            { exception ->
                _onError.trySend(exception)
            }
        )

        player?.setSurface(surface)
        player?.load(Uri.parse(preferences.playbackUrl ?: STREAM_URI))
        player?.play()
        launchUpdates()
    }

    private fun launchUpdates() {
        if (player == null) return

        launchIO {
            player?.let { mediaPlayer ->
                val bufferSize = mediaPlayer.bufferedPosition - mediaPlayer.position

                if (bufferSize > MAX_BUFFER_SIZE && !adjustPlayBackRate) {
                    adjustPlayBackRate = true
                }

                if (adjustPlayBackRate) {
                    adjustPlayBackRate = mediaPlayer.shouldAdjustRate(bufferSize)
                }

                _onInfoUpdate.trySend(
                    InfoUpdate(
                        mediaPlayer.version,
                        bufferSize.toDecimalSeconds(),
                        mediaPlayer.liveLatency.toDecimalSeconds(),
                        mediaPlayer.playbackRate.toDecimalSeconds(4),
                        timeToVideo = timeToVideo ?: 0
                    )
                )
            }

            delay(UPDATE_DELAY)
            launchUpdates()
        }
    }

    fun release() {
        Timber.d("Releasing player")
        player?.removeListener(listener)
        player?.release()
        player = null
        timeToVideo = null
    }

    fun captureClickTime() {
        preferences.capturedClickTime = Date().time
    }
}
