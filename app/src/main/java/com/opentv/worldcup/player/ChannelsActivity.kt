package com.opentv.worldcup.player

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.opentv.worldcup.AppConfig
import com.opentv.worldcup.R

/**
 * Shows the parsed M3U channels in a D-pad-navigable list. A "Source" button
 * lets the user switch between the configured playlists (iptv-org Sports /
 * host-country lists, or the bundled offline playlist). Selecting a channel
 * opens [PlayerActivity] with the full list so the player can switch channels.
 */
class ChannelsActivity : AppCompatActivity() {

    private lateinit var repository: PlaylistRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var sourceButton: Button
    private val adapter = ChannelAdapter(::openChannel)

    private var channels: List<Channel> = emptyList()
    private var sourceIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_channels)

        recyclerView = findViewById(R.id.channelList)
        progress = findViewById(R.id.channelsProgress)
        statusText = findViewById(R.id.channelsStatus)
        sourceButton = findViewById(R.id.sourceButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        sourceButton.setOnClickListener { showSourcePicker() }

        repository = PlaylistRepository(this)
        updateSourceLabel()
        loadPlaylist()
    }

    /** Lets the user pick which configured playlist to load. */
    private fun showSourcePicker() {
        val names = AppConfig.PLAYLIST_SOURCES.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_OpenTV_Dialog)
            .setTitle(R.string.channels_pick_source)
            .setSingleChoiceItems(names, sourceIndex) { dialog, which ->
                dialog.dismiss()
                if (which != sourceIndex) {
                    sourceIndex = which
                    updateSourceLabel()
                    loadPlaylist()
                }
            }
            .show()
    }

    private fun updateSourceLabel() {
        val source = AppConfig.PLAYLIST_SOURCES.getOrNull(sourceIndex)
        sourceButton.text = getString(R.string.channels_source_label, source?.name ?: "—")
    }

    private fun loadPlaylist() {
        val source = AppConfig.PLAYLIST_SOURCES.getOrNull(sourceIndex)
        setLoading(true)
        recyclerView.visibility = View.GONE
        statusText.visibility = View.GONE
        repository.load(
            url = source?.url,
            onSuccess = { result ->
                channels = result
                adapter.submit(result)
                setLoading(false)
                recyclerView.visibility = View.VISIBLE
                statusText.visibility = View.GONE
                recyclerView.post { recyclerView.requestFocus() }
            },
            onError = { message ->
                setLoading(false)
                recyclerView.visibility = View.GONE
                statusText.visibility = View.VISIBLE
                statusText.text = message
                sourceButton.requestFocus()
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun openChannel(position: Int) {
        if (position !in channels.indices) return
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_NAMES, channels.map { it.name }.toTypedArray())
            putExtra(PlayerActivity.EXTRA_URLS, channels.map { it.url }.toTypedArray())
            putExtra(PlayerActivity.EXTRA_INDEX, position)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        repository.shutdown()
        super.onDestroy()
    }
}
