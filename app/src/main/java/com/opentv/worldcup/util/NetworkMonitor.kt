package com.opentv.worldcup.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper

/**
 * Watches internet availability and reports changes on the main thread.
 *
 * @param onAvailable invoked when a validated internet connection appears.
 * @param onLost      invoked when connectivity is lost.
 */
class NetworkMonitor(
    context: Context,
    private val onAvailable: () -> Unit,
    private val onLost: () -> Unit
) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mainHandler.post(onAvailable)
        }

        override fun onLost(network: Network) {
            // Only fire "lost" if there is truly no other internet network.
            if (!isOnline()) {
                mainHandler.post(onLost)
            }
        }
    }

    /** Start receiving connectivity callbacks. */
    fun register() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    /** Stop receiving callbacks (call from onDestroy). */
    fun unregister() {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }

    /** Synchronous check: is there a validated internet connection right now? */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
