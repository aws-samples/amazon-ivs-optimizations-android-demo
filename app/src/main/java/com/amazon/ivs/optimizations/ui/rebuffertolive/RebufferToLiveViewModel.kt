package com.amazon.ivs.optimizations.ui.rebuffertolive

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

@HiltViewModel
class RebufferToLiveViewModel @Inject constructor(
    private val preferences: PreferenceProvider,
) : ViewModel() {
    private lateinit var playerListener: Player.Listener
    private var player: MediaPlayer? = null
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
        playerListener = player!!.init(
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
                        _onBuffering.trySend(false)
                        if (timeToVideo == null) {
                            timeToVideo = (Date().time - preferences.capturedClickTime).toInt()
                        }
                    }
                    else -> { /* Ignored */ }
                }
            },
            { exception ->
                _onError.trySend(exception)
            }
        )

        player?.setRebufferToLive(REBUFFER_TO_LIVE)
        player?.setSurface(surface)
        player?.load(Uri.parse(preferences.playbackUrl ?: STREAM_URI))
        player?.play()
        launchUpdates()
    }

    private fun launchUpdates() {
        if (player == null) return
        launchIO {
            player?.let { mediaPlayer ->
                _onInfoUpdate.trySend(
                    InfoUpdate(
                        mediaPlayer.version,
                        (mediaPlayer.bufferedPosition - mediaPlayer.position).toDecimalSeconds(),
                        mediaPlayer.liveLatency.toDecimalSeconds(),
                        rebufferToLive = REBUFFER_TO_LIVE,
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
        player?.removeListener(playerListener)
        player?.release()
        player = null
        timeToVideo = null
    }

    fun captureClickTime() {
        preferences.capturedClickTime = Date().time
    }
}
