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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.*

class RebufferToLiveViewModel(private val preferences: PreferenceProvider) : ViewModel() {

    private lateinit var playerListener: Player.Listener
    private var player: MediaPlayer? = null
    private var timeToVideo: Int? = null
    private val _onSizeChanged = ConsumableSharedFlow<Size>(canReplay = true)
    private val _onInfoUpdate = ConsumableSharedFlow<InfoUpdate>(canReplay = true)
    private val _onBuffering = ConsumableSharedFlow<Boolean>()
    private val _onError = ConsumableSharedFlow<Error>()

    val onSizeChanged = _onSizeChanged.asSharedFlow()
    val onInfoUpdate = _onInfoUpdate.asSharedFlow()
    val onBuffering = _onBuffering.asSharedFlow()
    val onError = _onError.asSharedFlow()
    val currentSize get() = _onSizeChanged.replayCache.lastOrNull()

    fun initPlayers(context: Context, surface: Surface, playbackUrl: String?) {
        _onBuffering.tryEmit(true)
        player = MediaPlayer(context)
        playerListener = player!!.init(
            { videoSizeState ->
                _onSizeChanged.tryEmit(videoSizeState)
            },
            { state ->
                when (state) {
                    Player.State.BUFFERING -> {
                        _onBuffering.tryEmit(true)
                    }
                    Player.State.READY -> {
                        player?.qualities?.firstOrNull { it.name == MAX_QUALITY }?.let { quality ->
                            player?.setAutoMaxQuality(quality)
                        }
                    }
                    Player.State.PLAYING -> {
                        _onBuffering.tryEmit(false)
                        if (timeToVideo == null) {
                            timeToVideo = (Date().time - preferences.capturedClickTime).toInt()
                        }
                    }
                    else -> { /* Ignored */ }
                }
            },
            { exception ->
                _onError.tryEmit(exception)
            }
        )

        player?.setRebufferToLive(REBUFFER_TO_LIVE)
        player?.setSurface(surface)
        player?.load(Uri.parse(playbackUrl ?: STREAM_URI))
        player?.play()
        launchUpdates()
    }

    private fun launchUpdates() {
        if (player == null) return
        launchIO {
            player?.let { mediaPlayer ->
                _onInfoUpdate.tryEmit(
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
}
