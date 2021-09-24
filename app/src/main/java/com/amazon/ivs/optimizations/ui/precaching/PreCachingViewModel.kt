package com.amazon.ivs.optimizations.ui.precaching

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.lifecycle.ViewModel
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import com.amazon.ivs.optimizations.common.ConsumableLiveData
import com.amazon.ivs.optimizations.common.init
import com.amazon.ivs.optimizations.common.launchIO
import com.amazon.ivs.optimizations.common.toDecimalSeconds
import com.amazon.ivs.optimizations.playerview.PlayerView
import com.amazon.ivs.optimizations.ui.models.*
import com.amazonaws.ivs.player.Player
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.*

class PreCachingViewModel(private val preferences: PreferenceProvider) : ViewModel() {

    private lateinit var playerListener: Player.Listener
    private var timeToVideo: Int? = null

    @SuppressLint("StaticFieldLeak")
    var playerView: PlayerView? = null
    val onSizeChanged = ConsumableLiveData<Size>()
    val onBuffering = ConsumableLiveData<Boolean>()
    val onError = ConsumableLiveData<Error>()
    val onInfoUpdate = ConsumableLiveData<InfoUpdate>()

    fun initPlayer(context: Context, playbackUrl: String?) {
        if (playerView != null) {
            playerView?.player?.load(Uri.parse(playbackUrl ?: STREAM_URI))
            return
        }
        onBuffering.postConsumable(true)

        playerView = PlayerView(context).apply {
            Timber.d("Initializing player and calling load()")
            player.load(Uri.parse(playbackUrl ?: STREAM_URI))
            setControlsEnabled(false)
            playerListener = player.init(
                { videoSizeState ->
                    onSizeChanged.postConsumable(videoSizeState)
                },
                { state ->
                    when (state) {
                        Player.State.BUFFERING -> {
                            onBuffering.postConsumable(true)
                        }
                        Player.State.READY -> {
                            player.qualities.firstOrNull { it.name == MAX_QUALITY }?.let { quality ->
                                player.setAutoMaxQuality(quality)
                            }
                            onBuffering.postConsumable(false)
                        }
                        Player.State.PLAYING -> {
                            onBuffering.postConsumable(false)
                            if (timeToVideo == null) {
                                timeToVideo = (Date().time - preferences.capturedClickTime).toInt()
                            }
                        }
                        else -> { /* Ignored */ }
                    }
                },
                { exception ->
                    onError.postConsumable(exception)
                }
            )
        }
        launchUpdates()
    }

    private fun launchUpdates() {
        if (playerView == null) return
        launchIO {
            playerView?.let { playerView ->
                onInfoUpdate.postConsumable(
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
