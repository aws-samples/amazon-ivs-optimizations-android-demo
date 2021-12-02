package com.amazon.ivs.optimizations.ui.precaching

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.lifecycle.ViewModel
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import com.amazon.ivs.optimizations.common.*
import com.amazon.ivs.optimizations.playerview.PlayerView
import com.amazon.ivs.optimizations.ui.models.*
import com.amazonaws.ivs.player.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.*

class PreCachingViewModel(private val preferences: PreferenceProvider) : ViewModel() {

    private lateinit var playerListener: Player.Listener
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

    @SuppressLint("StaticFieldLeak")
    var playerView: PlayerView? = null

    fun initPlayer(context: Context, playbackUrl: String?) {
        if (playerView != null) {
            playerView?.player?.load(Uri.parse(playbackUrl ?: STREAM_URI))
            return
        }
        _onBuffering.tryEmit(true)

        playerView = PlayerView(context).apply {
            Timber.d("Initializing player and calling load()")
            player.load(Uri.parse(playbackUrl ?: STREAM_URI))
            setControlsEnabled(false)
            playerListener = player.init(
                { videoSizeState ->
                    _onSizeChanged.tryEmit(videoSizeState)
                },
                { state ->
                    when (state) {
                        Player.State.BUFFERING -> {
                            _onBuffering.tryEmit(true)
                        }
                        Player.State.READY -> {
                            player.qualities.firstOrNull { it.name == MAX_QUALITY }?.let { quality ->
                                player.setAutoMaxQuality(quality)
                            }
                            _onBuffering.tryEmit(false)
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
        }
        launchUpdates()
    }

    private fun launchUpdates() {
        if (playerView == null) return
        launchIO {
            playerView?.let { playerView ->
                _onInfoUpdate.tryEmit(
                    InfoUpdate(
                        playerView.player.version,
                        (playerView.player.bufferedPosition - playerView.player.position).toDecimalSeconds(),
                        playerView.player.liveLatency.toDecimalSeconds(),
                        timeToVideo = timeToVideo ?: 0,
                        preCached = true
                    )
                )
            }
            delay(UPDATE_DELAY)
            launchUpdates()
        }
    }

    fun play() {
        playerView?.player?.play()
    }

    fun release() {
        Timber.d("Releasing player instance")
        playerView?.player?.removeListener(playerListener)
        playerView?.player?.release()
        playerView = null
        timeToVideo = null
    }
}
