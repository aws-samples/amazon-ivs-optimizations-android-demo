package com.amazon.ivs.optimizations.ui.rebuffertolive

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import com.amazon.ivs.optimizations.common.ConsumableLiveData
import com.amazon.ivs.optimizations.common.init
import com.amazon.ivs.optimizations.common.launchIO
import com.amazon.ivs.optimizations.common.toDecimalSeconds
import com.amazon.ivs.optimizations.ui.models.*
import com.amazonaws.ivs.player.MediaPlayer
import com.amazonaws.ivs.player.Player
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.*

class RebufferToLiveViewModel(private val preferences: PreferenceProvider) : ViewModel() {

    private lateinit var playerListener: Player.Listener
    private var player: MediaPlayer? = null
    private var timeToVideo: Int? = null

    val onSizeChanged = ConsumableLiveData<Size>()
    val onBuffering = ConsumableLiveData<Boolean>()
    val onError = ConsumableLiveData<Error>()
    val onInfoUpdate = ConsumableLiveData<InfoUpdate>()

    fun initPlayers(context: Context, surface: Surface, playbackUrl: String?) {
        onBuffering.postConsumable(true)
        player = MediaPlayer(context)
        playerListener = player!!.init(
            { videoSizeState ->
                onSizeChanged.postConsumable(videoSizeState)
            },
            { state ->
                when (state) {
                    Player.State.BUFFERING -> {
                        onBuffering.postConsumable(true)
                    }
                    Player.State.READY -> {
                        player?.qualities?.firstOrNull { it.name == MAX_QUALITY }?.let { quality ->
                            player?.setAutoMaxQuality(quality)
                        }
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
                onInfoUpdate.postConsumable(
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
