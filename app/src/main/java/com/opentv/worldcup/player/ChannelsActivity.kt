package com.opentv.worldcup.player

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ProgressBar
import android.widget.TextView
import com.opentv.worldcup.R

/**
 * Shows the parsed M3U channels in a D-pad-navigable list. Selecting a channel
 * opens [PlayerActivity] with the full list so the player can switch channels.
 */
class ChannelsActivity : AppCompatActivity() {

    private lateinit var repository: PlaylistRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var statusText: TextView
    private val adapter = ChannelAdapter(::openChannel)

    private var channels: List<Channel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_channels)

        recyclerView = findViewById(R.id.channelList)
        progress = findViewById(R.id.channelsProgress)
        statusText = findViewById(R.id.channelsStatus)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        repository = PlaylistRepository(this)
        loadPlaylist()
    }

    private fun loadPlaylist() {
        setLoading(true)
        repository.load(
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
