package com.opentv.app.player

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.opentv.app.AppConfig
import com.opentv.app.R

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
    private lateinit var filterButton: Button
    private val adapter = ChannelAdapter(::openChannel)

    /** Full parsed list from the current source. */
    private var allChannels: List<Channel> = emptyList()
    /** Currently displayed list (after optional filtering). */
    private var channels: List<Channel> = emptyList()
    private var sourceIndex = 0
    /** When true, only sports / 2026-broadcaster channels are shown. */
    private var filterRelevant = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_channels)

        recyclerView = findViewById(R.id.channelList)
        progress = findViewById(R.id.channelsProgress)
        statusText = findViewById(R.id.channelsStatus)
        sourceButton = findViewById(R.id.sourceButton)
        filterButton = findViewById(R.id.filterButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        sourceButton.setOnClickListener { showSourcePicker() }
        filterButton.setOnClickListener {
            filterRelevant = !filterRelevant
            updateFilterLabel()
            applyFilter()
        }

        repository = PlaylistRepository(this)
        updateSourceLabel()
        updateFilterLabel()
        loadPlaylist()
    }

    /** Applies the World Cup keyword filter to [allChannels] and updates the UI. */
    private fun applyFilter() {
        channels = if (filterRelevant) {
            allChannels.filter { ch ->
                val haystack = (ch.name + " " + (ch.group ?: "")).lowercase()
                AppConfig.RELEVANT_KEYWORDS.any { haystack.contains(it) }
            }
        } else {
            allChannels
        }
        adapter.submit(channels)

        if (channels.isEmpty()) {
            recyclerView.visibility = View.GONE
            statusText.visibility = View.VISIBLE
            statusText.text = getString(R.string.channels_no_matches)
        } else {
            statusText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.post { recyclerView.requestFocus() }
        }
    }

    private fun updateFilterLabel() {
        val state = getString(
            if (filterRelevant) R.string.channels_filter_worldcup
            else R.string.channels_filter_all
        )
        filterButton.text = getString(R.string.channels_filter_label, state)
    }

    /** Lets the user pick which configured playlist to load. */
    private fun showSourcePicker() {
        val names = AppConfig.PLAYLIST_SOURCES.map { it.name }.toTypedArray()
        AlertDialog.Builder(this, R.style.ThemeOverlay_OpenTV_Dialog)
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
                allChannels = result
                setLoading(false)
                applyFilter()
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
