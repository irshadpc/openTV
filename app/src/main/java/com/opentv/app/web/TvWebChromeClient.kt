package com.opentv.app.web

import android.view.View
import android.webkit.WebChromeClient
import android.widget.FrameLayout

/**
 * Custom [WebChromeClient] that powers HTML5 full-screen video.
 *
 * When a web video element requests full screen, the WebView hands us a custom
 * [View] which we add to a dedicated full-screen container on top of everything
 * else. When playback exits full screen, we remove it again.
 */
class TvWebChromeClient(
    private val fullScreenContainer: FrameLayout,
    private val webViewContainer: View,
    private val onProgress: (progress: Int) -> Unit,
    private val onEnterFullScreen: () -> Unit,
    private val onExitFullScreen: () -> Unit
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgress(newProgress)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        // Guard against the (rare) double-show.
        if (customView != null) {
            onHideCustomView()
            return
        }
        customView = view
        customViewCallback = callback

        webViewContainer.visibility = View.GONE
        fullScreenContainer.visibility = View.VISIBLE
        fullScreenContainer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        onEnterFullScreen()
    }

    override fun onHideCustomView() {
        val view = customView ?: return
        fullScreenContainer.removeView(view)
        fullScreenContainer.visibility = View.GONE
        webViewContainer.visibility = View.VISIBLE

        customView = null
        runCatching { customViewCallback?.onCustomViewHidden() }
        customViewCallback = null
        onExitFullScreen()
    }

    /** True while a video is in HTML5 full-screen mode. */
    fun isInFullScreen(): Boolean = customView != null

    /** Programmatically leave full screen (used by the BACK button). */
    fun exitFullScreen() {
        if (isInFullScreen()) onHideCustomView()
    }
}
