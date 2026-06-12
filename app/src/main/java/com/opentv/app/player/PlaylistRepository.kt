package com.opentv.app.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.opentv.app.AppConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Loads and parses the M3U playlist on a background thread, then delivers the
 * result on the main thread. Source priority:
 *   1. [AppConfig.PLAYLIST_URL] if non-blank (remote playlist), else
 *   2. the bundled asset [AppConfig.PLAYLIST_ASSET].
 */
class PlaylistRepository(context: Context) {

    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * @param url       remote M3U URL to load; blank/null loads the bundled
     *                  asset [AppConfig.PLAYLIST_ASSET].
     * @param onSuccess called on the main thread with the parsed channels.
     * @param onError   called on the main thread with a human-readable message.
     */
    fun load(
        url: String?,
        onSuccess: (List<Channel>) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val content = if (!url.isNullOrBlank()) {
                    fetchRemote(url)
                } else {
                    fetchAsset(AppConfig.PLAYLIST_ASSET)
                }
                val channels = M3uParser.parse(content)
                mainHandler.post {
                    if (channels.isEmpty()) {
                        onError("Playlist is empty. Add sources you're licensed to use.")
                    } else {
                        onSuccess(channels)
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post { onError(t.message ?: "Failed to load playlist") }
            }
        }
    }

    private fun fetchRemote(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code loading playlist")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchAsset(name: String): String =
        appContext.assets.open(name).bufferedReader().use { it.readText() }

    fun shutdown() {
        executor.shutdownNow()
    }
}
