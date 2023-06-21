package com.amazon.ivs.optimizations.playerview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.util.AttributeSet
import android.util.Size
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.amazon.ivs.optimizations.R
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerControlView

/**
 * View for displaying the content of a [Player] instance. When attached to an activity
 * window creates a [Player] instance with basic controls for live and vod playback.
 * PlayerView manages the player instance and attaches it to a surface, using [SurfaceView]
 * for display.
 */
class PlayerView : FrameLayout, SurfaceHolder.Callback, View.OnAttachStateChangeListener, View.OnLayoutChangeListener {
    lateinit var player: Player
    lateinit var surfaceView: SurfaceView
    private lateinit var captionsView: TextView
    private lateinit var progressView: ProgressBar
    private lateinit var controlsView: PlayerControlView

    private var surface: Surface? = null
    private var controlsEnabled = false
    private var mediaUri: Uri? = null
    private var videoWidth = 0
    private var videoHeight = 0

    /**
     * Creates a new PlayerView with the given context.
     * @param context activity context to use
     */
    constructor(context: Context) : super(context) {
        initialize()
    }

    /**
     * Creates a new PlayerView with the given context.
     * @param context activity context to use
     * @param attrs attributes
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    /**
     * Creates a new PlayerView with the given context.
     * @param context activity context to use
     * @param attrs attributes
     * @param defStyleAttr style attributes
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    private fun initialize() {
        controlsEnabled = false
        surfaceView = SurfaceView(context)
        var params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params.gravity = Gravity.CENTER
        addView(surfaceView, params)
        surfaceView.holder.addCallback(this)

        // captions text
        captionsView = TextView(context)
        captionsView.setBackgroundColor(Color.BLACK)
        captionsView.typeface = Typeface.MONOSPACE
        captionsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        captionsView.textAlignment = TEXT_ALIGNMENT_CENTER
        params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER or Gravity.BOTTOM
        addView(captionsView, params)

        // buffer indicator
        progressView = ProgressBar(context)
        progressView.isIndeterminate = true
        progressView.visibility = INVISIBLE
        params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER
        addView(progressView, params)

        // player instance
        player = Player.Factory.create(context)
        addOnLayoutChangeListener(this)
        addOnAttachStateChangeListener(this)

        // media controls
        controlsView = PlayerControlView(context)
        params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.BOTTOM
        addView(controlsView, params)
        controlsView.setPlayer(player)
        controlsView.durationTextView.setTextColor(context.getColor(R.color.primary_text_color))
        controlsView.positionTextView.setTextColor(context.getColor(R.color.primary_text_color))
    }

    /**
     * Enables or disables the media controls overlayed on the video view
     * @param enabled true to enable automatic display of the controls, false otherwise
     */
    fun setControlsEnabled(enabled: Boolean) {
        controlsEnabled = enabled
        if (controlsEnabled) {
            controlsView.visibility = VISIBLE
        } else {
            controlsView.visibility = INVISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = super.onTouchEvent(event)
        if (!handled && controlsEnabled) {
            controlsView.showControls(true)
        }
        return handled
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            player.play()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
            || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
        ) {
            player.pause()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = holder.surface
        player.setSurface(surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surface = null
        player.setSurface(null)
    }

    override fun onViewAttachedToWindow(v: View) {
        if (mediaUri != null) {
            player.load(mediaUri!!)
        }
        if (surface != null) {
            player.setSurface(surface)
        }
    }

    override fun onViewDetachedFromWindow(v: View) {
        progressView.visibility = INVISIBLE
    }

    override fun onLayoutChange(
        v: View, left: Int, top: Int, right: Int, bottom: Int,
        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
    ) {
        surfaceView.post {
            if (videoWidth > 0 && videoHeight > 0) {
                ViewUtil.setLayoutParams(surfaceView, videoWidth, videoHeight)
            }
        }
    }
}

/**
 * Helper class for resizing views to fit a given video dimensions.
 * @hide
 */
object ViewUtil {
    /**
     * Updates a [SurfaceView]'s layout params to fit the given video dimensions
     * @param surfaceView view to set the transform on
     * @param width video width
     * @param height video height
     */
    fun setLayoutParams(surfaceView: SurfaceView, width: Int, height: Int) {
        val parent = surfaceView.parent as View
        val scaled: Size = scaleTo(parent, width, height)
        val layoutParams = surfaceView.layoutParams
        layoutParams.width = scaled.width
        layoutParams.height = scaled.height
        surfaceView.layoutParams = layoutParams
    }

    private fun scaleTo(view: View, width: Int, height: Int): Size {
        val viewWidth = view.width
        val viewHeight = view.height
        val ratio = height / width.toFloat()
        val scaledWidth: Int
        val scaledHeight: Int
        if (viewHeight > viewWidth * ratio) {
            scaledWidth = viewWidth
            scaledHeight = (viewWidth * ratio).toInt()
        } else {
            scaledWidth = (viewHeight / ratio).toInt()
            scaledHeight = viewHeight
        }
        return Size(scaledWidth, scaledHeight)
    }
}
