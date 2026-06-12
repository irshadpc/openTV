package com.opentv.worldcup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.opentv.worldcup.databinding.ActivityMainBinding
import com.opentv.worldcup.player.ChannelsActivity
import com.opentv.worldcup.util.NetworkMonitor
import com.opentv.worldcup.web.TvWebChromeClient
import com.opentv.worldcup.web.TvWebViewClient

/**
 * Hosts the full-screen streaming WebView and all TV-specific behavior:
 * immersive mode, D-pad / remote handling, double-back-to-exit, a settings
 * panel, connectivity-aware reloading, loading spinner, and an offline screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var chromeClient: TvWebChromeClient
    private lateinit var networkMonitor: NetworkMonitor

    private val prefs by lazy {
        getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private var backPressedTime = 0L
    private var hasLoadedOnce = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind system bars so we can hide them for an immersive view.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the screen awake during streaming (no sleep mid-match).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupWebView()
        setupOverlayControls()
        setupNetworkMonitor()

        // Restore the last visited page, or fall back to the configured start URL.
        val startUrl = prefs.getString(AppConfig.KEY_LAST_URL, null) ?: AppConfig.START_URL
        loadUrlOrOffline(startUrl)
    }

    // ---------------------------------------------------------------------
    // WebView configuration
    // ---------------------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = binding.webView

        webView.settings.apply {
            javaScriptEnabled = true                 // required for the player
            domStorageEnabled = true                 // HTML5 localStorage
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false // allow autoplay of streams
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT      // cache web content
            allowContentAccess = true
            allowFileAccess = false                  // security: no local file URIs
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            // Identify as a TV browser; some players gate features on UA.
            userAgentString = userAgentString + " OpenTV/1.0 AndroidTV"
            // Mixed content can be required when an https page embeds an http
            // HLS stream. Allow it so streams are not silently blocked.
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        // Force a dark WebView surface where supported (matches the dark theme).
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
        }

        // Hardware-accelerated rendering for smooth video.
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.setBackgroundColor(0xFF000000.toInt())

        // Persist cookies / sessions across launches.
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        // Custom clients (security, fullscreen video, progress, errors).
        webView.webViewClient = TvWebViewClient(
            onPageLoadStarted = { showSpinner(true) },
            onPageLoadFinished = { url ->
                showSpinner(false)
                hasLoadedOnce = true
                rememberLastUrl(url)
            },
            onLoadError = { description ->
                showSpinner(false)
                if (!networkMonitor.isOnline()) {
                    showOffline(true)
                } else {
                    Toast.makeText(this, description, Toast.LENGTH_SHORT).show()
                }
            }
        )

        chromeClient = TvWebChromeClient(
            fullScreenContainer = binding.fullscreenContainer,
            webViewContainer = binding.webView,
            onProgress = { progress -> binding.progressBar.progress = progress },
            onEnterFullScreen = { hideSystemUi() },
            onExitFullScreen = { hideSystemUi() }
        )
        webView.webChromeClient = chromeClient
    }

    // ---------------------------------------------------------------------
    // Overlay controls (spinner, refresh, offline retry)
    // ---------------------------------------------------------------------

    private fun setupOverlayControls() {
        binding.refreshButton.setOnClickListener { webView.reload() }
        binding.retryButton.setOnClickListener {
            showOffline(false)
            loadUrlOrOffline(webView.url ?: AppConfig.START_URL)
        }
    }

    private fun showSpinner(show: Boolean) {
        binding.loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showOffline(show: Boolean) {
        binding.offlineLayout.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            showSpinner(false)
            binding.retryButton.requestFocus()
        }
    }

    // ---------------------------------------------------------------------
    // Connectivity
    // ---------------------------------------------------------------------

    private fun setupNetworkMonitor() {
        networkMonitor = NetworkMonitor(
            context = this,
            onAvailable = {
                // Connection came back: hide offline screen and reload if needed.
                if (binding.offlineLayout.visibility == View.VISIBLE || !hasLoadedOnce) {
                    showOffline(false)
                    loadUrlOrOffline(webView.url ?: AppConfig.START_URL)
                }
            },
            onLost = { /* Don't blank a running stream; offline screen shows on next error. */ }
        )
        networkMonitor.register()
    }

    private fun loadUrlOrOffline(url: String) {
        if (networkMonitor.isOnline()) {
            showOffline(false)
            webView.loadUrl(url)
        } else {
            showOffline(true)
        }
    }

    private fun rememberLastUrl(url: String) {
        // Don't remember error/blank URLs.
        if (url.startsWith("http")) {
            prefs.edit().putString(AppConfig.KEY_LAST_URL, url).apply()
        }
    }

    // ---------------------------------------------------------------------
    // Immersive mode
    // ---------------------------------------------------------------------

    private fun hideSystemUi() {
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    // ---------------------------------------------------------------------
    // Remote / D-pad handling
    // ---------------------------------------------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            // MENU button opens the settings panel.
            KeyEvent.KEYCODE_MENU -> {
                showSettingsPanel()
                return true
            }
            // Dedicated remote refresh keys, where present.
            KeyEvent.KEYCODE_R -> {
                webView.reload()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * BACK button logic:
     *  1. If a video is full-screen -> exit full screen.
     *  2. Else if the WebView can go back -> go back.
     *  3. Else double-press to exit the app.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            chromeClient.isInFullScreen() -> chromeClient.exitFullScreen()
            webView.canGoBack() -> webView.goBack()
            else -> handleDoubleBackExit()
        }
    }

    private fun handleDoubleBackExit() {
        val now = System.currentTimeMillis()
        if (now - backPressedTime < AppConfig.DOUBLE_BACK_EXIT_MS) {
            super.onBackPressed()
        } else {
            backPressedTime = now
            Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------------------------
    // Settings panel
    // ---------------------------------------------------------------------

    private fun showSettingsPanel() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_OpenTV_Dialog)
            .setTitle(R.string.settings_title)
            .setItems(
                arrayOf(
                    getString(R.string.action_live_channels),
                    getString(R.string.action_refresh),
                    getString(R.string.action_go_home),
                    getString(R.string.action_clear_cache),
                    getString(R.string.action_exit)
                )
            ) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, ChannelsActivity::class.java))
                    1 -> webView.reload()
                    2 -> webView.loadUrl(AppConfig.START_URL)
                    3 -> {
                        webView.clearCache(true)
                        Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
                    }
                    4 -> finish()
                }
            }
            .show()
    }

    // ---------------------------------------------------------------------
    // Lifecycle: keep WebView/video state consistent
    // ---------------------------------------------------------------------

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView.resumeTimers()
        webView.onResume()
        hideSystemUi()
    }

    override fun onDestroy() {
        networkMonitor.unregister()
        // Detach and destroy the WebView to avoid leaks.
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}
