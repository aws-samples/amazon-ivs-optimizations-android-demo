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
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.*

const val MAX_BUFFER_SIZE = 5000

class CatchUpToLiveViewModel(private val preferences: PreferenceProvider) : ViewModel() {

    private lateinit var listener: Player.Listener
    private var player: MediaPlayer? = null
    private var adjustPlayBackRate = false
    private var timeToVideo: Int? = null

    val onSizeChanged = ConsumableLiveData<Size>()
    val onBuffering = ConsumableLiveData<Boolean>()
    val onError = ConsumableLiveData<Error>()
    val onInfoUpdate = ConsumableLiveData<InfoUpdate>()

    fun initPlayers(context: Context, surface: Surface, playbackUrl: String?) {
        onBuffering.postConsumable(true)
        player = MediaPlayer(context)
        listener = player!!.init(
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
                        if (timeToVideo == null) {
                            timeToVideo = (Date().time - preferences.capturedClickTime).toInt()
                        }
                        onBuffering.postConsumable(false)
                    }
                    else -> { /* Ignored */ }
                }
            },
            { exception ->
                onError.postConsumable(exception)
            }
        )

        player?.setSurface(surface)
        player?.load(Uri.parse(playbackUrl ?: STREAM_URI))
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

                onInfoUpdate.postConsumable(
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
}
