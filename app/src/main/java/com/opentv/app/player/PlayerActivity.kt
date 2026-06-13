package com.opentv.app.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.opentv.app.R

/**
 * Native full-screen HLS player backed by Media3 / ExoPlayer.
 *
 * Launch with [EXTRA_NAMES] + [EXTRA_URLS] (parallel arrays) and [EXTRA_INDEX]
 * to enable CHANNEL up/down switching across the whole channel list.
 */
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAMES = "extra_names"
        const val EXTRA_URLS = "extra_urls"
        const val EXTRA_INDEX = "extra_index"
    }

    private lateinit var playerView: PlayerView
    private lateinit var channelLabel: TextView
    private var player: ExoPlayer? = null

    private lateinit var names: Array<String>
    private lateinit var urls: Array<String>
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player)

        // Keep the screen awake during playback.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.playerView)
        channelLabel = findViewById(R.id.channelLabel)

        names = intent.getStringArrayExtra(EXTRA_NAMES) ?: arrayOf("Stream")
        urls = intent.getStringArrayExtra(EXTRA_URLS) ?: emptyArray()
        index = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, (urls.size - 1).coerceAtLeast(0))

        if (urls.isEmpty()) {
            Toast.makeText(this, R.string.player_no_stream, Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    // ExoPlayer is created/released around the visible lifecycle so leaving the
    // screen stops the network/decoder work immediately.
    override fun onStart() {
        super.onStart()
        initPlayer()
        playCurrent()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initPlayer() {
        if (player != null) return
        val exo = ExoPlayer.Builder(this).build()
        playerView.player = exo
        exo.playWhenReady = true
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@PlayerActivity,
                    getString(R.string.player_error, error.errorCodeName),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        player = exo
    }

    private fun playCurrent() {
        val exo = player ?: return
        // ExoPlayer auto-detects HLS via the media3-exoplayer-hls module.
        exo.setMediaItem(MediaItem.fromUri(urls[index]))
        exo.prepare()
        showChannelLabel(names.getOrElse(index) { "Stream" })
    }

    private fun switchChannel(delta: Int) {
        if (urls.size <= 1) return
        index = (index + delta + urls.size) % urls.size
        playCurrent()
    }

    /** Briefly flashes the current channel name, TV-style. */
    private fun showChannelLabel(name: String) {
        channelLabel.text = name
        channelLabel.visibility = View.VISIBLE
        channelLabel.removeCallbacks(hideLabel)
        channelLabel.postDelayed(hideLabel, 2500)
    }

    private val hideLabel = Runnable { channelLabel.visibility = View.GONE }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_MEDIA_NEXT -> { switchChannel(+1); return true }

            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { switchChannel(-1); return true }

            // Many TV remotes have no CHANNEL keys, so when the transport
            // controls aren't showing, D-pad up/down change channel instead.
            KeyEvent.KEYCODE_DPAD_UP -> if (!playerView.isControllerFullyVisible) {
                switchChannel(+1); return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (!playerView.isControllerFullyVisible) {
                switchChannel(-1); return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        playerView.player = null
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    private fun hideSystemUi() {
        WindowInsetsControllerCompat(window, playerView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
